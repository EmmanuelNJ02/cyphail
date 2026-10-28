

package cr.ac.una.eif400.cyphail.frontend.handler;

/**
 * Handles the .about command in the Cyphail REPL.
 *
 * Displays information about the Cyphail project,
 * the development team, and the features implemented
 * during Sprint P2.
 *
 * Sprint P2 preserves the applicable Sprint P1
 * functionality and introduces real query execution
 * through SWI-Prolog and the Machine Query Interface.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Programming Paradigms
 * University: National University of Costa Rica
 * School: School of Informatics
 * Work Group: 04
 * Schedule: 10:00 a.m.
 * Group Code: 04-10am
 * Date: November 2026
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
public class AboutCommandHandler implements CommandHandler {

    private static final String COMMAND = ".about";

    /**
     * Checks whether the input corresponds to
     * the .about command.
     *
     * @param input command entered in the REPL
     * @return true when the input is .about
     */
    @Override
    public boolean supports(String input) {
        return COMMAND.equalsIgnoreCase(input.trim());
    }

    /**
     * Displays project, sprint, technology,
     * functionality, and author information.
     *
     * The original project and group identifiers
     * are preserved for backward compatibility.
     *
     * @param input command entered in the REPL
     * @return true because the REPL should
     *         continue running
     */
    @Override
    public boolean handle(String input) {

        // ==========================================
        // PROJECT INFORMATION
        // ==========================================

        System.out.println("Cyphail");
        System.out.println("Version: 0.1.0");
        System.out.println("Sprint: P2");
        System.out.println("Date: November 2026");
        System.out.println();

        // ==========================================
        // ACADEMIC INFORMATION
        // ==========================================

        System.out.println(
                "Course: EIF400 - Programming Paradigms"
        );

        System.out.println(
                "University: National University of Costa Rica"
        );

        System.out.println(
                "School: School of Informatics"
        );

        System.out.println("Work Group: 04");
        System.out.println("Schedule: 10:00 a.m.");
        System.out.println("Group Code: 04-10am");
        System.out.println();

        // ==========================================
        // PROJECT DESCRIPTION
        // ==========================================

        System.out.println("Project Description:");

        System.out.println(
                "Cyphail is an academic graph query language prototype."
        );

        System.out.println(
                "Sprint P2 extends the previous compiler and REPL"
        );

        System.out.println(
                "with real query execution using SWI-Prolog and MQI."
        );

        System.out.println();

        // ==========================================
        // SPRINT P1 FEATURES PRESERVED
        // ==========================================

        System.out.println("Sprint P1 Features Preserved:");

        System.out.println(
                "- Interactive Cyphail REPL"
        );

        System.out.println(
                "- Lexical analysis and parsing"
        );

        System.out.println(
                "- Functional parser combinators"
        );

        System.out.println(
                "- Abstract Syntax Tree (AST)"
        );

        System.out.println(
                "- Static semantic validation"
        );

        System.out.println(
                "- AST visualization using .tree"
        );

        System.out.println(
                "- Modular command routing"
        );

        System.out.println(
                "- Legacy graph catalog"
        );

        System.out.println();

        // ==========================================
        // SPRINT P2 FEATURES
        // ==========================================

        System.out.println("Sprint P2 Features:");

        System.out.println(
                "- Multiline Cyphail statements"
        );

        System.out.println(
                "- Semicolon-terminated queries"
        );

        System.out.println(
                "- Script execution using .load"
        );

        System.out.println(
                "- Node and relationship patterns"
        );

        System.out.println(
                "- Relationship direction support"
        );

        System.out.println(
                "- MATCH and WHERE queries"
        );

        System.out.println(
                "- CREATE nodes and relationships"
        );

        System.out.println(
                "- SET and REMOVE properties"
        );

        System.out.println(
                "- DELETE and DETACH DELETE"
        );

        System.out.println(
                "- AST-to-Prolog code generation"
        );

        System.out.println(
                "- Cyphail-to-Prolog variable mapping"
        );

        System.out.println(
                "- Real SWI-Prolog query execution"
        );

        System.out.println(
                "- MQI socket communication"
        );

        System.out.println(
                "- JSON response processing with Gson"
        );

        System.out.println(
                "- Tabular query result display"
        );

        System.out.println(
                "- Additional static semantic checks"
        );

        System.out.println();

        // ==========================================
        // EXECUTION ARCHITECTURE
        // ==========================================

        System.out.println("Execution Architecture:");

        System.out.println(
                "REPL -> Parser -> AST -> Semantic Analyzer"
        );

        System.out.println(
                "     -> Code Generator -> MQI Broker"
        );

        System.out.println(
                "     -> SWI-Prolog Engine -> JSON Results"
        );

        System.out.println(
                "     -> Tabular Output"
        );

        System.out.println();

        // ==========================================
        // TECHNOLOGIES
        // ==========================================

        System.out.println("Technologies:");

        System.out.println(
                "- Java 26"
        );

        System.out.println(
                "- Apache Maven"
        );

        System.out.println(
                "- JUnit 5"
        );

        System.out.println(
                "- picocli"
        );

        System.out.println(
                "- Gson"
        );

        System.out.println(
                "- SWI-Prolog"
        );

        System.out.println(
                "- Machine Query Interface (MQI)"
        );

        System.out.println(
                "- Git and GitHub"
        );

        System.out.println();

        // ==========================================
        // PROJECT SCOPE
        // ==========================================

        System.out.println("Project Scope:");

        System.out.println(
                "- Queries are executed by the real Prolog engine."
        );

        System.out.println(
                "- Graph changes remain available during the session."
        );

        System.out.println(
                "- The legacy .use catalog does not switch"
        );

        System.out.println(
                "  the active Prolog engine graph."
        );

        System.out.println(
                "- Sprint P2 replaces the simulated query engine."
        );

        System.out.println();

        // ==========================================
        // TEAM MEMBERS
        // ==========================================

        System.out.println("Authors:");

        System.out.println(
                "- Emmanuel Nunez Jimenez"
        );

        System.out.println(
                "- Valery Alfaro Morales"
        );

        System.out.println(
                "- Roy Arias Mejia"
        );

        System.out.println(
                "- Keynell Molina Mora"
        );

        System.out.println(
                "- Julissa Solano Valverde"
        );

        System.out.println();

        return true;
    }
}
