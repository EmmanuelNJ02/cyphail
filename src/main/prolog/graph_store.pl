/*
 * Universidad Nacional de Costa Rica
 * School of Informatics
 *
 * Course: EIF400 - Programming Paradigms
 * Project: Cyphail
 * Sprint: P2
 * Group: 04-10am
 *
 * Team Members:
 * - Emmanuel Nunez Jimenez
 * - Julissa Solano Valverde
 * - Valery Alfaro Morales
 * - Roy Arias Mejia
 * - Keynell Molina Mora
 *
 * File: graph_store.pl
 *
 * Description:
 *
 * In-memory graph storage for the Cyphail
 * SWI-Prolog execution engine.
 *
 * Provides:
 *
 * - Node creation and retrieval.
 * - Relationship creation and retrieval.
 * - Node and relationship properties.
 * - SET property updates.
 * - REMOVE property operations.
 * - DELETE node and relationship operations.
 *
 * Graph elements created by Cyphail are stored
 * in the SWI-Prolog recorded database.
 *
 * Sprint P2:
 *
 * SET additionally supports original sample
 * graph nodes defined in engine.pl.
 *
 * REMOVE additionally supports original sample
 * graph nodes and relationships.
 *
 * IMPORTANT:
 *
 * engine.pl must declare the following
 * predicates as dynamic:
 *
 * user_amigos_node_prop/3
 * user_amigos_edge_prop/3
 */


% ============================================
% PROPERTY VALIDATION
% ============================================

% Verifies that a property contains
% an atom key and an instantiated value.

cy_valid_property(Key-Value) :-
    atom(Key),
    ground(Value).


% ============================================
% NODE CREATION
% ============================================

% Creates a node with a generated identifier.
%
% Graph      - Graph name.
% Labels     - Node labels.
% Properties - Key-Value pairs.
% NodeId     - Generated node identifier.

cy_create_node(Graph, Labels, Properties, NodeId) :-
    atom(Graph),
    is_list(Labels),
    Labels \= [],
    maplist(atom, Labels),
    is_list(Properties),
    maplist(cy_valid_property, Properties),

    flag(
        cyphail_node_counter,
        Number,
        Number + 1
    ),

    format(
        atom(NodeId),
        'cy_node_~d',
        [Number]
    ),

    recordz(
        cyphail_graph_nodes,
        node(
            Graph,
            NodeId,
            Labels,
            Properties
        ),
        _
    ).


% ============================================
% NODE RETRIEVAL
% ============================================

% Retrieves a node from the recorded database.

cy_get_node(Graph, NodeId, Labels, Properties) :-
    recorded(
        cyphail_graph_nodes,
        node(
            Graph,
            NodeId,
            Labels,
            Properties
        ),
        _
    ).


% ============================================
% NODE PROPERTY RETRIEVAL
% ============================================

% Retrieves a property from a stored node.

cy_get_node_property(Graph, NodeId, Key, Value) :-
    cy_get_node(
        Graph,
        NodeId,
        _,
        Properties
    ),

    member(
        Key-Value,
        Properties
    ).


% ============================================
% RELATIONSHIP CREATION
% ============================================

% Creates a directed relationship between
% two existing stored nodes.
%
% Graph      - Graph name.
% From       - Source node identifier.
% To         - Destination node identifier.
% Type       - Relationship type.
% Properties - Relationship properties.
% EdgeId     - Generated relationship identifier.

cy_create_edge(
    Graph,
    From,
    To,
    Type,
    Properties,
    EdgeId
) :-
    atom(Graph),
    atom(Type),
    Type \= '',

    cy_get_node(
        Graph,
        From,
        _,
        _
    ),

    cy_get_node(
        Graph,
        To,
        _,
        _
    ),

    is_list(Properties),

    maplist(
        cy_valid_property,
        Properties
    ),

    flag(
        cyphail_edge_counter,
        Number,
        Number + 1
    ),

    format(
        atom(EdgeId),
        'cy_edge_~d',
        [Number]
    ),

    recordz(
        cyphail_graph_edges,
        edge(
            Graph,
            EdgeId,
            Type,
            From,
            To,
            Properties
        ),
        _
    ).


% ============================================
% RELATIONSHIP RETRIEVAL
% ============================================

% Retrieves a stored relationship.

cy_get_edge(
    Graph,
    EdgeId,
    Type,
    From,
    To,
    Properties
) :-
    recorded(
        cyphail_graph_edges,
        edge(
            Graph,
            EdgeId,
            Type,
            From,
            To,
            Properties
        ),
        _
    ).


% ============================================
% RELATIONSHIP PROPERTY RETRIEVAL
% ============================================

% Retrieves a property from a relationship
% stored in the recorded database.

cy_get_edge_property(Graph, EdgeId, Key, Value) :-
    cy_get_edge(
        Graph,
        EdgeId,
        _,
        _,
        _,
        Properties
    ),

    member(
        Key-Value,
        Properties
    ).


% ============================================
% PROPERTY UPDATE
% ============================================

% Replaces an existing property or adds
% a new property when the key does not exist.
%
% Original properties are not modified
% directly; an updated list is produced.

cy_update_property(
    Key,
    Value,
    Properties,
    UpdatedProperties
) :-
    atom(Key),
    ground(Value),

    (
        select(
            Key-_,
            Properties,
            RemainingProperties
        )
    ->
        true
    ;
        RemainingProperties = Properties
    ),

    UpdatedProperties = [
        Key-Value
        | RemainingProperties
    ].


% ============================================
% NODE PROPERTY UPDATE - STORED NODES
% ============================================

% Updates a node created through CREATE.
%
% The node is located in the recorded
% database, updated and stored again.
%
% The old record is removed after the
% replacement has been created.
%
% This preserves the existing Sprint P2
% CREATE and SET behavior.

cy_set_node_property(Graph, NodeId, Key, Value) :-
    atom(Key),
    ground(Value),

    recorded(
        cyphail_graph_nodes,
        node(
            Graph,
            NodeId,
            Labels,
            Properties
        ),
        Reference
    ),

    cy_update_property(
        Key,
        Value,
        Properties,
        UpdatedProperties
    ),

    recordz(
        cyphail_graph_nodes,
        node(
            Graph,
            NodeId,
            Labels,
            UpdatedProperties
        ),
        _
    ),

    erase(Reference).


% ============================================
% NODE PROPERTY UPDATE - ORIGINAL GRAPH
% ============================================

% Sprint P2 SET support for original nodes.
%
% The sample graph in engine.pl defines
% nodes such as:
%
% user_amigos_node(id1).
%
% and properties such as:
%
% user_amigos_node_prop(id1, age, 30).
%
% These original nodes are not contained
% in cyphail_graph_nodes.
%
% Therefore, SET requires an additional
% implementation for these nodes.
%
% Example:
%
% MATCH (a:Person {name: 'Juan Perez'})
% SET a.age = 31
% RETURN a.age
%
% The previous age fact is removed and
% replaced by the updated property fact.
%
% The operation is persistent throughout
% the current SWI-Prolog process.
%
% This clause only applies to original
% sample nodes, not CREATE nodes.
%
% IMPORTANT:
%
% user_amigos_node_prop/3 must be declared
% dynamic in engine.pl before use.

cy_set_node_property(amigos, NodeId, Key, Value) :-
    atom(NodeId),
    atom(Key),
    ground(Value),

    % The node must not be a CREATE node.

    \+ cy_get_node(
        amigos,
        NodeId,
        _,
        _
    ),

    % Verify that the original node exists.

    user_amigos_node(NodeId),

    % Remove all previous facts for this key.
    %
    % retractall is necessary because some
    % sample nodes have repeated property facts.

    retractall(
        user_amigos_node_prop(
            NodeId,
            Key,
            _
        )
    ),

    % Persist the new property value.

    assertz(
        user_amigos_node_prop(
            NodeId,
            Key,
            Value
        )
    ).


% ============================================
% RELATIONSHIP PROPERTY UPDATE
% ============================================

% Updates an existing relationship stored
% in the Cyphail recorded database.
%
% Graph  - Graph name.
% EdgeId - Relationship identifier.
% Key    - Property name.
% Value  - New property value.

cy_set_edge_property(Graph, EdgeId, Key, Value) :-
    atom(Key),
    ground(Value),

    recorded(
        cyphail_graph_edges,
        edge(
            Graph,
            EdgeId,
            Type,
            From,
            To,
            Properties
        ),
        Reference
    ),

    cy_update_property(
        Key,
        Value,
        Properties,
        UpdatedProperties
    ),

    recordz(
        cyphail_graph_edges,
        edge(
            Graph,
            EdgeId,
            Type,
            From,
            To,
            UpdatedProperties
        ),
        _
    ),

    erase(Reference).


% ============================================
% PROPERTY REMOVAL
% ============================================

% Removes all property pairs having
% the requested key.
%
% Removing a nonexistent property leaves
% the remaining properties unchanged.

cy_remove_property(
    Key,
    Properties,
    UpdatedProperties
) :-
    atom(Key),

    findall(
        ExistingKey-ExistingValue,
        (
            member(
                ExistingKey-ExistingValue,
                Properties
            ),

            dif(
                ExistingKey,
                Key
            )
        ),
        UpdatedProperties
    ).


% ============================================
% NODE PROPERTY REMOVAL - STORED NODES
% ============================================

% Removes a property from a node stored
% in the recorded database.
%
% Graph  - Graph name.
% NodeId - Node identifier.
% Key    - Property to remove.
%
% This clause preserves the existing behavior
% for nodes created with CREATE.

cy_remove_node_property(Graph, NodeId, Key) :-
    atom(Graph),
    atom(NodeId),
    atom(Key),

    recorded(
        cyphail_graph_nodes,
        node(
            Graph,
            NodeId,
            Labels,
            Properties
        ),
        Reference
    ),

    cy_remove_property(
        Key,
        Properties,
        UpdatedProperties
    ),

    recordz(
        cyphail_graph_nodes,
        node(
            Graph,
            NodeId,
            Labels,
            UpdatedProperties
        ),
        _
    ),

    erase(Reference).


% ============================================
% NODE PROPERTY REMOVAL - ORIGINAL GRAPH
% ============================================

% Sprint P2 REMOVE support for original
% nodes defined in engine.pl.
%
% These nodes use dynamic Prolog facts
% rather than the recorded database.
%
% Example:
%
% MATCH (a:Person {name: 'Juan Perez'})
% REMOVE a.age
% RETURN a.name
%
% The operation removes only the requested
% property, preserving the original node,
% labels, relationships and other properties.
%
% Example original facts:
%
% user_amigos_node(id1).
% user_amigos_node_prop(id1, name, 'Juan Perez').
% user_amigos_node_prop(id1, age, 30).
%
% After REMOVE a.age:
%
% user_amigos_node(id1).
% user_amigos_node_prop(id1, name, 'Juan Perez').
%
% The age fact no longer exists.
%
% Repeated property facts are all removed
% for the requested key.
%
% Removing a nonexistent property succeeds
% without changing the original node.
%
% The update persists for the lifetime
% of the active SWI-Prolog process.
%
% IMPORTANT:
%
% engine.pl must declare
% user_amigos_node_prop/3 as dynamic.

cy_remove_node_property(amigos, NodeId, Key) :-
    atom(NodeId),
    atom(Key),

    % Only handle original sample nodes.
    % Stored CREATE nodes are handled above.

    \+ cy_get_node(
        amigos,
        NodeId,
        _,
        _
    ),

    % Verify that the original node exists.

    user_amigos_node(NodeId),

    % Remove every property fact matching
    % the requested node and property key.

    retractall(
        user_amigos_node_prop(
            NodeId,
            Key,
            _
        )
    ).


% ============================================
% RELATIONSHIP PROPERTY REMOVAL - STORED EDGES
% ============================================

% Removes a property from a relationship
% stored in the recorded database.
%
% Preserves the other relationship properties
% and the relationship itself.

cy_remove_edge_property(Graph, EdgeId, Key) :-
    atom(Graph),
    atom(EdgeId),
    atom(Key),

    recorded(
        cyphail_graph_edges,
        edge(
            Graph,
            EdgeId,
            Type,
            From,
            To,
            Properties
        ),
        Reference
    ),

    cy_remove_property(
        Key,
        Properties,
        UpdatedProperties
    ),

    recordz(
        cyphail_graph_edges,
        edge(
            Graph,
            EdgeId,
            Type,
            From,
            To,
            UpdatedProperties
        ),
        _
    ),

    erase(Reference).


% ============================================
% RELATIONSHIP PROPERTY REMOVAL - ORIGINAL GRAPH
% ============================================

% Sprint P2 REMOVE support for original
% relationships defined in engine.pl.
%
% Original relationships include:
%
% user_amigos_edge(e1, id1, id2).
% user_amigos_edge(e2, id1, id4).
% user_amigos_edge(e3, id2, id1).
% user_amigos_edge(e4, id2, id3).
%
% They are not stored in the recorded database.
%
% Their optional properties are represented by
% dynamic facts of the following form:
%
% user_amigos_edge_prop(EdgeId, Key, Value).
%
% This clause removes all facts matching
% the relationship ID and property name.
%
% The relationship, its type and its
% endpoints are preserved.
%
% Removing a nonexistent property succeeds
% when the relationship itself exists.
%
% IMPORTANT:
%
% engine.pl must declare
% user_amigos_edge_prop/3 as dynamic.

cy_remove_edge_property(amigos, EdgeId, Key) :-
    atom(EdgeId),
    atom(Key),

    % This implementation applies only
    % to original sample relationships.
    %
    % Relationships created through CREATE
    % are handled by the preceding clause.

    \+ cy_get_edge(
        amigos,
        EdgeId,
        _,
        _,
        _,
        _
    ),

    % The original relationship must exist.

    user_amigos_edge(
        EdgeId,
        _,
        _
    ),

    % Remove only the requested property.
    %
    % retractall also removes duplicates
    % for the same property key.

    retractall(
        user_amigos_edge_prop(
            EdgeId,
            Key,
            _
        )
    ).


% ============================================
% RELATIONSHIP DELETION
% ============================================

% Deletes an existing stored relationship.
%
% The source and destination nodes remain
% unchanged.
%
% The operation fails when the relationship
% does not exist in the specified graph.

cy_delete_edge(Graph, EdgeId) :-
    atom(Graph),
    atom(EdgeId),

    recorded(
        cyphail_graph_edges,
        edge(
            Graph,
            EdgeId,
            _,
            _,
            _,
            _
        ),
        Reference
    ),

    erase(Reference).


% ============================================
% NODE DELETION
% ============================================

% Deletes an existing stored node.
%
% A node can only be deleted when it
% has no attached stored relationships.
%
% Graph  - Graph name.
% NodeId - Node identifier.
%
% The operation fails if the node does
% not exist or has connected relationships.

cy_delete_node(Graph, NodeId) :-
    atom(Graph),
    atom(NodeId),

    recorded(
        cyphail_graph_nodes,
        node(
            Graph,
            NodeId,
            _,
            _
        ),
        Reference
    ),

    % Check outgoing relationships.

    \+ recorded(
        cyphail_graph_edges,
        edge(
            Graph,
            _,
            _,
            NodeId,
            _,
            _
        ),
        _
    ),

    % Check incoming relationships.

    \+ recorded(
        cyphail_graph_edges,
        edge(
            Graph,
            _,
            _,
            _,
            NodeId,
            _
        ),
        _
    ),

    erase(Reference).


% ============================================
% DETACH NODE DELETION - SPRINT P2
% ============================================

% Deletes a stored node together with all
% relationships connected to that node.
%
% Graph  - Graph identifier.
% NodeId - Identifier of the node to delete.
%
% The operation:
%
% 1. Verifies that the stored node exists.
% 2. Collects all incoming and outgoing edges.
% 3. Removes duplicate edge identifiers.
% 4. Deletes the collected relationships.
% 5. Deletes the disconnected node.
%
% Duplicate removal is important for self-loops,
% where the source and destination are the same node.
%
% This initial implementation handles nodes and
% relationships stored through Cyphail CREATE.
% Support for original sample nodes is added
% separately.

cy_detach_delete_node(Graph, NodeId) :-
    atom(Graph),
    atom(NodeId),

    % The target stored node must exist.

    cy_get_node(
        Graph,
        NodeId,
        _,
        _
    ),

    % Collect incoming and outgoing relationships.

    findall(
        EdgeId,
        (
            cy_get_edge(
                Graph,
                EdgeId,
                _,
                From,
                To,
                _
            ),

            (
                From == NodeId
                ;
                To == NodeId
            )
        ),
        CollectedEdges
    ),

    % Avoid processing the same relationship twice.

    sort(
        CollectedEdges,
        UniqueEdges
    ),

    % Delete every attached stored relationship.

    maplist(
        cy_delete_edge(Graph),
        UniqueEdges
    ),

    % Delete the node after disconnecting it.

    cy_delete_node(
        Graph,
        NodeId
    ).



% ============================================
% ORIGINAL GRAPH DELETION - SPRINT P2
% ============================================

% Additional clauses supporting the original
% sample graph defined in engine.pl.
%
% The original graph contains facts such as:
%
% user_amigos_node(id1).
% user_amigos_edge(e1, id1, id2).
% user_amigos_label(id1, 'Person').
%
% These predicates also contain rules used
% for retrieving elements created with CREATE.
%
% IMPORTANT:
% Only original facts must be removed.
% Rules supporting CREATE must be preserved.

:- discontiguous cy_delete_edge/2.
:- discontiguous cy_delete_node/2.
:- discontiguous cy_detach_delete_node/2.


% ============================================
% DELETE ORIGINAL RELATIONSHIP
% ============================================

% Deletes a relationship from the original
% professor sample graph.
%
% This clause complements the implementation
% for relationships stored through CREATE.
%
% Only the original relationship facts and
% their associated data are removed.
%
% Rules for stored relationships remain intact.

cy_delete_edge(amigos, EdgeId) :-
    atom(EdgeId),

    % This clause only handles original edges.

    \+ cy_get_edge(
        amigos,
        EdgeId,
        _,
        _,
        _,
        _
    ),

    % Retrieve references to original facts.
    %
    % The clause/3 call uses Body = true
    % to select facts and exclude rules.

    findall(
        Reference,
        clause(
            user_amigos_edge(
                EdgeId,
                _,
                _
            ),
            true,
            Reference
        ),
        EdgeReferences
    ),

    % The original relationship must exist.

    EdgeReferences \= [],

    % Delete original relationship facts.

    maplist(
        erase,
        EdgeReferences
    ),

    % Retrieve the relationship label facts.
    %
    % Do not delete user_amigos_label/2 rules.

    findall(
        Reference,
        clause(
            user_amigos_label(
                EdgeId,
                _
            ),
            true,
            Reference
        ),
        LabelReferences
    ),

    maplist(
        erase,
        LabelReferences
    ),

    % Delete properties associated with
    % this original relationship.

    retractall(
        user_amigos_edge_prop(
            EdgeId,
            _,
            _
        )
    ).


% ============================================
% DELETE ORIGINAL NODE
% ============================================

% Deletes an original graph node only when
% it has no incoming or outgoing edges.
%
% Normal DELETE must not automatically
% disconnect a node.
%
% DETACH DELETE is responsible for deleting
% connected relationships first.

cy_delete_node(amigos, NodeId) :-
    atom(NodeId),

    % This clause only handles original nodes.

    \+ cy_get_node(
        amigos,
        NodeId,
        _,
        _
    ),

    % Find only original node facts.
    %
    % Do not remove the user_amigos_node/1
    % rule used for CREATE nodes.

    findall(
        Reference,
        clause(
            user_amigos_node(NodeId),
            true,
            Reference
        ),
        NodeReferences
    ),

    NodeReferences \= [],

    % Reject deletion when an original
    % outgoing relationship still exists.

    \+ user_amigos_edge(
        _,
        NodeId,
        _
    ),

    % Reject deletion when an original
    % incoming relationship still exists.

    \+ user_amigos_edge(
        _,
        _,
        NodeId
    ),

    % Also protect against recorded edges
    % that reference the same node ID.

    \+ cy_get_edge(
        amigos,
        _,
        _,
        NodeId,
        _,
        _
    ),

    \+ cy_get_edge(
        amigos,
        _,
        _,
        _,
        NodeId,
        _
    ),

    % Remove only original node facts.

    maplist(
        erase,
        NodeReferences
    ),

    % Remove original node label facts.
    %
    % Body = true excludes CREATE rules.

    findall(
        Reference,
        clause(
            user_amigos_label(
                NodeId,
                _
            ),
            true,
            Reference
        ),
        LabelReferences
    ),

    maplist(
        erase,
        LabelReferences
    ),

    % Remove all properties of the node.
    %
    % The property predicate contains
    % dynamic facts, not CREATE rules.

    retractall(
        user_amigos_node_prop(
            NodeId,
            _,
            _
        )
    ).


% ============================================
% DETACH DELETE ORIGINAL NODE
% ============================================

% Deletes an original node together with
% all relationships connected to it.
%
% Handles:
%
% - Outgoing relationships.
% - Incoming relationships.
% - Self-loop relationships.
% - Multiple relationships.
%
% Other nodes must remain unchanged.

cy_detach_delete_node(amigos, NodeId) :-
    atom(NodeId),

    % This clause is for original nodes,
    % not nodes stored through CREATE.

    \+ cy_get_node(
        amigos,
        NodeId,
        _,
        _
    ),

    % Verify that an original node fact
    % exists without matching CREATE rules.

    clause(
        user_amigos_node(NodeId),
        true,
        _
    ),

    % Collect relationships connected
    % to the selected original node.

    findall(
        EdgeId,
        (
            (
                user_amigos_edge(
                    EdgeId,
                    From,
                    To
                )
                ;
                cy_get_edge(
                    amigos,
                    EdgeId,
                    _,
                    From,
                    To,
                    _
                )
            ),

            (
                From == NodeId
                ;
                To == NodeId
            )
        ),
        CollectedEdges
    ),

    % Remove duplicate relationship IDs,
    % including self-loop duplicates.

    sort(
        CollectedEdges,
        UniqueEdges
    ),

    % Delete each attached relationship
    % using the appropriate DELETE clause.

    maplist(
        cy_delete_edge(amigos),
        UniqueEdges
    ),

    % Once disconnected, delete the node
    % using the normal DELETE implementation.

    cy_delete_node(
        amigos,
        NodeId
    ).