
package cr.ac.una.eif400.cyphail.engine.mqi;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de integracion real Java - MQI - SWI-Prolog.
 *
 * Basada en el procedimiento de inicio y conexion
 * del demo MQI proporcionado para Cyphail P2.
 *
 * Grupo: 04-10am
 * Requiere SWI-Prolog instalado y accesible como "swipl".
 */
class MqiLiveIT {

    @Test
    void shouldExecuteGoalsOnRealSwiProlog() throws Exception {

        ProcessBuilder builder = new ProcessBuilder(
                "swipl",
                "mqi",
                "--write_connection_values=true"
        );

        builder.redirectError(
                ProcessBuilder.Redirect.INHERIT
        );

        Process prolog = builder.start();

        try {
            BufferedReader startup = new BufferedReader(
                    new InputStreamReader(
                            prolog.getInputStream(),
                            StandardCharsets.UTF_8
                    )
            );

            int port = Integer.parseInt(
                    startup.readLine().trim()
            );

            String password = startup.readLine().trim();

            try (Socket socket = new Socket()) {

                socket.connect(
                        new InetSocketAddress("127.0.0.1", port),
                        5000
                );

                socket.setSoTimeout(5000);

                try (MqiClient client = new MqiClient(
                        socket.getInputStream(),
                        socket.getOutputStream()
                )) {
                    client.authenticate(password);

                    // Primera consulta: operacion matematica.
                    PrologResult.Success arithmetic =
                            assertInstanceOf(
                                    PrologResult.Success.class,
                                    client.run("X is 2+3")
                            );

                    assertEquals(
                            1,
                            arithmetic.rows().size()
                    );

                    assertEquals(
                            5L,
                            arithmetic.rows().get(0).get("X")
                    );

                    // Segunda consulta: multiples soluciones.
                    PrologResult.Success members =
                            assertInstanceOf(
                                    PrologResult.Success.class,
                                    client.run("member(X, [10,20])")
                            );

                    assertEquals(2, members.rows().size());

                    assertEquals(
                            10L,
                            members.rows().get(0).get("X")
                    );

                    assertEquals(
                            20L,
                            members.rows().get(1).get("X")
                    );
                }
            }

        } finally {
            prolog.destroy();

            if (!prolog.waitFor(3, TimeUnit.SECONDS)) {
                prolog.destroyForcibly();
            }
        }
    }
}
