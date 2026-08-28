package cr.ac.una.eif400.cyphail.frontend.handler;

import cr.ac.una.eif400.cyphail.model.GraphCatalog;
import cr.ac.una.eif400.cyphail.model.GraphInfo;

import java.util.List;
import java.util.Optional;

/**
 * Handles the .use command in the Cyphail REPL.
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
public class UseCommandHandler implements CommandHandler {

    private static final String COMMAND = ".use";

    /**
     * Checks whether the input corresponds to the .use command.
     *
     * Supported forms:
     * .use
     * .use <graph>
     *
     * @param input command entered in the REPL
     * @return true when the command starts with .use
     */
    @Override
    public boolean supports(String input) {
        String trimmedInput = input.trim();

        return trimmedInput.equalsIgnoreCase(COMMAND)
                || trimmedInput.toLowerCase().startsWith(COMMAND + " ");
    }

    /**
     * Processes the .use command.
     *
     * Without arguments, it displays the available graphs.
     * With a graph name, it simulates access to that graph.
     *
     * @param input command entered in the REPL
     * @return true because the REPL should continue running
     */
    @Override
    public boolean handle(String input) {
        String trimmedInput = input.trim();

        if (trimmedInput.equalsIgnoreCase(COMMAND)) {
            showAvailableGraphs();
            return true;
        }

        String graphName = trimmedInput.substring(COMMAND.length()).trim();

        if (graphName.isEmpty()) {
            showAvailableGraphs();
            return true;
        }

        selectGraph(graphName);

        return true;
    }

    /**
     * Displays the fake list of graphs available in Sprint P1.1.
     */
    private void showAvailableGraphs() {
        List<GraphInfo> graphs = GraphCatalog.getGraphs();

        System.out.printf("%-12s%s%n", "Graph", "Description");
        System.out.println("------------------------------------");

        for (GraphInfo graph : graphs) {
            System.out.printf(
                    "%-12s%s%n",
                    graph.getName(),
                    graph.getDescription()
            );
        }

        System.out.println();
        System.out.println("OK. Query available after 5 ms.");
        System.out.println();
    }

    /**
     * Simulates access to an available graph.
     *
     * @param graphName graph requested by the user
     */
    private void selectGraph(String graphName) {
        Optional<GraphInfo> graph = GraphCatalog.findByName(graphName);

        if (graph.isPresent()) {
            System.out.println(
                    "OK. \"" + graph.get().getName()
                            + "\" graph available after 1 ms."
            );
        } else {
            System.out.println(
                    "ERROR. Graph \"" + graphName
                            + "\" is not available."
            );
        }

        System.out.println();
    }
}