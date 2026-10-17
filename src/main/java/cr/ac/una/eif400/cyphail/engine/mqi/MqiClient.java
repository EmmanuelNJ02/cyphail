

package cr.ac.una.eif400.cyphail.engine.mqi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Cliente MQI de SWI-Prolog para Cyphail P2, grupo 04-10am.
 *
 * Adaptado del demo MQI suministrado por el profesor (autoria
 * original del archivo de demostracion atribuida a Claude).
 *
 * El saludo de autenticacion tiene un formato distinto al de
 * los resultados de consultas: incluye threads/2 y version/2.
 * No debe interpretarse como una lista de bindings con "=".
 */
public final class MqiClient implements AutoCloseable {

    private static final int MAX_MESSAGE_BYTES = 16 * 1024 * 1024;
    private static final int MAX_HEADER_DIGITS = 10;

    private final InputStream input;
    private final OutputStream output;
    private boolean closed;

    public MqiClient(InputStream input, OutputStream output) {
        this.input = new BufferedInputStream(
                Objects.requireNonNull(input, "input"));
        this.output = Objects.requireNonNull(output, "output");
    }

    /**
     * Envia la clave y valida el saludo de MQI.
     */
    public void authenticate(String password) throws IOException {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "La clave MQI no puede estar vacia.");
        }

        send(password);
        validateHandshake(receive());
    }

    /**
     * El saludo de MQI contiene threads/2 y version/2.
     * No contiene bindings normales de una consulta.
     *
     * Se acepta true booleano para mantener las pruebas
     * unitarias existentes.
     */
    private static void validateHandshake(String response)
            throws IOException {

        final JsonElement root;

        try {
            root = JsonParser.parseString(response);
        } catch (JsonParseException exception) {
            throw new IOException(
                    "Respuesta de autenticacion MQI no es JSON valido.",
                    exception);
        }

        if (root.isJsonPrimitive()
                && root.getAsJsonPrimitive().isBoolean()) {

            if (root.getAsBoolean()) {
                return;
            }

            throw new IOException(
                    "El servidor MQI rechazo la autenticacion.");
        }

        if (!root.isJsonObject()) {
            throw new IOException(
                    "Formato de autenticacion MQI inesperado.");
        }

        JsonObject object = root.getAsJsonObject();
        JsonElement functorValue = object.get("functor");

        if (functorValue == null
                || !functorValue.isJsonPrimitive()
                || !functorValue.getAsJsonPrimitive().isString()) {

            throw new IOException(
                    "La respuesta MQI no contiene un functor valido.");
        }

        String functor = functorValue.getAsString();

        if ("exception".equals(functor)) {
            throw new IOException(
                    "SWI-Prolog rechazo la autenticacion: " + object);
        }

        if (!"true".equals(functor)) {
            throw new IOException(
                    "Respuesta MQI de autenticacion inesperada: "
                    + functor);
        }

        JsonElement argsValue = object.get("args");

        if (argsValue == null || !argsValue.isJsonArray()) {
            throw new IOException(
                    "El saludo de MQI no contiene argumentos.");
        }

        JsonArray args = argsValue.getAsJsonArray();

        if (args.isEmpty() || !args.get(0).isJsonArray()) {
            throw new IOException(
                    "El saludo de MQI no contiene soluciones.");
        }

        JsonArray solutions = args.get(0).getAsJsonArray();

        if (solutions.isEmpty()
                || !solutions.get(0).isJsonArray()) {

            throw new IOException(
                    "El saludo de MQI no contiene datos de conexion.");
        }

        boolean hasThreads = false;

        for (JsonElement entry :
                solutions.get(0).getAsJsonArray()) {

            if (!entry.isJsonObject()) {
                continue;
            }

            JsonObject term = entry.getAsJsonObject();

            JsonElement name = term.get("functor");
            JsonElement values = term.get("args");

            if (name != null
                    && name.isJsonPrimitive()
                    && "threads".equals(name.getAsString())
                    && values != null
                    && values.isJsonArray()
                    && values.getAsJsonArray().size() == 2) {

                hasThreads = true;
            }
        }

        if (!hasThreads) {
            throw new IOException(
                    "Saludo MQI sin confirmacion threads/2.");
        }
    }

    /**
     * Ejecuta un objetivo sin limite de tiempo.
     */
    public PrologResult run(String goal) throws IOException {
        return run(goal, -1);
    }

    /**
     * Ejecuta un objetivo Prolog mediante run/2.
     * -1 indica que no hay limite de tiempo.
     */
    public PrologResult run(
            String goal,
            int timeoutSeconds) throws IOException {

        if (goal == null || goal.isBlank()) {
            throw new IllegalArgumentException(
                    "El objetivo Prolog no puede estar vacio.");
        }

        if (timeoutSeconds < -1) {
            throw new IllegalArgumentException(
                    "Tiempo limite MQI no valido.");
        }

        String normalized = goal.trim();

        if (normalized.endsWith(".")) {
            throw new IllegalArgumentException(
                    "El objetivo debe ingresarse sin punto final.");
        }

        send("run(("
                + normalized
                + "), "
                + timeoutSeconds
                + ")");

        try {
            return MqiResponseParser.parse(receive());
        } catch (RuntimeException exception) {
            throw new IOException(
                    "No fue posible interpretar el resultado JSON de MQI.",
                    exception);
        }
    }

    /**
     * Envia mensajes respetando la longitud en bytes UTF-8.
     */
    private void send(String message) throws IOException {

        if (closed) {
            throw new IOException(
                    "La conexion MQI esta cerrada.");
        }

        byte[] payload = (message + ".\n")
                .getBytes(StandardCharsets.UTF_8);

        if (payload.length > MAX_MESSAGE_BYTES) {
            throw new IOException(
                    "El mensaje MQI supera el limite permitido.");
        }

        byte[] header = (payload.length + ".\n")
                .getBytes(StandardCharsets.US_ASCII);

        output.write(header);
        output.write(payload);
        output.flush();
    }

    /**
     * Recibe una respuesta completa de SWI-Prolog.
     * Admite heartbeats antes de la cabecera.
     */
    private String receive() throws IOException {

        if (closed) {
            throw new IOException(
                    "La conexion MQI esta cerrada.");
        }

        int character = input.read();

        // Descartar señales de espera.
        while (character == '.') {
            character = input.read();
        }

        if (character == -1) {
            throw new EOFException(
                    "Conexion cerrada antes de la cabecera MQI.");
        }

        StringBuilder lengthText = new StringBuilder();

        while (character >= '0' && character <= '9') {

            if (lengthText.length() >= MAX_HEADER_DIGITS) {
                throw new IOException(
                        "Cabecera MQI demasiado larga.");
            }

            lengthText.append((char) character);
            character = input.read();
        }

        if (character == -1) {
            throw new EOFException(
                    "Conexion cerrada durante la cabecera MQI.");
        }

        if (lengthText.isEmpty() || character != '.') {
            throw new IOException(
                    "Cabecera MQI incorrecta: " + lengthText);
        }

        int terminator = input.read();

        if (terminator == '\r') {
            terminator = input.read();
        }

        if (terminator != '\n') {
            throw new IOException(
                    "Terminador de cabecera MQI no valido: "
                    + terminator);
        }

        final int length;

        try {
            length = Integer.parseInt(lengthText.toString());
        } catch (NumberFormatException exception) {
            throw new IOException(
                    "Longitud MQI incorrecta.",
                    exception);
        }

        if (length <= 0 || length > MAX_MESSAGE_BYTES) {
            throw new IOException(
                    "Tamano de respuesta MQI fuera de limite: "
                    + length);
        }

        byte[] payload = input.readNBytes(length);

        if (payload.length != length) {
            throw new EOFException(
                    "Respuesta MQI incompleta: "
                    + payload.length + "/" + length);
        }

        String body = new String(
                payload,
                StandardCharsets.UTF_8).strip();

        if (body.endsWith(".")) {
            body = body.substring(
                    0,
                    body.length() - 1).stripTrailing();
        }

        if (body.isEmpty()) {
            throw new IOException(
                    "Respuesta MQI vacia.");
        }

        return body;
    }

    /**
     * Cierra la conexion y libera sus flujos.
     */
    @Override
    public void close() throws IOException {

        if (closed) {
            return;
        }

        IOException error = null;

        try {
            send("close");
            receive();
        } catch (IOException exception) {
            error = exception;
        } finally {

            closed = true;

            try {
                output.close();
            } catch (IOException exception) {

                if (error == null) {
                    error = exception;
                } else {
                    error.addSuppressed(exception);
                }
            }

            try {
                input.close();
            } catch (IOException exception) {

                if (error == null) {
                    error = exception;
                } else {
                    error.addSuppressed(exception);
                }
            }
        }

        if (error != null) {
            throw error;
        }
    }
}

