
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
 * In-memory graph storage for Cyphail.
 * Supports CREATE, MATCH, SET, REMOVE,
 * DELETE and DETACH DELETE.
 *
 * Supports both original graph elements
 * and elements created during the session.
 */

% ============================================
% PROPERTY VALIDATION
% ============================================

cy_valid_property(Key-Value) :-
    atom(Key),
    ground(Value).


% ============================================
% NODE CREATION
% ============================================

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
% NODE EXISTENCE - P2 FIX
% ============================================

% Supports stored CREATE nodes and original
% nodes declared as facts in engine.pl.

cy_node_exists(Graph, NodeId) :-
    atom(Graph),
    atom(NodeId),

    once((
        cy_get_node(Graph, NodeId, _, _)
        ;
        (
            Graph == amigos,
            clause(
                user_amigos_node(NodeId),
                true,
                _
            )
        )
    )).


% ============================================
% NODE PROPERTY RETRIEVAL
% ============================================

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
% RELATIONSHIP CREATION - P2 FIX
% ============================================

% Relationships can connect original nodes,
% stored nodes, or one of each.

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

    cy_node_exists(Graph, From),

    cy_node_exists(Graph, To),

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
% SET - STORED NODES
% ============================================

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
% SET - ORIGINAL NODES
% ============================================

cy_set_node_property(amigos, NodeId, Key, Value) :-
    atom(NodeId),
    atom(Key),
    ground(Value),

    \+ cy_get_node(
        amigos,
        NodeId,
        _,
        _
    ),

    user_amigos_node(NodeId),

    retractall(
        user_amigos_node_prop(
            NodeId,
            Key,
            _
        )
    ),

    assertz(
        user_amigos_node_prop(
            NodeId,
            Key,
            Value
        )
    ).


% ============================================
% SET - STORED RELATIONSHIPS
% ============================================

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
% SET - ORIGINAL RELATIONSHIPS - P2 FIX
% ============================================

% Allows SET on e1, e2, e3, e4 and other
% original relationships from engine.pl.

cy_set_edge_property(amigos, EdgeId, Key, Value) :-
    atom(EdgeId),
    atom(Key),
    ground(Value),

    \+ cy_get_edge(
        amigos,
        EdgeId,
        _,
        _,
        _,
        _
    ),

    user_amigos_edge(
        EdgeId,
        _,
        _
    ),

    retractall(
        user_amigos_edge_prop(
            EdgeId,
            Key,
            _
        )
    ),

    assertz(
        user_amigos_edge_prop(
            EdgeId,
            Key,
            Value
        )
    ).


% ============================================
% PROPERTY REMOVAL
% ============================================

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
% REMOVE - STORED NODE PROPERTY
% ============================================

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
% REMOVE - ORIGINAL NODE PROPERTY
% ============================================

cy_remove_node_property(amigos, NodeId, Key) :-
    atom(NodeId),
    atom(Key),

    \+ cy_get_node(
        amigos,
        NodeId,
        _,
        _
    ),

    user_amigos_node(NodeId),

    retractall(
        user_amigos_node_prop(
            NodeId,
            Key,
            _
        )
    ).


% ============================================
% REMOVE - STORED RELATIONSHIP PROPERTY
% ============================================

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
% REMOVE - ORIGINAL RELATIONSHIP PROPERTY
% ============================================

cy_remove_edge_property(amigos, EdgeId, Key) :-
    atom(EdgeId),
    atom(Key),

    \+ cy_get_edge(
        amigos,
        EdgeId,
        _,
        _,
        _,
        _
    ),

    user_amigos_edge(
        EdgeId,
        _,
        _
    ),

    retractall(
        user_amigos_edge_prop(
            EdgeId,
            Key,
            _
        )
    ).


% ============================================
% DELETE - STORED RELATIONSHIP
% ============================================

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
% DELETE - STORED NODE
% ============================================

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

    % Reject deletion with outgoing edges.

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

    % Reject deletion with incoming edges.

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
% DETACH DELETE - STORED NODE
% ============================================

cy_detach_delete_node(Graph, NodeId) :-
    atom(Graph),
    atom(NodeId),

    cy_get_node(
        Graph,
        NodeId,
        _,
        _
    ),

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

    sort(
        CollectedEdges,
        UniqueEdges
    ),

    maplist(
        cy_delete_edge(Graph),
        UniqueEdges
    ),

    cy_delete_node(
        Graph,
        NodeId
    ).


% ============================================
% ORIGINAL GRAPH DELETE DECLARATIONS
% ============================================

:- discontiguous cy_delete_edge/2.
:- discontiguous cy_delete_node/2.
:- discontiguous cy_detach_delete_node/2.


% ============================================
% DELETE - ORIGINAL RELATIONSHIP
% ============================================

cy_delete_edge(amigos, EdgeId) :-
    atom(EdgeId),

    \+ cy_get_edge(
        amigos,
        EdgeId,
        _,
        _,
        _,
        _
    ),

    % Select original facts only, not rules.

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

    EdgeReferences \= [],

    maplist(
        erase,
        EdgeReferences
    ),

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

    retractall(
        user_amigos_edge_prop(
            EdgeId,
            _,
            _
        )
    ).


% ============================================
% DELETE - ORIGINAL NODE
% ============================================

cy_delete_node(amigos, NodeId) :-
    atom(NodeId),

    \+ cy_get_node(
        amigos,
        NodeId,
        _,
        _
    ),

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

    % Verify no original outgoing edges.

    \+ user_amigos_edge(
        _,
        NodeId,
        _
    ),

    % Verify no original incoming edges.

    \+ user_amigos_edge(
        _,
        _,
        NodeId
    ),

    % Verify no stored outgoing edges.

    \+ cy_get_edge(
        amigos,
        _,
        _,
        NodeId,
        _,
        _
    ),

    % Verify no stored incoming edges.

    \+ cy_get_edge(
        amigos,
        _,
        _,
        _,
        NodeId,
        _
    ),

    maplist(
        erase,
        NodeReferences
    ),

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

    retractall(
        user_amigos_node_prop(
            NodeId,
            _,
            _
        )
    ).


% ============================================
% DETACH DELETE - ORIGINAL NODE
% ============================================

cy_detach_delete_node(amigos, NodeId) :-
    atom(NodeId),

    \+ cy_get_node(
        amigos,
        NodeId,
        _,
        _
    ),

    clause(
        user_amigos_node(NodeId),
        true,
        _
    ),

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

    sort(
        CollectedEdges,
        UniqueEdges
    ),

    maplist(
        cy_delete_edge(amigos),
        UniqueEdges
    ),

    cy_delete_node(
        amigos,
        NodeId
    ).
