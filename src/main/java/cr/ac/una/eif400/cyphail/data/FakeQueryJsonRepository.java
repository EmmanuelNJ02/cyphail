package cr.ac.una.eif400.cyphail.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import cr.ac.una.eif400.cyphail.model.FakeQueryResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Loads the fake query results used by Cyphail from JSON files on disk.
 *
 * Each query is stored in an independent JSON document under
 * data/queries. The files are read again for every lookup so changes
 * made during execution can be observed without recompiling Cyphail.
 *
 * Gson is used in tree mode. No annotated POJO classes are required
 * to decode the JSON documents.
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
public final class FakeQueryJsonRepository {

    private static final Path QUERIES_DIRECTORY =
            Path.of("data", "queries");

    /**
     * Prevents instances of this utility class.
     */
    private FakeQueryJsonRepository() {
    }

    /**
     * Finds the fake result associated with a Cyphail query.
     *
     * Query comparison is case insensitive and ignores repeated
     * whitespace, preserving the behavior provided by Sprint P1.1.
     *
     * @param query query entered by the user
     * @return fake result when a matching JSON document exists
     */
    public static Optional<FakeQueryResult> findByQuery(String query) {
        Objects.requireNonNull(query, "query");

        String normalizedQuery = normalize(query);

        if (!Files.isDirectory(QUERIES_DIRECTORY)) {
            return Optional.empty();
        }

        try (Stream<Path> files = Files.list(QUERIES_DIRECTORY)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(FakeQueryJsonRepository::isJsonFile)
                    .sorted()
                    .map(FakeQueryJsonRepository::loadEntrySafely)
                    .flatMap(Optional::stream)
                    .filter(entry ->
                            normalize(entry.query())
                                    .equals(normalizedQuery)
                    )
                    .map(QueryEntry::result)
                    .findFirst();
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    /**
     * Loads one query document without preventing other JSON files
     * from being considered when a single document is unreadable or
     * malformed.
     *
     * @param path query JSON file
     * @return decoded entry when the document is valid
     */
    private static Optional<QueryEntry> loadEntrySafely(Path path) {
        try {
            return Optional.of(loadEntry(path));
        } catch (IOException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    /**
     * Decodes one JSON query document using Gson's tree API.
     *
     * @param path query JSON file
     * @return decoded query entry
     * @throws IOException when the file cannot be read
     */
    private static QueryEntry loadEntry(Path path) throws IOException {
        JsonObject root = JsonFileLoader.load(path).getAsJsonObject();

        String query = root
                .get("query")
                .getAsString();

        List<String> headers = readStringArray(
                root.getAsJsonArray("headers")
        );

        List<List<String>> rows = readRows(
                root.getAsJsonArray("rows")
        );

        String statusMessage = root
                .get("statusMessage")
                .getAsString();

        FakeQueryResult result = new FakeQueryResult(
                headers,
                rows,
                statusMessage
        );

        return new QueryEntry(query, result);
    }

    /**
     * Converts a JSON array into an immutable list of strings.
     *
     * @param array JSON string array
     * @return decoded values
     */
    private static List<String> readStringArray(JsonArray array) {
        return mapArray(
                array,
                index -> array.get(index).getAsString()
        );
    }

    /**
     * Converts the JSON rows array into immutable Java lists.
     *
     * @param rowsArray JSON array containing result rows
     * @return decoded rows
     */
    private static List<List<String>> readRows(JsonArray rowsArray) {
        return mapArray(
                rowsArray,
                index -> readStringArray(
                        rowsArray.get(index).getAsJsonArray()
                )
        );
    }

    /**
     * Maps every element position of a JSON array using a functional
     * transformation.
     *
     * @param array source JSON array
     * @param mapper transformation applied to each position
     * @param <T> resulting element type
     * @return mapped immutable list
     */
    private static <T> List<T> mapArray(
            JsonArray array,
            IntFunction<T> mapper
    ) {
        return IntStream
                .range(0, array.size())
                .mapToObj(mapper)
                .toList();
    }

    /**
     * Determines whether a disk file has the JSON extension.
     *
     * @param path file path
     * @return true when the file name ends in .json
     */
    private static boolean isJsonFile(Path path) {
        return path
                .getFileName()
                .toString()
                .toLowerCase(Locale.ROOT)
                .endsWith(".json");
    }

    /**
     * Normalizes queries for comparison.
     *
     * @param query query text
     * @return normalized query
     */
    private static String normalize(String query) {
        return query
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Groups a query string with its decoded fake result.
     *
     * @param query query represented by the JSON document
     * @param result fake result associated with the query
     */
    private record QueryEntry(
            String query,
            FakeQueryResult result
    ) {
    }
}