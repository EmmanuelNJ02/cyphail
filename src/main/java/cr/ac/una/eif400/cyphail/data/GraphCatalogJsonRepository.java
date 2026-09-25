package cr.ac.una.eif400.cyphail.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import cr.ac.una.eif400.cyphail.model.GraphInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Loads the Cyphail fake graph catalog from a JSON file stored on disk.
 *
 * The graph catalog is stored in data/graphs.json and is read again
 * every time it is requested so changes made to the JSON file can be
 * observed without recompiling the application.
 *
 * Gson is used in tree mode to decode the JSON structure.
 *
 * Project: Cyphail
 * Course: EIF400 - Paradigmas de Programacion
 * University: Universidad Nacional de Costa Rica
 * School: Escuela de Informatica
 * Work Group: 04
 * Schedule: 10:00 a.m.
 * Group Code: 04-10am
 *
 * Authors:
 * Emmanuel Nunez Jimenez
 * Valery Alfaro Morales
 * Roy Arias Mejia
 * Keynell Molina Mora
 * Julissa Solano Valverde
 *
 * @author Emmanuel Nunez Jimenez
 * @author Valery Alfaro Morales
 * @author Roy Arias Mejia
 * @author Keynell Molina Mora
 * @author Julissa Solano Valverde
 */
public final class GraphCatalogJsonRepository {

    private static final Path GRAPHS_FILE =
            Path.of("data", "graphs.json");

    /**
     * Prevents instances of this utility class.
     */
    private GraphCatalogJsonRepository() {
    }

    /**
     * Loads all graphs defined in data/graphs.json.
     *
     * The file is read from disk on every invocation. If the document
     * cannot be read or decoded, an empty list is returned instead of
     * terminating the REPL.
     *
     * @return immutable list of available graphs
     */
    public static List<GraphInfo> findAll() {
        try {
            JsonArray graphs =
                    JsonFileLoader.load(GRAPHS_FILE).getAsJsonArray();

            return IntStream
                    .range(0, graphs.size())
                    .mapToObj(index ->
                            decodeGraph(
                                    graphs
                                            .get(index)
                                            .getAsJsonObject()
                            )
                    )
                    .toList();
        } catch (IOException | RuntimeException exception) {
            return List.of();
        }
    }

    /**
     * Decodes one graph entry from Gson's tree representation.
     *
     * @param graphObject JSON object representing one graph
     * @return decoded graph information
     */
    private static GraphInfo decodeGraph(JsonObject graphObject) {
        String name = graphObject
                .get("name")
                .getAsString();

        String description = graphObject
                .get("description")
                .getAsString();

        return new GraphInfo(name, description);
    }
}