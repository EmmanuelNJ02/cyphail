
package cr.ac.una.eif400.cyphail.engine.mqi;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
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
 * Broker de Cyphail P2: administra una sesion con SWI-Prolog via MQI.
 *
 * Se basa en el procedimiento de arranque del demo del profesor:
 * swipl -l engine.pl mqi --write_connection_values=true.
 *
 * El proceso entrega primero el puerto y despues la contrasena MQI.
 *
 * El broker mantiene la conexion durante varias consultas.
 * Debe cerrarse al finalizar la sesion, preferiblemente usando
 * try-with-resources.
 *
 * Este componente no reemplaza al engine Prolog definitivo.
 */
public final class MqiBroker implements AutoCloseable {

    private static final int STARTUP_TIMEOUT_SECONDS = 15;
    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
    private static final int AUTH_TIMEOUT_MILLIS = 10000;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 3;

    private final Process process;
    private final BufferedReader processOutput;
    private final Socket socket;
    private final MqiClient client;

    private boolean closed;

    private MqiBroker(
            Process process,
            BufferedReader processOutput,
            Socket socket,
            MqiClient client) {

        this.process = process;
        this.processOutput = processOutput;
        this.socket = socket;
        this.client = client;
    }

    /**
     * Inicia SWI-Prolog cargando el archivo engine.pl indicado.
     * Permite rutas que contengan espacios.
     */
    public static MqiBroker start(Path engineFile) throws IOException {

        Objects.requireNonNull(engineFile, "engineFile");

        Path normalized = engineFile.toAbsolutePath().normalize();

        if (!Files.isRegularFile(normalized)) {
            throw new FileNotFoundException(
                    "No existe el engine Prolog: " + normalized);
        }

        return launch(List.of(
                "swipl",
                "-l",
                normalized.toString(),
                "mqi",
                "--write_connection_values=true"
        ));
    }

    /**
     * Inicia MQI sin cargar un archivo Prolog adicional.
     * Util para probar predicados propios de SWI-Prolog.
     */
    public static MqiBroker start() throws IOException {

        return launch(List.of(
                "swipl",
                "mqi",
                "--write_connection_values=true"
        ));
    }

    /**
     * Inicia el proceso, obtiene los datos MQI y autentica al cliente.
     */
    private static MqiBroker launch(List<String> command)
            throws IOException {

        ProcessBuilder builder = new ProcessBuilder(command);

        // Los errores van a stderr para no mezclarlos
        // con el puerto y la contrasena enviados por stdout.
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = builder.start();

        BufferedReader output = new BufferedReader(
                new InputStreamReader(
                        process.getInputStream(),
                        StandardCharsets.UTF_8
                )
        );

        Socket socket = null;

        try {

            ConnectionInfo info =
                    readConnectionInfo(process, output);

            socket = new Socket();

            socket.connect(
                    new InetSocketAddress(
                            "127.0.0.1",
                            info.port()
                    ),
                    CONNECT_TIMEOUT_MILLIS
            );

            // Limite para recibir la respuesta de autenticacion.
            socket.setSoTimeout(AUTH_TIMEOUT_MILLIS);

            MqiClient client = new MqiClient(
                    socket.getInputStream(),
                    socket.getOutputStream()
            );

            client.authenticate(info.password());

            // Despues del saludo no se aplica timeout global
            // al socket. Las consultas pueden controlar su
            // propio tiempo mediante execute(goal, segundos).
            socket.setSoTimeout(0);

            return new MqiBroker(
                    process,
                    output,
                    socket,
                    client
            );

        } catch (IOException | RuntimeException exception) {

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException closeError) {
                    exception.addSuppressed(closeError);
                }
            }

            stopProcess(process);

            try {
                output.close();
            } catch (IOException closeError) {
                exception.addSuppressed(closeError);
            }

            throw exception;
        }
    }

    /**
     * Obtiene el puerto y la contrasena generados por MQI.
     * Evita esperar indefinidamente si falla el arranque.
     */
    private static ConnectionInfo readConnectionInfo(
            Process process,
            BufferedReader output) throws IOException {

        ExecutorService executor =
                Executors.newSingleThreadExecutor(task -> {

                    Thread worker = new Thread(
                            task,
                            "cyphail-mqi-startup"
                    );

                    worker.setDaemon(true);
                    return worker;
                });

        try {

            Future<ConnectionInfo> result =
                    executor.submit(() -> {

                        String portLine = output.readLine();
                        String password = output.readLine();

                        if (portLine == null || password == null) {
                            throw new IOException(
                                    "SWI-Prolog finalizo sin entregar "
                                    + "puerto y contrasena MQI."
                            );
                        }

                        int port;

                        try {
                            port = Integer.parseInt(
                                    portLine.strip()
                            );
                        } catch (NumberFormatException exception) {
                            throw new IOException(
                                    "Puerto MQI no valido: " + portLine,
                                    exception
                            );
                        }

                        if (port < 1
                                || port > 65535
                                || password.isBlank()) {

                            throw new IOException(
                                    "Valores de conexion MQI invalidos."
                            );
                        }

                        return new ConnectionInfo(
                                port,
                                password.strip()
                        );
                    });

            try {

                return result.get(
                        STARTUP_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS
                );

            } catch (TimeoutException exception) {

                result.cancel(true);

                throw new IOException(
                        "SWI-Prolog no entrego datos MQI dentro de "
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

    /**
     * Ejecuta un goal reutilizando la conexion existente.
     */
    public synchronized PrologResult execute(String goal)
            throws IOException {

        ensureOpen();

        return client.run(goal);
    }

    /**
     * Ejecuta un goal con limite de tiempo en segundos.
     * -1 significa sin limite de tiempo en MQI.
     */
    public synchronized PrologResult execute(
            String goal,
            int timeoutSeconds) throws IOException {

        ensureOpen();

        return client.run(goal, timeoutSeconds);
    }

    /**
     * Comprueba si la sesion sigue disponible.
     */
    public synchronized boolean isOpen() {

        return !closed
                && process.isAlive()
                && !socket.isClosed();
    }

    /**
     * Impide ejecutar consultas sobre una sesion cerrada.
     */
    private void ensureOpen() throws IOException {

        if (!isOpen()) {
            throw new IOException(
                    "La sesion MQI no esta disponible "
                    + "o ya fue cerrada."
            );
        }
    }

    /**
     * Cierra la conexion MQI y detiene el proceso SWI-Prolog.
     */
    @Override
    public synchronized void close() throws IOException {

        if (closed) {
            return;
        }

        closed = true;

        IOException failure = null;

        try {

            socket.setSoTimeout(AUTH_TIMEOUT_MILLIS);
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
                    failure.addSuppressed(exception);
                }
            }

            stopProcess(process);

            try {
                processOutput.close();
            } catch (IOException exception) {

                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }

        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Detiene el proceso y fuerza el cierre si no termina.
     */
    private static void stopProcess(Process process) {

        if (!process.isAlive()) {
            return;
        }

        process.destroy();

        try {

            if (!process.waitFor(
                    SHUTDOWN_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS)) {

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

    /**
     * Datos de conexion proporcionados por SWI-Prolog.
     */
    private record ConnectionInfo(
            int port,
            String password) {
    }
}
