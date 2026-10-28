
package cr.ac.una.eif400.cyphail.frontend;

import cr.ac.una.eif400.cyphail.engine.mqi.MqiBroker;

import picocli.CommandLine.Command;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Picocli command that starts the Cyphail interactive REPL.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Paradigmas de Programacion
 * University: Universidad Nacional de Costa Rica
 * School: Escuela de Informatica
 * Group Code: 04-10am
 *
 * Authors:
 * Emmanuel Nunez Jimenez
 * Valery Alfaro Morales
 * Roy Arias Mejia
 * Keynell Molina Mora
 * Julissa Solano Valverde
 *
 * Sprint P2:
 *
 * Starts a real SWI-Prolog MQI session and shares
 * that session with the interactive Cyphail REPL.
 *
 * The same broker is used for every query, including
 * statements loaded through the .load command.
 *
 * Resources are closed automatically when the
 * REPL exits.
 *
 * @author Emmanuel Nunez Jimenez
 * @author Valery Alfaro Morales
 * @author Roy Arias Mejia
 * @author Keynell Molina Mora
 * @author Julissa Solano Valverde
 */
@Command(
        name = "repl",
        description = "Starts the interactive Cyphail REPL."
)
public class ReplCommand implements Runnable {

    /**
     * Starts the production Cyphail REPL
     * with a persistent SWI-Prolog MQI connection.
     */
    @Override
    public void run() {

        // =====================================
        // STEP 1 - LOCATE PROLOG ENGINE
        // =====================================

        Path engineFile = Path.of(
                "src",
                "main",
                "prolog",
                "engine.pl"
        ).toAbsolutePath().normalize();

        // =====================================
        // STEP 2 - START MQI SESSION
        // =====================================

        try (MqiBroker broker =
                     MqiBroker.start(engineFile)) {

            // =================================
            // STEP 3 - CREATE REAL ROUTER
            // =================================

            /*
             * The production router receives
             * the authenticated MQI broker.
             *
             * It uses PrologQueryHandler instead
             * of FakeQueryHandler.
             */
            CommandRouter router =
                    new CommandRouter(broker);

            // =================================
            // STEP 4 - CREATE REPL
            // =================================

            /*
             * Keep the existing multiline
             * statement buffer.
             *
             * This preserves the P2 behavior
             * of accepting Cyphail statements
             * until their terminating semicolon.
             */
            CyphailStatementBuffer buffer =
                    new CyphailStatementBuffer();

            Repl repl = new Repl(
                    router,
                    buffer
            );

            // =================================
            // STEP 5 - START INTERACTIVE REPL
            // =================================

            repl.start();

            /*
             * When repl.start() finishes,
             * try-with-resources automatically
             * closes the broker and terminates
             * the SWI-Prolog child process.
             */

        } catch (IOException exception) {

            // =================================
            // MQI STARTUP OR SHUTDOWN ERROR
            // =================================

            System.err.println(
                    "Unable to start or close "
                            + "the SWI-Prolog MQI session."
            );

            System.err.println(
                    "Details: "
                            + exception.getMessage()
            );

            System.err.println(
                    "Verify that SWI-Prolog is installed, "
                            + "the swipl command is available, "
                            + "and engine.pl exists."
            );
        }
    }
}
