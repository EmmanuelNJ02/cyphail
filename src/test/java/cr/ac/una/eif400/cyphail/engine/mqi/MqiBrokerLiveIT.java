
package cr.ac.una.eif400.cyphail.engine.mqi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integracion reales del broker MQI.
 *
 * Requieren SWI-Prolog instalado y el comando
 * swipl disponible en el PATH del sistema.
 *
 * Ejecutar con:
 * mvn -Dtest=MqiBrokerLiveIT test
 */
class MqiBrokerLiveIT {

    @Test
    void debeIniciarPrologReutilizarConexionYCerrarSesion()
            throws Exception {

        MqiBroker broker = MqiBroker.start();

        try {
            assertTrue(
                    broker.isOpen(),
                    "La sesion MQI debe estar abierta."
            );

            // Primera consulta: operacion aritmetica.
            PrologResult first = broker.execute(
                    "X is 2+3"
            );

            assertNumber(first, "X", 5);

            // Segunda consulta en la misma sesion.
            PrologResult second = broker.execute(
                    "member(X, [10,20])"
            );

            assertInstanceOf(
                    PrologResult.Success.class,
                    second
            );

            PrologResult.Success success =
                    (PrologResult.Success) second;

            List<Map<String, Object>> rows =
                    success.rows();

            assertEquals(
                    2,
                    rows.size(),
                    "member/2 debe producir dos soluciones."
            );

            assertEquals(
                    10L,
                    ((Number) rows.get(0).get("X"))
                            .longValue()
            );

            assertEquals(
                    20L,
                    ((Number) rows.get(1).get("X"))
                            .longValue()
            );

            assertTrue(
                    broker.isOpen(),
                    "La conexion debe continuar abierta "
                    + "despues de ejecutar varias consultas."
            );

        } finally {
            broker.close();
        }

        assertFalse(
                broker.isOpen(),
                "El broker debe quedar cerrado."
        );

        assertThrows(
                IOException.class,
                () -> broker.execute("true"),
                "No deben ejecutarse consultas "
                + "despues de cerrar el broker."
        );

        // El cierre debe poder llamarse mas de una vez.
        assertDoesNotThrow(broker::close);
    }

    @Test
    void debeCargarArchivoPrologYConsultarPredicado(
            @TempDir Path temporaryDirectory)
            throws Exception {

        Path engine = temporaryDirectory.resolve(
                "engine_test.pl"
        );

        Files.writeString(
                engine,
                "cyphail_broker_test_value(42).\n",
                StandardCharsets.UTF_8
        );

        try (MqiBroker broker = MqiBroker.start(engine)) {

            assertTrue(broker.isOpen());

            PrologResult result = broker.execute(
                    "cyphail_broker_test_value(X)"
            );

            assertNumber(result, "X", 42);
        }
    }

    /**
     * Verifica que una consulta produzca una fila
     * con el valor numerico esperado.
     */
    private static void assertNumber(
            PrologResult result,
            String variable,
            long expected) {

        assertInstanceOf(
                PrologResult.Success.class,
                result,
                "La consulta debe completarse correctamente."
        );

        PrologResult.Success success =
                (PrologResult.Success) result;

        assertEquals(
                1,
                success.rows().size(),
                "Se esperaba exactamente una solucion."
        );

        Object value = success.rows()
                .get(0)
                .get(variable);

        assertInstanceOf(
                Number.class,
                value,
                "La variable debe contener un numero."
        );

        assertEquals(
                expected,
                ((Number) value).longValue()
        );
    }
}
