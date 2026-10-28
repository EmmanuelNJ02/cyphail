
package cr.ac.una.eif400.cyphail.frontend.handler;

import cr.ac.una.eif400.cyphail.model.GraphCatalog;
import cr.ac.una.eif400.cyphail.model.GraphInfo;

import java.util.List;
import java.util.Optional;

/**
 * Handles the .use command in the Cyphail REPL.
 *
 * Sprint P2 preserves the graph catalog introduced
 * during Sprint P1.
 *
 * This handler displays the available catalog entries
 * and checks whether a requested graph exists.
 *
 * The graph catalog is independent of the SWI-Prolog
 * query execution engine. This command does not
 * change the active graph in the Prolog engine.
 *
 * Sprint P2 executes Cyphail queries through
 * the real SWI-Prolog engine using MQI.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Programming Paradigms
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
public class UseCommandHandler implements CommandHandler {

    private static final String COMMAND = ".use";

    /**
     * Checks whether the input corresponds to the
     * .use command.
     *
     * Supported forms:
     *
     * .use
     * .use <graph>
     *
     * @param input command entered in the REPL
     * @return true when the command begins with .use
     */
    @Override
    public boolean supports(String input) {

        String trimmedInput = input.trim();

        return trimmedInput.equalsIgnoreCase(COMMAND)
                || trimmedInput.toLowerCase(
                        java.util.Locale.ROOT
                ).startsWith(COMMAND + " ");
    }

    /**
     * Processes the .use command.
     *
     * Without arguments, the command displays
     * the available graph catalog entries.
     *
     * With a graph name, it verifies whether
     * the requested graph exists in the catalog.
     *
     * This method does not switch the active
     * Prolog engine graph or execute a query.
     *
     * @param input command entered in the REPL
     * @return true to keep the REPL running
     */
    @Override
    public boolean handle(String input) {

        String trimmedInput = input.trim();

        if (trimmedInput.equalsIgnoreCase(COMMAND)) {

            showAvailableGraphs();

            return true;
        }

        String graphName = trimmedInput
                .substring(COMMAND.length())
                .trim();

        if (graphName.isEmpty()) {

            showAvailableGraphs();

            return true;
        }

        selectGraph(graphName);

        return true;
    }

    /**
     * Displays all graphs registered in the
     * legacy graph catalog.
     *
     * The catalog is maintained for Sprint P1
     * compatibility.
     *
     * No query is executed by this operation.
     */
    private void showAvailableGraphs() {

        List<GraphInfo> graphs = GraphCatalog.getGraphs();

        System.out.printf(
                "%-12s%s%n",
                "Graph",
                "Description"
        );

        System.out.println(
                "------------------------------------"
        );

        if (graphs.isEmpty()) {

            System.out.println(
                    "No graphs are registered in the catalog."
            );

        } else {

            for (GraphInfo graph : graphs) {

                System.out.printf(
                        "%-12s%s%n",
                        graph.getName(),
                        graph.getDescription()
                );
            }
        }

        System.out.println();

        System.out.println(
                "Graph catalog information displayed."
        );

        System.out.println(
                "No query was executed through SWI-Prolog/MQI."
        );

        System.out.println();
    }

    /**
     * Checks whether the requested graph exists
     * in the legacy graph catalog.
     *
     * This method preserves the P1 catalog
     * lookup behavior without reporting
     * simulated execution times.
     *
     * It does not change the active graph
     * used by the SWI-Prolog engine.
     *
     * @param graphName name of the requested graph
     */
    private void selectGraph(String graphName) {

        Optional<GraphInfo> graph =
                GraphCatalog.findByName(graphName);

        if (graph.isPresent()) {

            System.out.println(
                    "OK. Graph \""
                            + graph.get().getName()
                            + "\" is available in the catalog."
            );

            System.out.println(
                    "The active SWI-Prolog engine graph"
                            + " was not changed."
            );

        } else {

            System.out.println(
                    "ERROR. Graph \""
                            + graphName
                            + "\" is not available."
            );
        }

        System.out.println();
    }
}
