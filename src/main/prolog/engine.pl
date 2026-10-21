/*
 * Cyphail - Sprint P2
 * SWI-Prolog Graph Engine
 *
 * Initial implementation of the graph storage layer.
 * The engine will be extended with query execution,
 * updates and CodeGen integration.
 *
 * Important:
 * Do not print messages to standard output during
 * MQI execution.
 */


%%%%%%%%% Sample Graph data base %%%%%%%%%%%%%%%%%%%%%

:- discontiguous user_amigos_label/2.
:- discontiguous user_amigos_node_prop/3.
:- discontiguous user_amigos_edge_prop/3.

cy_db('amigos').

user_amigos_node(id1).
user_amigos_node(id2).
user_amigos_node(id3).
user_amigos_node(id4).

user_amigos_label(id1, 'Person').
user_amigos_label(id2, 'Person').
user_amigos_label(id3, 'Person').
user_amigos_label(id3, 'Person').

user_amigos_edge(e1, id1, id2).
user_amigos_edge(e2, id1, id4).
user_amigos_edge(e3, id2, id1).
user_amigos_edge(e4, id2, id3).

user_amigos_label(e1, 'Amigo').
user_amigos_label(e2, 'Amigo').
user_amigos_label(e3, 'Amigo').
user_amigos_label(e4, 'Amigo').

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

%%%%%%%%%%%%%%%%%%%    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

user_amigos_query(NfromName, NfromAge, ELabel, NtoName, NtoAge) :-
    user_amigos_edge(E, Nfrom, Nto),
    user_amigos_label(E, ELabel),
    user_amigos_node_prop(Nfrom, age, NfromAge),
    user_amigos_node_prop(Nto, age, NtoAge),
    NfromAge > NtoAge,
    user_amigos_node_prop(Nto, name, NtoName),
    user_amigos_node_prop(Nfrom, name, NfromName)
.

query_node_names(Rows) :-
    findall([V], user_amigos_node_prop(_, name, V), LV),
    maplist(format_prop_value, LV, LFV),
    atomic_list_concat(LFV, '\n', Rows)
.

format_prop_value(V, A) :-
    format(atom(A), '"~w"', V)
.

test_foo(1).

helloworld(H) :-
    member(H, [
        'Hi',
        'Hello'
    ])
.

salute :-
    writeln('* Engine running *')
.

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

main :-
    forall(
        user_amigos_query(FN, FA, E, TN, TA),
        (format(atom(A), '~w{age:~d} --~w--> ~w{age:~d}',
                        [FN, FA, E, TN, TA]),
         writeln(A))
    )
.

user_amigos_edge(EdgeId, Type, From, To) :-
    user_amigos_edge(EdgeId, From, To),
    user_amigos_label(EdgeId, Type).

user_amigos_prop(NodeId, Property, Value) :-
    user_amigos_node_prop(NodeId, Property, Value).


% Predicados de comparacion para Cyphail P2.
% Se utilizan en los goals generados por CodeGen.

% Verifica que dos valores sean diferentes.
cy_neq(A, B) :-
    dif(A, B).

% Verifica que un numero sea mayor que otro.
cy_gt(A, B) :-
    number(A),
    number(B),
    A > B.

% Verifica la igualdad de dos valores.
cy_eq(A, B) :-
    A = B.