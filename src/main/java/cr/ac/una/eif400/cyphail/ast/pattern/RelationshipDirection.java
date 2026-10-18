package cr.ac.una.eif400.cyphail.ast.pattern;

/**
 * Represents the direction of a relationship in a Cyphail pattern.
 *
 * <p>Sprint P2 introduces relationship patterns with three possible
 * directions: incoming, outgoing and undirected.</p>
 *
 * <p>Examples:</p>
 * <pre>
 * (a)-[r:KNOWS]->(b)  OUT
 * (a)<-[r:KNOWS]-(b)  IN
 * (a)-[r:KNOWS]-(b)   NONE
 * </pre>
 *
 * <p>Project: Cyphail - Sprint P2</p>
 * <p>Course: EIF400 - Paradigmas de Programacion</p>
 * <p>Universidad Nacional de Costa Rica</p>
 * <p>Escuela de Informatica</p>
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
 * <p>Sprint P2 contribution:
 * Relationship direction model.</p>
 */
public enum RelationshipDirection {

    /**
     * Relationship directed from the left node to the right node.
     *
     * Example: (a)-[r:KNOWS]->(b)
     */
    OUT,

    /**
     * Relationship directed from the right node to the left node.
     *
     * Example: (a)<-[r:KNOWS]-(b)
     */
    IN,

    /**
     * Relationship without a specified direction.
     *
     * Example: (a)-[r:KNOWS]-(b)
     */
    NONE
}