
package cr.ac.una.eif400.cyphail.engine.mqi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Interpreta el JSON recibido desde MQI de SWI-Prolog.
 *
 * Adaptado del demo MQI proporcionado para Cyphail P2.
 */
public final class MqiResponseParser {

    private MqiResponseParser() {
    }

    /**
     * Representa un termino compuesto de Prolog.
     */
    public record Compound(String functor, List<Object> args) {
    }

    /**
     * Convierte una respuesta JSON en un PrologResult.
     */
    public static PrologResult parse(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException(
                    "La respuesta MQI esta vacia."
            );
        }

        JsonElement root = JsonParser.parseString(json);

        // Respuestas booleanas: true o false.
        if (root.isJsonPrimitive()
                && root.getAsJsonPrimitive().isBoolean()) {

            return root.getAsBoolean()
                    ? new PrologResult.Success(List.of(Map.of()))
                    : new PrologResult.Failure();
        }

        JsonObject response = asObject(root, "respuesta MQI");
        String functor = getString(response, "functor");
        JsonArray args = getArray(response, "args");

        return switch (functor) {
            case "true" -> {
                if (args.size() == 0) {
                    throw new IllegalArgumentException(
                            "La respuesta de exito no contiene soluciones."
                    );
                }

                yield new PrologResult.Success(
                        parseSolutions(
                                asArray(args.get(0), "soluciones")
                        )
                );
            }

            case "exception" -> {
                if (args.size() == 0) {
                    throw new IllegalArgumentException(
                            "La respuesta de error no contiene su termino."
                    );
                }

                yield new PrologResult.PrologError(
                        toJava(args.get(0))
                );
            }

            default -> throw new IllegalArgumentException(
                    "Respuesta MQI desconocida: " + functor
            );
        };
    }

    /**
     * Extrae las soluciones y sus variables.
     * LinkedHashMap conserva el orden recibido.
     */
    private static List<Map<String, Object>> parseSolutions(
            JsonArray solutions) {

        List<Map<String, Object>> rows = new ArrayList<>();

        for (JsonElement solution : solutions) {
            Map<String, Object> row = new LinkedHashMap<>();

            JsonArray bindings = asArray(
                    solution, "bindings de una solucion"
            );

            for (JsonElement binding : bindings) {
                JsonObject assignment = asObject(
                        binding, "binding"
                );

                if (!"=".equals(
                        getString(assignment, "functor"))) {
                    throw new IllegalArgumentException(
                            "Binding MQI inesperado."
                    );
                }

                JsonArray pair = getArray(assignment, "args");

                if (pair.size() != 2) {
                    throw new IllegalArgumentException(
                            "Un binding MQI debe tener variable y valor."
                    );
                }

                String variable = pair.get(0).getAsString();

                row.put(
                        variable,
                        toJava(pair.get(1))
                );
            }

            rows.add(row);
        }

        return rows;
    }

    /**
     * Convierte los tipos JSON en tipos Java.
     */
    static Object toJava(JsonElement element) {

        if (element == null || element.isJsonNull()) {
            return null;
        }

        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive =
                    element.getAsJsonPrimitive();

            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            }

            if (primitive.isNumber()) {
                String number = primitive.getAsString();

                if (number.contains(".")
                        || number.contains("e")
                        || number.contains("E")) {
                    return primitive.getAsDouble();
                }

                return primitive.getAsLong();
            }

            return primitive.getAsString();
        }

        if (element.isJsonArray()) {
            List<Object> values = new ArrayList<>();

            for (JsonElement item : element.getAsJsonArray()) {
                values.add(toJava(item));
            }

            return values;
        }

        JsonObject compound = asObject(
                element, "termino Prolog"
        );

        String functor = getString(compound, "functor");
        JsonArray args = getArray(compound, "args");

        List<Object> values = new ArrayList<>();

        for (JsonElement item : args) {
            values.add(toJava(item));
        }

        return new Compound(functor, values);
    }

    private static JsonObject asObject(
            JsonElement element, String context) {

        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException(
                    "Se esperaba objeto JSON en " + context
            );
        }

        return element.getAsJsonObject();
    }

    private static JsonArray asArray(
            JsonElement element, String context) {

        if (element == null || !element.isJsonArray()) {
            throw new IllegalArgumentException(
                    "Se esperaba arreglo JSON en " + context
            );
        }

        return element.getAsJsonArray();
    }

    private static JsonArray getArray(
            JsonObject object, String field) {

        return asArray(object.get(field), field);
    }

    private static String getString(
            JsonObject object, String field) {

        JsonElement value = object.get(field);

        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {

            throw new IllegalArgumentException(
                    "Falta texto JSON en " + field
            );
        }

        return value.getAsString();
    }
}
