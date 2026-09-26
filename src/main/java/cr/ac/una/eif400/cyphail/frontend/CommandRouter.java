package cr.ac.una.eif400.cyphail.frontend;

import cr.ac.una.eif400.cyphail.frontend.handler.AboutCommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.CommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.ExitCommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.FakeQueryHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.HelpCommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.TreeCommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.UseCommandHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Routes REPL input to the appropriate Cyphail command handler.
 *
 * <p>Project: Cyphail</p>
 * <p>Course: EIF400 - Paradigmas de Programacion</p>
 * <p>Universidad Nacional de Costa Rica - Escuela de Informatica</p>
 * <p>Group Code: 04-10am</p>
 *
 * <p>Authors:</p>
 * <ul>
 *     <li>Emmanuel Nunez Jimenez</li>
 *     <li>Valery Alfaro Morales</li>
 *     <li>Roy Arias Mejia</li>
 *     <li>Keynell Molina Mora</li>
 *     <li>Julissa Solano Valverde</li>
 * </ul>
 *
 * @author Emmanuel Nunez Jimenez
 * @author Valery Alfaro Morales
 * @author Roy Arias Mejia
 * @author Keynell Molina Mora
 * @author Julissa Solano Valverde
 */
public final class CommandRouter {

    private final List<CommandHandler> handlers;

    /**
     * Creates the router and registers the REPL command handlers.
     *
     * <p>TreeCommandHandler is registered before FakeQueryHandler so
     * query lines entered after .tree are captured by the Sprint P1
     * parser/analyzer flow instead of being executed by the P1.1
     * fake query engine.</p>
     */
    public CommandRouter() {
        handlers = new ArrayList<>();

        handlers.add(new HelpCommandHandler());
        handlers.add(new AboutCommandHandler());
        handlers.add(new UseCommandHandler());
        handlers.add(new TreeCommandHandler());
        handlers.add(new FakeQueryHandler());
        handlers.add(new ExitCommandHandler());
    }

    /**
     * Routes the input to the first handler that supports it.
     *
     * @param input input entered by the user
     * @return true if the REPL should continue running;
     *         false if it should terminate
     */
    public boolean route(String input) {
        for (CommandHandler handler : handlers) {
            if (handler.supports(input)) {
                return handler.handle(input);
            }
        }

        System.out.println(
                "Command or query not implemented yet: " + input
        );
        System.out.println();

        return true;
    }
}