
package cr.ac.una.eif400.cyphail.engine.mqi;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;
import java.util.Objects;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Paradigmas de Programacion
 * Universidad Nacional de Costa Rica
 * Escuela de Informatica
 * Group Code: 04-10am
 *
 * Broker responsible for managing a persistent
 * SWI-Prolog session through MQI.
 *
 * Startup command:
 *
 * swipl -l engine.pl mqi --write_connection_values=true
 *
 * The process provides:
 * 1. TCP port.
 * 2. MQI authentication password.
 *
 * The broker establishes a connection, performs
 * authentication, executes queries and closes
 * the session when no longer needed.
 *
 * Sprint P2:
 * Added bounded connection retries to handle
 * intermittent connection-refused errors
 * during SWI-Prolog MQI startup.
 */
public final class MqiBroker implements AutoCloseable {

    // =====================================================
    // TIMEOUT CONFIGURATION
    // =====================================================

    private static final int STARTUP_TIMEOUT_SECONDS = 15;

    /**
     * Maximum total time allowed to establish
     * the TCP connection after receiving
     * the MQI connection information.
     */
    private static final int CONNECT_TIMEOUT_MILLIS = 5000;

    /**
     * Maximum duration of an individual
     * connection attempt.
     */
    private static final int CONNECT_ATTEMPT_TIMEOUT_MILLIS = 500;

    /**
     * Delay between connection attempts.
     */
    private static final int CONNECT_RETRY_DELAY_MILLIS = 100;

    private static final int AUTH_TIMEOUT_MILLIS = 10000;

    private static final int SHUTDOWN_TIMEOUT_SECONDS = 3;

    // =====================================================
    // BROKER STATE
    // =====================================================

    private final Process process;

    private final BufferedReader processOutput;

    private final Socket socket;

    private final MqiClient client;

    private boolean closed;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    private MqiBroker(
            Process process,
            BufferedReader processOutput,
            Socket socket,
            MqiClient client
    ) {

        this.process = process;

        this.processOutput = processOutput;

        this.socket = socket;

        this.client = client;
    }

    // =====================================================
    // START WITH PROLOG ENGINE
    // =====================================================

    /**
     * Starts SWI-Prolog and loads the specified
     * Cyphail Prolog engine.
     *
     * Supports file paths containing spaces.
     */
    public static MqiBroker start(
            Path engineFile
    ) throws IOException {

        Objects.requireNonNull(
                engineFile,
                "engineFile"
        );

        Path normalized = engineFile
                .toAbsolutePath()
                .normalize();

        if (!Files.isRegularFile(normalized)) {

            throw new FileNotFoundException(
                    "No existe el engine Prolog: "
                            + normalized
            );
        }

        return launch(
                List.of(
                        "swipl",
                        "-l",
                        normalized.toString(),
                        "mqi",
                        "--write_connection_values=true"
                )
        );
    }

    // =====================================================
    // START WITHOUT ADDITIONAL ENGINE
    // =====================================================

    /**
     * Starts MQI without loading an additional
     * Prolog source file.
     *
     * Useful for testing SWI-Prolog predicates.
     */
    public static MqiBroker start()
            throws IOException {

        return launch(
                List.of(
                        "swipl",
                        "mqi",
                        "--write_connection_values=true"
                )
        );
    }

    // =====================================================
    // LAUNCH MQI PROCESS
    // =====================================================

    /**
     * Starts the SWI-Prolog process.
     *
     * Steps:
     *
     * 1. Launch SWI-Prolog.
     * 2. Read port and password.
     * 3. Connect with bounded retries.
     * 4. Authenticate the MQI client.
     * 5. Return the connected broker.
     *
     * Resources are released if startup fails.
     */
    private static MqiBroker launch(
            List<String> command
    ) throws IOException {

        ProcessBuilder builder =
                new ProcessBuilder(command);

        /*
         * Keep stderr separate from stdout.
         *
         * MQI sends its connection information
         * through stdout, so error messages
         * must not be mixed into that stream.
         */
        builder.redirectError(
                ProcessBuilder.Redirect.INHERIT
        );

        Process process =
                builder.start();

        BufferedReader output =
                new BufferedReader(
                        new InputStreamReader(
                                process.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                );

        Socket socket = null;

        try {

            // -----------------------------------------
            // READ MQI CONNECTION INFORMATION
            // -----------------------------------------

            ConnectionInfo info =
                    readConnectionInfo(
                            process,
                            output
                    );

            // -----------------------------------------
            // CONNECT TO MQI
            // -----------------------------------------

            /*
             * Do not assume that the MQI server
             * is already accepting connections
             * immediately after printing its port.
             *
             * Connection-refused errors are retried
             * within a limited time window.
             */
            socket = connectWithRetry(
                    process,
                    info.port()
            );

            // -----------------------------------------
            // AUTHENTICATION
            // -----------------------------------------

            socket.setSoTimeout(
                    AUTH_TIMEOUT_MILLIS
            );

            MqiClient client =
                    new MqiClient(
                            socket.getInputStream(),
                            socket.getOutputStream()
                    );

            client.authenticate(
                    info.password()
            );

            /*
             * Remove the global socket read timeout
             * after authentication.
             *
             * Individual query timeouts remain
             * available through execute(goal, seconds).
             */
            socket.setSoTimeout(0);

            // -----------------------------------------
            // RETURN CONNECTED BROKER
            // -----------------------------------------

            return new MqiBroker(
                    process,
                    output,
                    socket,
                    client
            );

        } catch (
                IOException | RuntimeException exception
        ) {

            /*
             * Release the socket and child process
             * if connection or authentication fails.
             */
            if (socket != null) {

                try {

                    socket.close();

                } catch (IOException closeError) {

                    exception.addSuppressed(
                            closeError
                    );
                }
            }

            stopProcess(process);

            try {

                output.close();

            } catch (IOException closeError) {

                exception.addSuppressed(
                        closeError
                );
            }

            throw exception;
        }
    }

    // =====================================================
    // MQI TCP CONNECTION WITH RETRIES
    // =====================================================

    /**
     * Establishes a TCP connection to SWI-Prolog.
     *
     * A new Socket is created for every attempt.
     *
     * Retries are only used for connection-refused
     * and connection-timeout errors.
     *
     * The method:
     *
     * - Uses a fixed total connection deadline.
     * - Checks whether the Prolog process is alive.
     * - Closes sockets from failed attempts.
     * - Preserves thread interruption.
     * - Reports a descriptive error if retries fail.
     *
     * @param process active SWI-Prolog process
     * @param port MQI listening port
     * @return connected TCP socket
     * @throws IOException if MQI cannot be reached
     */
    private static Socket connectWithRetry(
            Process process,
            int port
    ) throws IOException {

        InetSocketAddress address =
                new InetSocketAddress(
                        "127.0.0.1",
                        port
                );

        long deadline =
                System.nanoTime()
                        + TimeUnit.MILLISECONDS.toNanos(
                                CONNECT_TIMEOUT_MILLIS
                        );

        IOException lastFailure = null;

        while (true) {

            // -----------------------------------------
            // CHECK PROCESS
            // -----------------------------------------

            if (!process.isAlive()) {

                throw new IOException(
                        "SWI-Prolog finalizo antes de "
                                + "establecer la conexion MQI "
                                + "en el puerto "
                                + port
                                + ".",
                        lastFailure
                );
            }

            // -----------------------------------------
            // CHECK REMAINING TIME
            // -----------------------------------------

            long remainingNanos =
                    deadline - System.nanoTime();

            if (remainingNanos <= 0) {

                throw new IOException(
                        "MQI no acepto la conexion TCP "
                                + "en 127.0.0.1:"
                                + port
                                + " dentro de "
                                + CONNECT_TIMEOUT_MILLIS
                                + " milisegundos.",
                        lastFailure
                );
            }

            int attemptTimeout =
                    (int) Math.min(
                            CONNECT_ATTEMPT_TIMEOUT_MILLIS,
                            Math.max(
                                    1,
                                    TimeUnit.NANOSECONDS.toMillis(
                                            remainingNanos
                                    )
                            )
                    );

            // -----------------------------------------
            // CREATE A FRESH SOCKET
            // -----------------------------------------

            Socket candidate =
                    new Socket();

            try {

                candidate.connect(
                        address,
                        attemptTimeout
                );

                /*
                 * Connection established.
                 *
                 * Return the socket so that
                 * authentication can begin.
                 */
                return candidate;

            } catch (
                    ConnectException
                            | SocketTimeoutException exception
            ) {

                lastFailure = exception;

                // -------------------------------------
                // CLOSE FAILED SOCKET
                // -------------------------------------

                try {

                    candidate.close();

                } catch (IOException closeError) {

                    exception.addSuppressed(
                            closeError
                    );
                }

                /*
                 * If SWI-Prolog stopped, do not
                 * continue retrying connections.
                 */
                if (!process.isAlive()) {

                    throw new IOException(
                            "SWI-Prolog finalizo durante "
                                    + "el intento de conexion MQI.",
                            exception
                    );
                }

                // -------------------------------------
                // WAIT BEFORE NEXT ATTEMPT
                // -------------------------------------

                long timeLeft =
                        deadline - System.nanoTime();

                if (timeLeft <= 0) {

                    continue;
                }

                long delayMillis =
                        Math.min(
                                CONNECT_RETRY_DELAY_MILLIS,
                                Math.max(
                                        1,
                                        TimeUnit.NANOSECONDS.toMillis(
                                                timeLeft
                                        )
                                )
                        );

                try {

                    Thread.sleep(
                            delayMillis
                    );

                } catch (InterruptedException interrupted) {

                    Thread.currentThread().interrupt();

                    throw new IOException(
                            "Conexion MQI interrumpida.",
                            interrupted
                    );
                }

            } catch (
                    IOException | RuntimeException exception
            ) {

                /*
                 * Unexpected connection errors
                 * are not automatically retried.
                 */
                try {

                    candidate.close();

                } catch (IOException closeError) {

                    exception.addSuppressed(
                            closeError
                    );
                }

                throw exception;
            }
        }
    }

    // =====================================================
    // READ MQI PORT AND PASSWORD
    // =====================================================

    /**
     * Reads the connection information supplied
     * through the Prolog process stdout.
     *
     * Expected order:
     *
     * First line: TCP port.
     * Second line: MQI password.
     *
     * Startup has a bounded timeout to avoid
     * waiting indefinitely.
     */
    private static ConnectionInfo readConnectionInfo(
            Process process,
            BufferedReader output
    ) throws IOException {

        ExecutorService executor =
                Executors.newSingleThreadExecutor(
                        task -> {

                            Thread worker =
                                    new Thread(
                                            task,
                                            "cyphail-mqi-startup"
                                    );

                            worker.setDaemon(true);

                            return worker;
                        }
                );

        try {

            Future<ConnectionInfo> result =
                    executor.submit(
                            () -> {

                                String portLine =
                                        output.readLine();

                                String password =
                                        output.readLine();

                                if (portLine == null
                                        || password == null) {

                                    throw new IOException(
                                            "SWI-Prolog finalizo sin "
                                                    + "entregar puerto "
                                                    + "y contrasena MQI."
                                    );
                                }

                                int port;

                                try {

                                    port = Integer.parseInt(
                                            portLine.strip()
                                    );

                                } catch (
                                        NumberFormatException exception
                                ) {

                                    throw new IOException(
                                            "Puerto MQI no valido: "
                                                    + portLine,
                                            exception
                                    );
                                }

                                if (port < 1
                                        || port > 65535
                                        || password.isBlank()) {

                                    throw new IOException(
                                            "Valores de conexion "
                                                    + "MQI invalidos."
                                    );
                                }

                                return new ConnectionInfo(
                                        port,
                                        password.strip()
                                );
                            }
                    );

            try {

                return result.get(
                        STARTUP_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS
                );

            } catch (TimeoutException exception) {

                result.cancel(true);

                throw new IOException(
                        "SWI-Prolog no entrego datos "
                                + "MQI dentro de "
                                + STARTUP_TIMEOUT_SECONDS
                                + " segundos.",
                        exception
                );

            } catch (InterruptedException exception) {

                Thread.currentThread().interrupt();

                throw new IOException(
                        "Arranque MQI interrumpido.",
                        exception
                );

            } catch (ExecutionException exception) {

                throw new IOException(
                        "No fue posible iniciar MQI "
                                + "(proceso activo: "
                                + process.isAlive()
                                + ").",
                        exception.getCause()
                );
            }

        } finally {

            executor.shutdownNow();
        }
    }

    // =====================================================
    // EXECUTE PROLOG GOALS
    // =====================================================

    /**
     * Executes a Prolog goal using the
     * existing MQI connection.
     */
    public synchronized PrologResult execute(
            String goal
    ) throws IOException {

        ensureOpen();

        return client.run(goal);
    }

    /**
     * Executes a Prolog goal with a timeout.
     *
     * -1 means no MQI query timeout.
     */
    public synchronized PrologResult execute(
            String goal,
            int timeoutSeconds
    ) throws IOException {

        ensureOpen();

        return client.run(
                goal,
                timeoutSeconds
        );
    }

    // =====================================================
    // CONNECTION STATUS
    // =====================================================

    /**
     * Indicates whether the broker session
     * remains available.
     */
    public synchronized boolean isOpen() {

        return !closed
                && process.isAlive()
                && !socket.isClosed();
    }

    /**
     * Prevents executing a goal on a
     * closed or unavailable MQI session.
     */
    private void ensureOpen()
            throws IOException {

        if (!isOpen()) {

            throw new IOException(
                    "La sesion MQI no esta disponible "
                            + "o ya fue cerrada."
            );
        }
    }

    // =====================================================
    // CLOSE BROKER
    // =====================================================

    /**
     * Closes the MQI connection and terminates
     * the SWI-Prolog child process.
     *
     * Intended for try-with-resources.
     */
    @Override
    public synchronized void close()
            throws IOException {

        if (closed) {

            return;
        }

        closed = true;

        IOException failure = null;

        try {

            socket.setSoTimeout(
                    AUTH_TIMEOUT_MILLIS
            );

            client.close();

        } catch (IOException exception) {

            failure = exception;

        } finally {

            try {

                socket.close();

            } catch (IOException exception) {

                if (failure == null) {

                    failure = exception;

                } else {

                    failure.addSuppressed(
                            exception
                    );
                }
            }

            stopProcess(process);

            try {

                processOutput.close();

            } catch (IOException exception) {

                if (failure == null) {

                    failure = exception;

                } else {

                    failure.addSuppressed(
                            exception
                    );
                }
            }
        }

        if (failure != null) {

            throw failure;
        }
    }

    // =====================================================
    // STOP SWI-PROLOG PROCESS
    // =====================================================

    /**
     * Stops SWI-Prolog and forces termination
     * if the process does not exit normally.
     */
    private static void stopProcess(
            Process process
    ) {

        if (!process.isAlive()) {

            return;
        }

        process.destroy();

        try {

            if (!process.waitFor(
                    SHUTDOWN_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            )) {

                process.destroyForcibly();

                process.waitFor(
                        SHUTDOWN_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS
                );
            }

        } catch (InterruptedException exception) {

            process.destroyForcibly();

            Thread.currentThread().interrupt();
        }
    }

    // =====================================================
    // CONNECTION INFORMATION
    // =====================================================

    /**
     * TCP connection values generated
     * by the SWI-Prolog MQI server.
     */
    private record ConnectionInfo(
            int port,
            String password
    ) {
    }
}
