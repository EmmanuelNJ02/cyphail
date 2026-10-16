
package cr.ac.una.eif400.cyphail.engine.mqi;

import java.util.List;
import java.util.Map;

/**
 * Representa el resultado de ejecutar un objetivo Prolog
 * mediante la interfaz MQI de SWI-Prolog.
 *
 * Adaptado de PrologResult.java del demo MQI
 * proporcionado para el Sprint P2.
 *
 * Proyecto: Cyphail P2 - Grupo 04-10am.
 */
public sealed interface PrologResult {

    /**
     * Consulta ejecutada correctamente.
     * Cada mapa contiene los valores de las variables
     * correspondientes a una solución.
     */
    record Success(
            List<Map<String, Object>> rows
    ) implements PrologResult {
    }

    /**
     * Consulta sin soluciones.
     * No representa un error de ejecución.
     */
    record Failure() implements PrologResult {
    }

    /**
     * Error reportado por SWI-Prolog.
     */
    record PrologError(
            Object term
    ) implements PrologResult {
    }
}
