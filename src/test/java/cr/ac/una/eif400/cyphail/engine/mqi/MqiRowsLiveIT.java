
package cr.ac.una.eif400.cyphail.engine.mqi;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de integracion real del resultado tabular Rows.
 *
 * Proyecto: Cyphail - Sprint P2
 * Grupo: 04-10am
 *
 * Verifica que un texto producido por SWI-Prolog
 * llegue correctamente a Java mediante MQI.
 *
 * Ejecutar con:
 * mvn -Dtest=MqiRowsLiveIT test
 */
class MqiRowsLiveIT {

    @Test
    void debeRecibirRowsDesdePrologConSaltoDeLinea()
            throws Exception {

        try (MqiBroker broker = MqiBroker.start()) {

            assertTrue(
                    broker.isOpen(),
                    "El broker debe estar conectado a SWI-Prolog."
            );

            // SWI-Prolog construye una cadena con un salto
            // de linea mediante format/3.
            String goal =
                    "format(string(Rows), "
                    + "'nombre edad~nJorge 43', [])";

            PrologResult result = broker.execute(goal);

            PrologResult.Success success =
                    assertInstanceOf(
                            PrologResult.Success.class,
                            result,
                            "SWI-Prolog debe devolver una respuesta exitosa."
                    );

            assertEquals(
                    1,
                    success.rows().size(),
                    "Se esperaba una solucion."
            );

            Map<String, Object> row =
                    success.rows().get(0);

            assertTrue(
                    row.containsKey("Rows"),
                    "La respuesta debe contener la variable Rows."
            );

            assertEquals(
                    "nombre edad\nJorge 43",
                    row.get("Rows"),
                    "Debe conservarse el texto tabular "
                    + "y el salto de linea."
            );

        }
    }
}
