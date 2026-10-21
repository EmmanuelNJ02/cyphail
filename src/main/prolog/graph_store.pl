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
 * - Emmanuel Núñez Jiménez
 * - Julissa Solano Valverde
 * - Valery Alfaro Morales
 * - Keynell Molina Mora
 * - Roy Arias Mejia
 *
 * File: graph_store.pl
 *
 * Description:
 * In-memory graph storage for the Cyphail
 * SWI-Prolog execution engine.
 *
 * This file provides operations for creating
 * and retrieving graph nodes, relationships,
 * and their properties.
 *
 * Graph data is stored in the SWI-Prolog
 * recorded database without declaring
 * dynamic predicates.
 */


% ============================================
% PROPERTY VALIDATION
% ============================================

% Verifies that a property contains an
% atom key and a fully instantiated value.

cy_valid_property(Key-Value) :-
    atom(Key),
    ground(Value).


% ============================================
% NODE CREATION
% ============================================

% Creates a node with a generated identifier.
%
% Graph      - Graph name.
% Labels     - List of node labels.
% Properties - List of Key-Value pairs.
% NodeId     - Generated node identifier.

cy_create_node(Graph, Labels, Properties, NodeId) :-
    atom(Graph),
    is_list(Labels),
    Labels \= [],
    maplist(atom, Labels),
    is_list(Properties),
    maplist(cy_valid_property, Properties),

    flag(cyphail_node_counter, Number, Number + 1),

    format(
        atom(NodeId),
        'cy_node_~d',
        [Number]
    ),

    recordz(
        cyphail_graph_nodes,
        node(Graph, NodeId, Labels, Properties),
        _
    ).


% ============================================
% NODE RETRIEVAL
% ============================================

% Retrieves a node from the recorded database.

cy_get_node(Graph, NodeId, Labels, Properties) :-
    recorded(
        cyphail_graph_nodes,
        node(Graph, NodeId, Labels, Properties),
        _
    ).


% ============================================
% NODE PROPERTY RETRIEVAL
% ============================================

% Retrieves a property from a stored node.

cy_get_node_property(Graph, NodeId, Key, Value) :-
    cy_get_node(Graph, NodeId, _, Properties),
    member(Key-Value, Properties).


% ============================================
% RELATIONSHIP CREATION
% ============================================

% Creates a directed relationship between
% two existing nodes in the same graph.
%
% Graph      - Graph name.
% From       - Source node identifier.
% To         - Destination node identifier.
% Type       - Relationship type.
% Properties - List of Key-Value pairs.
% EdgeId     - Generated relationship identifier.

cy_create_edge(Graph, From, To, Type, Properties, EdgeId) :-
    atom(Graph),
    atom(Type),
    Type \= '',

    cy_get_node(Graph, From, _, _),
    cy_get_node(Graph, To, _, _),

    is_list(Properties),
    maplist(cy_valid_property, Properties),

    flag(cyphail_edge_counter, Number, Number + 1),

    format(
        atom(EdgeId),
        'cy_edge_~d',
        [Number]
    ),

    recordz(
        cyphail_graph_edges,
        edge(Graph, EdgeId, Type, From, To, Properties),
        _
    ).


% ============================================
% RELATIONSHIP RETRIEVAL
% ============================================

% Retrieves a stored relationship, including
% its direction, type, and properties.

cy_get_edge(Graph, EdgeId, Type, From, To, Properties) :-
    recorded(
        cyphail_graph_edges,
        edge(Graph, EdgeId, Type, From, To, Properties),
        _
    ).


% ============================================
% RELATIONSHIP PROPERTY RETRIEVAL
% ============================================

% Retrieves a property from a stored
% graph relationship.

cy_get_edge_property(Graph, EdgeId, Key, Value) :-
    cy_get_edge(Graph, EdgeId, _, _, _, Properties),
    member(Key-Value, Properties).


% ============================================
% PROPERTY UPDATE
% ============================================

% Replaces an existing property or adds it
% when the property does not exist.

cy_update_property(Key, Value, Properties, UpdatedProperties) :-
    atom(Key),
    ground(Value),
    (
        select(Key-_, Properties, RemainingProperties)
    ->
        true
    ;
        RemainingProperties = Properties
    ),
    UpdatedProperties = [Key-Value | RemainingProperties].


% ============================================
% NODE PROPERTY UPDATE
% ============================================

% Updates a property of an existing node.
%
% Graph  - Graph name.
% NodeId - Node identifier.
% Key    - Property name.
% Value  - New property value.

cy_set_node_property(Graph, NodeId, Key, Value) :-
    atom(Key),
    ground(Value),

    recorded(
        cyphail_graph_nodes,
        node(Graph, NodeId, Labels, Properties),
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
        node(Graph, NodeId, Labels, UpdatedProperties),
        _
    ),

    erase(Reference).


% ============================================
% RELATIONSHIP PROPERTY UPDATE
% ============================================

% Updates a property of an existing relationship.
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
        edge(Graph, EdgeId, Type, From, To, Properties),
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

% Removes all occurrences of a property
% identified by its key.
%
% Key               - Property name.
% Properties        - Original property list.
% UpdatedProperties - Property list without the key.
%
% Removing a nonexistent property leaves
% the original property list unchanged.

cy_remove_property(Key, Properties, UpdatedProperties) :-
    atom(Key),
    findall(
        ExistingKey-ExistingValue,
        (
            member(ExistingKey-ExistingValue, Properties),
            dif(ExistingKey, Key)
        ),
        UpdatedProperties
    ).


% ============================================
% NODE PROPERTY REMOVAL
% ============================================

% Removes a property from an existing node.
%
% Graph  - Graph name.
% NodeId - Node identifier.
% Key    - Property name to remove.

cy_remove_node_property(Graph, NodeId, Key) :-
    atom(Graph),
    atom(NodeId),
    atom(Key),

    recorded(
        cyphail_graph_nodes,
        node(Graph, NodeId, Labels, Properties),
        Reference
    ),

    cy_remove_property(
        Key,
        Properties,
        UpdatedProperties
    ),

    recordz(
        cyphail_graph_nodes,
        node(Graph, NodeId, Labels, UpdatedProperties),
        _
    ),

    erase(Reference).


% ============================================
% RELATIONSHIP PROPERTY REMOVAL
% ============================================

% Removes a property from an existing
% graph relationship.
%
% Graph  - Graph name.
% EdgeId - Relationship identifier.
% Key    - Property name to remove.

cy_remove_edge_property(Graph, EdgeId, Key) :-
    atom(Graph),
    atom(EdgeId),
    atom(Key),

    recorded(
        cyphail_graph_edges,
        edge(Graph, EdgeId, Type, From, To, Properties),
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
% RELATIONSHIP DELETION
% ============================================

% Deletes an existing relationship from the graph.
%
% Graph  - Graph name.
% EdgeId - Relationship identifier.
%
% The source and destination nodes are preserved.
% The operation fails when the relationship
% does not exist in the specified graph.

cy_delete_edge(Graph, EdgeId) :-
    atom(Graph),
    atom(EdgeId),

    recorded(
        cyphail_graph_edges,
        edge(Graph, EdgeId, _, _, _, _),
        Reference
    ),

    erase(Reference).

% ============================================
% NODE DELETION
% ============================================

% Deletes an existing node from the graph.
%
% Graph  - Graph name.
% NodeId - Node identifier.
%
% A node can only be deleted when it has
% no incoming or outgoing relationships.
%
% The operation fails if the node does not
% exist or still has connected relationships.

cy_delete_node(Graph, NodeId) :-
    atom(Graph),
    atom(NodeId),

    recorded(
        cyphail_graph_nodes,
        node(Graph, NodeId, _, _),
        Reference
    ),

    \+ recorded(
        cyphail_graph_edges,
        edge(Graph, _, _, NodeId, _, _),
        _
    ),

    \+ recorded(
        cyphail_graph_edges,
        edge(Graph, _, _, _, NodeId, _),
        _
    ),

    erase(Reference).