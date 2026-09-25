package cr.ac.una.eif400.cyphail.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Loads JSON documents from disk for Cyphail fake data.
 *
 * This class centralizes the low-level file reading operation so that
 * components such as the fake query engine and graph catalog do not
 * duplicate JSON loading logic.
 *
 * JSON is parsed with Gson using its tree representation
 * ({@link JsonElement}) instead of annotated data transfer classes.
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
public final class JsonFileLoader {

    /**
     * Prevents instances of this utility class.
     */
    private JsonFileLoader() {
    }

    /**
     * Reads and parses a JSON document from the supplied disk path.
     *
     * The file is opened using UTF-8 and converted to Gson's tree
     * representation. The file is read every time this method is called,
     * allowing changes made to the JSON data on disk to be observed without
     * recompiling the application.
     *
     * @param path path of the JSON file to read
     * @return parsed JSON tree
     * @throws IOException when the file cannot be read
     * @throws NullPointerException when path is null
     */
    public static JsonElement load(Path path) throws IOException {
        Objects.requireNonNull(path, "path");

        try (Reader reader = Files.newBufferedReader(
                path,
                StandardCharsets.UTF_8
        )) {
            return JsonParser.parseReader(reader);
        }
    }
}