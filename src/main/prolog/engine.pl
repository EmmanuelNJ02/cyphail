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
 * - Keynell Molina Mora
 * - Roy Arias Mejia
 *
 * File: engine.pl
 *
 * Description:
 * SWI-Prolog graph engine for Cyphail.
 *
 * The engine provides sample graph data,
 * graph access predicates, comparisons,
 * and query lifecycle operations.
 *
 * Important:
 * Do not write messages to standard output
 * while loading the engine through MQI.
 */


% ============================================
% SAMPLE GRAPH DATABASE
% ============================================

:- discontiguous user_amigos_label/2.
:- discontiguous user_amigos_node_prop/3.
:- discontiguous user_amigos_edge_prop/3.

cy_db('amigos').


% ============================================
% GRAPH NODES
% ============================================

user_amigos_node(id1).
user_amigos_node(id2).
user_amigos_node(id3).
user_amigos_node(id4).


% ============================================
% GRAPH LABELS
% ============================================

user_amigos_label(id1, 'Person').
user_amigos_label(id2, 'Person').
user_amigos_label(id3, 'Person').
user_amigos_label(id3, 'Person').


% ============================================
% GRAPH RELATIONSHIPS
% ============================================

user_amigos_edge(e1, id1, id2).
user_amigos_edge(e2, id1, id4).
user_amigos_edge(e3, id2, id1).
user_amigos_edge(e4, id2, id3).


% ============================================
% RELATIONSHIP LABELS
% ============================================

user_amigos_label(e1, 'Amigo').
user_amigos_label(e2, 'Amigo').
user_amigos_label(e3, 'Amigo').
user_amigos_label(e4, 'Amigo').


% ============================================
% NODE PROPERTIES
% ============================================

user_amigos_node_prop(id1, name, 'Juan Perez').
user_amigos_node_prop(id1, age, 30).
user_amigos_node_prop(id1, gender, 'male').

user_amigos_node_prop(id2, name, 'Jorge Soto').
user_amigos_node_prop(id2, age, 43).
user_amigos_node_prop(id2, gender, 'male').

user_amigos_node_prop(id3, age, 25).
user_amigos_node_prop(id3, name, 'Maria Salas').
user_amigos_node_prop(id3, age, 30).
user_amigos_node_prop(id3, gender, 'female').

user_amigos_node_prop(id4, age, 25).
user_amigos_node_prop(id4, name, 'Ana Flores').
user_amigos_node_prop(id4, age, 21).
user_amigos_node_prop(id4, gender, 'female').


% ============================================
% SAMPLE GRAPH QUERY
% ============================================

user_amigos_query(
    NfromName,
    NfromAge,
    ELabel,
    NtoName,
    NtoAge
) :-
    user_amigos_edge(E, Nfrom, Nto),
    user_amigos_label(E, ELabel),
    user_amigos_node_prop(Nfrom, age, NfromAge),
    user_amigos_node_prop(Nto, age, NtoAge),
    NfromAge > NtoAge,
    user_amigos_node_prop(Nto, name, NtoName),
    user_amigos_node_prop(Nfrom, name, NfromName).


% ============================================
% SAMPLE QUERY FORMATTING
% ============================================

query_node_names(Rows) :-
    findall(
        [V],
        user_amigos_node_prop(_, name, V),
        Values
    ),
    maplist(format_prop_value, Values, FormattedValues),
    atomic_list_concat(FormattedValues, '\n', Rows).


format_prop_value(V, A) :-
    format(atom(A), '"~w"', V).


% ============================================
% ORIGINAL DEMO PREDICATES
% ============================================

test_foo(1).

helloworld(H) :-
    member(H, [
        'Hi',
        'Hello'
    ]).

salute :-
    writeln('* Engine running *').


main :-
    forall(
        user_amigos_query(FN, FA, E, TN, TA),
        (
            format(
                atom(A),
                '~w{age:~d} --~w--> ~w{age:~d}',
                [FN, FA, E, TN, TA]
            ),
            writeln(A)
        )
    ).


% ============================================
% GRAPH ACCESS PREDICATES
% ============================================

% Retrieves a relationship with its type,
% source node, and destination node.

user_amigos_edge(EdgeId, Type, From, To) :-
    user_amigos_edge(EdgeId, From, To),
    user_amigos_label(EdgeId, Type).


% Retrieves a property associated with a node.

user_amigos_prop(NodeId, Property, Value) :-
    user_amigos_node_prop(NodeId, Property, Value).


% ============================================
% COMPARISON PREDICATES
% ============================================

% Verifies that two values are different.

cy_neq(A, B) :-
    dif(A, B).


% Verifies that the first numeric value
% is greater than the second.

cy_gt(A, B) :-
    number(A),
    number(B),
    A > B.


% Verifies equality between two values.

cy_eq(A, B) :-
    A = B.


% ============================================
% QUERY LIFECYCLE
% ============================================

% Registers a generated Prolog clause.
%
% QueryId receives a reference to the
% newly registered clause.
%
% The reference identifies the specific
% query and allows its later removal.

cy_create_query(QueryId, Clause) :-
    assertz(Clause, QueryId).


% Executes a registered query.
%
% QueryId identifies the stored clause.
% ColumnNames contains the output headers.
% Rows contains the formatted result table.

cy_exec_query(QueryId, ColumnNames, Rows) :-
    clause(Head, Body, QueryId),

    Head =.. [_Predicate | Values],

    findall(
        Line,
        (
            call(Body),
            maplist(cy_format_value, Values, FormattedValues),
            atomic_list_concat(FormattedValues, ' ', Line)
        ),
        ResultLines
    ),

    atomic_list_concat(ColumnNames, ' ', Header),

    atomic_list_concat(
        [Header | ResultLines],
        '\n',
        Rows
    ).


% Removes the registered query clause
% using its unique reference.

cy_erase_query(QueryId) :-
    erase(QueryId).


% ============================================
% RESULT VALUE FORMATTING
% ============================================

% Converts a Prolog value to an atom
% suitable for tabular output.

cy_format_value(Value, Formatted) :-
    format(atom(Formatted), '~q', [Value]).