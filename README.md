# Cyphail — Sprint P2

Cyphail is an academic prototype of a graph query language developed for **EIF400 — Programming Paradigms** at the **School of Informatics, National University of Costa Rica (UNA)**.

Sprint P2 extends the Sprint P1 lexer, functional parser combinators, abstract syntax tree (AST), semantic analyzer, and command-line interface. Its main change is the replacement of simulated query execution with a **real SWI-Prolog engine accessed through the Machine Query Interface (MQI)**.

This README documents the Sprint P2 implementation for the **November 2026** project phase: architecture, setup, execution, tests, verified behavior, and remaining steps before submission.

**Latest verified results — November 8, 2026:** `mvn test` passed **438 standard tests**; `mvn "-Dtest=*IT" test` separately passed **65 real SWI-Prolog/MQI integration tests**. Both runs reported **0 failures, 0 errors, and 0 skipped tests: 503 passing test cases across two separate executions**. A fresh `mvn clean package` also passed **438 standard tests**, compiled **88 production** and **69 test** Java source files with `release 21`, and generated the executable shaded JAR `target\cyphail-0.1.0.jar`. The JAR was launched successfully from CMD and ran real Cyphail queries through the MQI engine. These results describe the locally verified, **uncommitted** worktree, not the final bundle or submission.

---

## Project Information

| Field | Details |
|---|---|
| Project | Cyphail |
| Sprint | P2 |
| Application version | 0.1.0 |
| Course | EIF400 — Programming Paradigms |
| University | National University of Costa Rica (Universidad Nacional, UNA) |
| School | School of Informatics (Escuela de Informática) |
| Group | 04-10am |
| Class time | 10:00 a.m. |
| Main language | Java (JDK 26.0.1 used for the local checks; Maven compiler release set to 21) |
| Query execution engine | SWI-Prolog through MQI |
| Academic term | Second semester, 2026 |
| Project presentation month | November 2026 |
| Last documented verification | November 8, 2026 (438 standard + 65 real MQI tests; fresh clean package; real JAR/REPL demo) |

## Team Members

- Emmanuel Núñez Jiménez
- Valery Alfaro Morales
- Roy Arias Mejía
- Keynell Molina Mora
- Julissa Solano Valverde

---

## Sprint P2 Scope

Sprint P2 preserves the applicable Sprint P1 functionality and adds the following capabilities:

- Multiline Cyphail statements. A semicolon (`;`) executes a complete statement immediately; a complete statement without `;` can be dispatched by entering a blank line or by reaching end-of-input. Immediate execution after a single non-terminated Enter has **not** been established.
- The `.load "path/to/file.cyphail"` command, which reads and executes statements from disk.
- Relationship patterns, including incoming (`IN`), outgoing (`OUT`), and undirected (`NONE`) AST directions.
- Node and relationship labels, variables, and properties.
- `MATCH` and `WHERE` queries, including comparisons and logical conditions supported by the parser and code generator.
- `CREATE` statements with or without a preceding `MATCH`.
- `SET` for updating node or relationship properties.
- `REMOVE` for removing properties.
- `DELETE` for deleting nodes or relationships.
- `DETACH DELETE` for deleting nodes together with their incident relationships.
- AST-to-Prolog code generation and mapping between Cyphail and generated variables.
- A broker that launches SWI-Prolog, opens an MQI socket connection, and reuses that connection during the REPL session.
- MQI JSON response parsing using Gson and tabular result display.
- Additional static semantic checks for relationship variables, reserved variable names, and repeated labels **inside one node pattern**.
- `NOT` in supported `WHERE` expressions and related parsing/code-generation paths, as confirmed by the November regression suites.
- Duplicate physical-edge prevention when multiple relationship occurrences appear in a `MATCH`.

The **Sprint P1 Fake Query Engine is no longer part of the query execution path**. The previous simulated query-result JSON files have also been removed.

The graph catalog used by `.use` is a separate legacy feature. It is not the engine used to execute Cyphail queries.

---

## Prerequisites

The following tools must be installed and accessible from the command line:

- **Java Development Kit**: the verified Windows setup uses **JDK 26.0.1**, while the Maven compiler is configured with `release 21` (`maven.compiler.release=21`). This permits Java 21-compatible class files; an actual JDK 21 runtime was **not** separately tested and is not required to repeat the documented checks on the existing verified setup.
- **Apache Maven**.
- **SWI-Prolog** with MQI support and `swipl` on the system `PATH`.
- **Git** for version control and the final bundle.
- **Windows Command Prompt (CMD)** for the commands documented below.

Verify the installation:

```cmd
java -version
javac -version
mvn -version
swipl --version
git --version
```

The team has used SWI-Prolog 10.0.2 on Windows during development. SWI-Prolog must be available when running real MQI tests or starting the REPL. The README is stored as UTF-8; on some Windows CMD configurations, `type README.md` may display Unicode accents or the em dash incorrectly even when the file itself is valid. A UTF-8-aware editor should display the text correctly.

The engine source file is currently located at:

```text
src/main/prolog/engine.pl
```

---

## Project Structure

The relevant directories and files are organized approximately as follows:

```text
cyphail/
|-- README.md
|-- pom.xml
|-- cyphail.bat
|-- .gitignore
|-- demo-tests/               (local demo cases; inspect and track before delivery)
|-- docs/
|-- data/
|   `-- graphs.json
|-- src/
|   |-- main/
|   |   |-- java/
|   |   |   `-- cr/ac/una/eif400/cyphail/
|   |   |       |-- ast/
|   |   |       |-- compiler/
|   |   |       |   |-- analyzer/
|   |   |       |   |-- codegen/
|   |   |       |   |-- lexer/
|   |   |       |   `-- parser/
|   |   |       |       `-- combinator/
|   |   |       |-- data/
|   |   |       |-- engine/
|   |   |       |   `-- mqi/
|   |   |       |-- frontend/
|   |   |       |   `-- handler/
|   |   |       |-- model/
|   |   |       `-- starter/
|   |   `-- prolog/
|   |       |-- engine.pl
|   |       `-- graph_store.pl
|   `-- test/
|       `-- java/
`-- target/                   (generated by Maven)
```

The `target/` directory contains generated build and test artifacts and is ignored by Git. The legacy `data/graphs.json` file is used by the graph catalog and must not be confused with the removed P1 fake query-response files. `graph_store.pl` implements mutable graph storage used by `engine.pl`; both Prolog files are required by the current execution design.

---

## Architecture Overview

Cyphail P2 follows this execution pipeline:

```text
Interactive REPL / .load script
             |
             v
        CommandRouter
             |
             v
      PrologQueryHandler
             |
             v
  Lexer + Functional Parser Combinators
             |
             v
         Cyphail AST
             |
             v
       SemanticAnalyzer
             |
             v
     PrologCodeGenerator
             |
             v
        Prolog goals
             |
             v
          MqiBroker
             |
             v
           MqiClient
             |
             v
        Local MQI socket
             |
             v
     SWI-Prolog / engine.pl
             |
             v
        MQI JSON response
             |
             v
   MqiResponseParser + Gson
             |
             v
       PrologResult / Rows
             |
             v
      Tabular REPL output
```

### REPL and Routing

The REPL receives interactive commands and Cyphail statements. A statement may span multiple lines; it is dispatched when `;` terminates it, or when a blank line/end-of-input completes a syntactically valid buffered statement. `CommandRouter` routes a completed statement to its handler. A statement without `;` is **not** currently documented as executing after only one Enter; this distinction matters in the instructor's optional-semicolon demonstration.

### Lexer, Parser, and AST

The lexer and functional parser combinators recognize Cyphail syntax. The parser constructs an AST that represents patterns, expressions, projections, and update clauses. The `.tree` command displays this structure without executing the query against Prolog.

### Semantic Analysis

The semantic analyzer validates variable declarations, uses, and role restrictions before code generation. Node variables may be reused as joins, but relationship variables cannot be redeclared within the same query or reused as node variables.

On November 8, a regression was corrected: a node pattern containing the same label twice, for example `MATCH (e:Company:Company) RETURN e;`, is rejected with `Semantic error: Node pattern contains duplicate labels in MATCH pattern.` A dedicated JUnit test covers this rule **within one node pattern**. For the *same node variable reused across separate patterns with different labels*, the team's selected policy is to **merge label constraints (logical AND)**: the variable always denotes one physical node that must satisfy every label. Negative and positive REPL tests, as well as automated tests, were verified.

### Code Generation

`PrologCodeGenerator` translates the AST into Prolog goals that are understood by the engine. It also manages the association between Cyphail variables, generated Prolog variables, computed values, and result columns.

The query lifecycle uses Prolog's `setup_call_cleanup/3` so that temporary generated query clauses are erased even when execution raises an exception. The cleanup behavior was tested directly in SWI-Prolog and protected by code-generation unit tests.

### October 29 CodeGen Regression Fixes

Two compatibility and correctness fixes were tested in the current local revision:

1. **Distinct physical relationships:** when a `MATCH` contains multiple relationship occurrences, CodeGen emits pairwise `cy_neq/2` checks between their physical edge IDs. This applies to named, anonymous, and chained relationship patterns; repeated node variables remain valid joins. The behavior is covered by six `PrologCodeGeneratorDistinctEdgesTest` unit tests and three `PrologDistinctEdgesMqiLiveIT` real-MQI tests.
2. **Property references in MATCH patterns:** a pattern property value such as `personId: p.id` may reference a property of an earlier `MATCH` variable. CodeGen now resolves the reference through `visitPropertyExpression` while preserving literal handling. This is covered by four `PrologCodeGeneratorMatchPropertyReferenceTest` unit tests and one `PrologMatchPropertyReferenceMqiLiveIT` real-MQI test.

These fixes passed both the standard and real-MQI suites. The modified production files were reviewed with `git diff` on October 29, 2026. Testing does not mean that the local changes have already been committed or merged.

### MQI Broker and Engine

`MqiBroker` launches SWI-Prolog and establishes communication over a socket using MQI. The REPL reuses the broker during the session and closes it when the session ends. The engine executes the generated goals and returns JSON-formatted MQI responses.

### Result Processing

`MqiResponseParser` processes MQI responses using Gson. The query handler extracts the relevant values and displays results as tables.

---

## Building the Project

Open Windows CMD in the repository root (the folder containing `pom.xml` and `src`). For example, if the project is inside your Documents folder:

```cmd
cd "%USERPROFILE%\Documents\cyphail"
```

If your clone is stored elsewhere, replace that path with its actual location.

Run a normal build:

```cmd
mvn package
```

Run a clean build when the `target/` directory is not locked by another process:

```cmd
mvn clean package
```

The Maven Shade plugin is configured to produce an executable JAR with dependencies included:

```text
target\cyphail-0.1.0.jar
```

A successful Maven build ends with:

```text
BUILD SUCCESS
```

**Historical baseline (October 29, 2026):** `mvn clean package` compiled 86 production and 67 test Java files, passed 419 standard tests, and generated the shaded JAR; a separate `*LiveIT` run passed 62 MQI tests. These numbers describe that **earlier revision only**.

**Latest verification (November 8, 2026), performed in Windows CMD:**

- `mvn test` — **438** standard tests passed; 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS`.
- `mvn "-Dtest=*IT" test` — **65** live SWI-Prolog/MQI tests passed; 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS`.
- `mvn clean package` — removed the previous `target/` directory, compiled **88 production Java files** and **69 test files** with `[debug release 21]`, passed **438** standard tests and generated `target\cyphail-0.1.0.jar` using Maven Shade; `BUILD SUCCESS`.
- `java -jar target\cyphail-0.1.0.jar --help` — displayed the `repl` CLI command.
- `java -jar target\cyphail-0.1.0.jar repl` — started the interactive REPL, executed `MATCH (p:Person) RETURN p.name, p.age;` and the same query without `;` followed by a blank line, and returned real graph rows.
- `.load "prueba_load_p2.cyphail"` — successfully dispatched **5 statements** (`MATCH`, `CREATE`, `MATCH`, `SET`, `MATCH`), with the test node's age changing from 22 to 40.
- R0/C0–C12, supplemental 8b/8c, and semantic/physical-edge uniqueness cases were manually exercised in the local REPL; these are **development verification results**, not a professor-graded review or a finalized repeatable fixture.

The Shade plugin printed warnings for overlapping JAR metadata (`module-info` and `MANIFEST.MF`); **the build succeeded**. The current JAR still expects external Prolog files from the project tree. The local, verified changes have **not yet been committed, pushed, bundled or released**; repeat the appropriate tests if code changes before submission.
---

## Running Cyphail

With the executable JAR available, start the REPL from the **project root**:

```cmd
java -jar target\cyphail-0.1.0.jar repl
```

The Windows launcher may also be used:

```cmd
cyphail.bat repl
```

The launcher verifies Java and the built JAR, temporarily changes to the repository directory, passes arguments to Java, then restores the original working directory and returns the Java process exit code.

For example, from the parent folder:

```cmd
cyphail\cyphail.bat repl
```

**Manually verified on October 27, 2026:** the launcher started the REPL from the repository root **and** from its parent folder; a real `CREATE` followed by a `MATCH` returned the same node ID within the remote-folder launch session.

The startup text included:

```text
Welcome to Cyphail v0.1.0 - Sprint P2
November 2026
```

General CLI help:

```cmd
java -jar target\cyphail-0.1.0.jar --help
```

Application version:

```cmd
java -jar target\cyphail-0.1.0.jar --version
```

### Engine Path Requirement

The current REPL startup resolves the Prolog engine from this project-relative path:

```text
src/main/prolog/engine.pl
```

The engine also depends on the accompanying `src/main/prolog/graph_store.pl`. Keep both files in the expected project layout.

Therefore, invoke `java -jar` from the repository root, or use `cyphail.bat` to run from another working directory.

The launcher selects the repository root automatically; it still needs the `src/main/prolog/engine.pl` file in the project tree.

**Running the standalone JAR without this engine file or from an unrelated directory has not been verified** and remains part of the packaging audit.

---

## REPL Commands

| Command | Purpose |
|---|---|
| `.help` | Display REPL help. |
| `.about` | Display project and team information. |
| `.use` | List graphs from the legacy catalog. |
| `.use <graph>` | Check the requested graph against the catalog. |
| `.tree` | Enter multiline AST inspection mode. |
| `.tree <query>` | Parse and display a query AST. |
| `.load "path/to/script.cyphail"` | Read and execute Cyphail statements from a file. |
| `.exit` | Exit the REPL and close the MQI session. |

**Semicolon behavior verified from the JAR:** a query ending in `;` executes immediately. Without a semicolon, a syntactically complete statement can be submitted by entering a **blank line**; a buffered complete statement can also be dispatched at EOF. This was tested manually and in `ReplMultilineIntegrationTest`. **Limitation to clarify before the instructor demo:** a single Enter after a complete, unterminated query currently leaves the continuation prompt (`...`) until a blank line; whether the instructor expects immediate execution with one Enter must be confirmed and demonstrated honestly.

The `.tree` commands inspect queries; they do not mutate the Prolog graph.

**Command review on October 27, 2026:** the current `HelpCommandHandler.java` source lists `.load` and the other commands above, while `.about` was executed manually and displayed the P2 information, November 2026 date, and all five team members.

A direct interactive `.help` output check can be included in the final smoke test. The packaged JAR CLI `--help` was verified on October 29, 2026; this is distinct from the interactive REPL `.help` command.

### `.use` and the Legacy Graph Catalog

The `.use` command reads `data/graphs.json`.

In the current implementation, `.use <graph>` reports whether the graph exists in that catalog; it **does not switch the active graph inside the Prolog engine**.

Some inherited messages may contain fixed P1 timing values and must not be interpreted as real MQI performance measurements.

---

## Multiline Statements

A regular Cyphail query can span several lines:

```cypher
MATCH (p:Person)
WHERE p.age > 30
RETURN p.name, p.age;
```

The REPL accumulates a statement until its `;`, or until a syntactically complete buffer is followed by a blank line or EOF. To execute a statement without a semicolon in an interactive session, **press Enter once on the statement and once more on an empty line**. Do not confuse this with immediate execution on the first Enter: that behavior has not been verified.

The completed query is then parsed, analyzed, generated, and sent to the real engine.

---

## Running Scripts with `.load`

Create a UTF-8 file named `example_p2.cyphail` in the project root:

```cypher
CREATE (p:ExampleLoadP2 {name: 'FromFile'})
RETURN p;

MATCH (p:ExampleLoadP2)
RETURN p, p.name;
```

Start the REPL and enter:

```text
.load "example_p2.cyphail"
```

The file handler reads and dispatches complete statements **in their original order**, using the same routing and MQI session as interactive queries. A local `prueba_load_p2.cyphail` example was executed successfully with five dispatched statements, creating a `PruebaLoadP2` node, changing `age` from 22 to 40, and reading the updated value. This file is currently **untracked**; decide which demo files to include in Git before packaging.

A later statement may therefore read data created by an earlier one.

The command reports missing files and incomplete statements instead of silently ignoring them. The path must be enclosed in quotation marks.

---

## Verified Manual REPL Smoke Tests — October 27, 2026

The following was executed using `cyphail\cyphail.bat repl` while the Windows CMD working directory was the **parent** of the repository.

This verified launcher-relative path handling, actual MQI execution, and graph state reuse within a single REPL session:

```cypher
CREATE (p:LauncherP2 {name: 'ExternalFolderTest'}) RETURN p;

MATCH (p:LauncherP2 {name: 'ExternalFolderTest'})
RETURN p, p.name;
```

Observed REPL output:

```text
p
cy_node_0

p p.name
cy_node_0 'ExternalFolderTest'
```

The node variable `p` returned only its unique graph ID, as required by the Sprint P2 `RETURN` restriction.

The same ID appeared in both statements.

This test does **not** establish persistent storage across separate REPL sessions.

The `.about` and `.exit` commands were also exercised manually; `.about` displayed the P2 details and five authors, and `.exit` returned cleanly to CMD.

---

## Verified Live REPL Results — November 8, 2026

The following table was returned by the JAR from the instructor-provided example graph:

```text
>>> MATCH (p:Person) RETURN p, p.name, p.age;
p p.name p.age
id1 'Juan Perez' 30
id2 'Jorge Soto' 43
id3 'Maria Salas' 25
id3 'Maria Salas' 30
```

Two rows for `id3` correspond to two `age` facts in the Prolog example graph. This is **not** evidence of a fourth `Person` node. The demo `.load` script also returned an updated test-node age of 40 after a `SET`. All these values depend on the current engine data and session.

---

## Sprint P2 Query Examples

The examples below demonstrate supported language constructs. The verified instructor review also includes **C11 (multiple SET assignments)** and **C12 (multiple REMOVE operations)**, tested manually against nodes and relationships on November 8. A separate `demo-tests/` directory exists locally but is not yet tracked in Git; inspect its contents and ensure the final demonstration scripts are versioned.

Results depend on the contents of the Prolog graph in the current session.

The included example graph provides some `Person` and `Amigo` records, while the other example labels are created by the illustrated statements. Results may vary if the graph has been changed.

### 1. Match Nodes and Properties

```cypher
MATCH (p:Person)
RETURN p.name, p.age;
```

### 2. Filter with WHERE

```cypher
MATCH (p:Person)
WHERE p.age > 30
RETURN p.name, p.age;
```

### 3. Match Directed Relationships

```cypher
MATCH (a:Person)-[r:Amigo]->(b:Person)
RETURN r, a.name, b.name;
```

### 4. Reuse a Node Variable for a Join

```cypher
MATCH (a:Person)-[r:Amigo]->(b:Person),
      (b)-[s:Amigo]->(c:Person)
RETURN a.name, b.name, c.name;
```

Here, `b` refers to the same node in both patterns.

Reusing a node variable in this way is valid.

### 5. Create a Node without MATCH

```cypher
CREATE (p:ExampleP2 {name: 'Node_P2', age: 27})
RETURN p;
```

### 6. Read the Created Node

```cypher
MATCH (p:ExampleP2 {name: 'Node_P2'})
RETURN p, p.name, p.age;
```

### 7. Update a Node Property

```cypher
MATCH (p:ExampleP2 {name: 'Node_P2'})
SET p.age = 35
RETURN p, p.age;
```

### 8. Remove a Node Property

```cypher
MATCH (p:ExampleP2 {name: 'Node_P2'})
REMOVE p.age
RETURN p;
```

### 9. Create a Relationship with Properties

```cypher
CREATE (a:ExampleRelationP2 {name: 'Source'})
       -[r:LinkP2 {since: 2026, grade: 8.5}]->
       (b:ExampleRelationP2 {name: 'Target'})
RETURN a, r, b;
```

### 10. Read Relationship Properties

```cypher
MATCH (a:ExampleRelationP2)-[r:LinkP2]->(b:ExampleRelationP2)
RETURN r, r.since, r.grade;
```

### 11. Update a Relationship Property

```cypher
MATCH (a:ExampleRelationP2)-[r:LinkP2]->(b:ExampleRelationP2)
SET r.grade = 9.5
RETURN r, r.grade;
```

### 12. Remove a Relationship Property

```cypher
MATCH (a:ExampleRelationP2)-[r:LinkP2]->(b:ExampleRelationP2)
REMOVE r.grade
RETURN r, r.since;
```

### 13. Delete a Relationship

```cypher
MATCH (a:ExampleRelationP2)-[r:LinkP2]->(b:ExampleRelationP2)
DELETE r;
```

### 14. Detach and Delete a Node

```cypher
MATCH (a:ExampleRelationP2 {name: 'Source'})
DETACH DELETE a;
```

### 15. Match Using a Previously Declared Property

The following example demonstrates a Sprint P1 property-reference parsing case now supported by the P2 CodeGen and engine. Run these statements in the same REPL session:

```cypher
CREATE (p:Person {id: 202610298761, name: 'MatchReferenceP2'})
RETURN p;

CREATE (o:Order {personId: 202610298761, total: 150})
RETURN o;

MATCH (p:Person {id: 202610298761}),
      (o:Order {personId: p.id})
RETURN p.name, o.total;
```

The corresponding real-MQI integration test also creates an unrelated order with `personId: 202610298762` and verifies that it is not returned. Prolog's tabular representation may quote string atoms, such as `'MatchReferenceP2'`.

These statements are intended to be executed against the real MQI-connected engine.

Their results are not read from prerecorded JSON response files.

---

## RETURN Semantics

Under the Sprint P2 specification, when a `RETURN` item is a pattern variable, the result contains **only the unique ID** of the corresponding node or relationship.

For a node:

```cypher
MATCH (p:Person)
RETURN p;
```

For a relationship:

```cypher
MATCH (a:Person)-[r:Amigo]->(b:Person)
RETURN r;
```

To return individual attributes, use property expressions:

```cypher
MATCH (p:Person)
RETURN p.name, p.age;
```

This ID-only rule is part of the prototype scope and does not attempt to return full Cypher-style node or relationship objects.

---

## Static Semantic Validation

Sprint P2 extends the semantic analyzer with additional restrictions.

### 1. A Relationship Variable Cannot Be Repeated

The following query must be rejected because `r` is declared in two distinct relationship patterns:

```cypher
MATCH (a:Person)-[r:Amigo]->(b:Person),
      (c:Person)-[r:Amigo]->(d:Person)
RETURN a;
```

### 2. A Variable Cannot Be Both a Node and a Relationship

This query must be rejected because `a` is used in both roles:

```cypher
MATCH (a:Person)-[a:Amigo]->(b:Person)
RETURN a;
```

### 3. Cyphail Variable Names Cannot Start with `__`

This query must be rejected:

```cypher
MATCH (__person:Person)
RETURN __person;
```

The `__` prefix is reserved to prevent name collisions when code generation creates Prolog variables and internal identifiers.

### 4. Reusing a Node Variable Is Allowed

This query is valid because `b` joins two patterns:

```cypher
MATCH (a:Person)-[r:Amigo]->(b:Person),
      (b)-[s:Amigo]->(c:Person)
RETURN a.name, b.name, c.name;
```

### 5. References to Undefined Variables

The project preserves the earlier semantic checks for variables used before their declaration or used without any valid declaration.

### 6. Duplicate Labels in One Node Pattern Are Rejected

The following query is invalid:

```cypher
MATCH (e:Company:Company)
RETURN e;
```

Observed on November 8, 2026:

```text
Semantic error: Node pattern contains duplicate labels in MATCH pattern.
```

The new `SemanticAnalyzerTest.shouldRejectDuplicateLabelsInMatchPattern` test also passes. The distinct-labels example `MATCH (x:Person:Employee)` is **not** a duplicate and remains permitted by this check.

### 7. A Node Variable Reused with Different Labels: Merge Constraints (AND)

The selected policy is **conjunction**, not a semantic error: a repeated node variable refers to the **same node**, which must have all labels specified in the separate patterns.

**Negative example** (both labels exist on other nodes, but no node has both):

```cypher
MATCH (e:DemoC4), (e:DemoC8)
RETURN e;
```

The REPL returned only the `e` header, with no rows.

**Positive example** (one node has both labels):

```cypher
CREATE (e:DemoC4:DemoC8 {name: 'NodoDobleEtiquetaP2'}) RETURN e;

MATCH (e:DemoC4), (e:DemoC8)
WHERE e.name = 'NodoDobleEtiquetaP2'
RETURN e;
```

The returned ID matched the ID created (`cy_node_8` in that **single test session**). Different sessions may allocate different IDs. Three added live-MQI tests in `PrologCodeGeneratorMqiLiveIT` cover reuse with the same label, incompatible labels, and a node satisfying two different labels. **A duplicated label within one node pattern remains invalid**.

### 8. Distinct Physical Relationships in Multi-Pattern MATCH

Code generation adds pairwise `cy_neq/2` restrictions so two different relationship occurrences cannot consume the **same physical edge**, even with distinct variable names. This is separate from the static check prohibiting reuse of one relationship variable name. A negative manual test pairing `PRUEBA_C8` with itself returned no rows; a positive test combining two distinct `TRABAJA_C10` relations returned `cy_edge_3 cy_edge_6` and `cy_edge_6 cy_edge_3` without any self-pairs. The supplemental 8c test also verified a Cartesian product of disconnected patterns (2 × 1 = 2 rows). These results refer to one local REPL session.

Static semantic errors are identified before the generated query is sent to SWI-Prolog.

---

## Sprint P1 Compatibility

The following Sprint P1 components remain relevant to P2:

- Command-line application and modular command routing.
- Lexer and functional parser combinators.
- Cyphail AST and visitors.
- Semantic analyzer.
- `.tree` query inspection.
- Existing REPL commands and graph catalog.
- Applicable unit and integration tests.

Representative P1 parsing cases include:

### Pattern and Projection

```cypher
MATCH (m:Movie)
RETURN m.title, m.year AS year;
```

### WHERE and Projection

```cypher
MATCH (b:Book)
WHERE b.pages < 300
RETURN b.title, b.pages AS totalPages;
```

### Multiple Labels and Properties

```cypher
MATCH (a:Person:Employee {id: 1})
WHERE a.age > 30
RETURN a.name AS name;
```

### Disconnected Patterns

```cypher
MATCH (m:Movie), (p:Person)
WHERE m.year > 2000
RETURN m.title, p.name;
```

### Attribute Comparisons

```cypher
MATCH (m:Movie), (p:Person)
WHERE m.year <> p.age
RETURN m.title, p.name;
```

These examples document language parsing cases, not guaranteed preloaded database contents.

---

## Removal of the P1 Fake Query Engine

In P1, predefined query results were returned from JSON files on disk.

P2 replaces that approach with actual SWI-Prolog execution through MQI.

The following obsolete production classes have been removed:

```text
FakeQueryEngine.java
FakeQueryHandler.java
FakeQueryResult.java
FakeQueryJsonRepository.java
```

The corresponding fake-engine test and the four prerecorded JSON files under `data/queries/` have also been removed.

The current execution path uses `PrologQueryHandler`, code generation, the MQI broker and client, and the Prolog engine.

`data/graphs.json` is intentionally retained for the independent legacy `.use` catalog.

---

## Automated Tests

The project uses **JUnit 5** and **Maven Surefire**.

The standard suite and the classes matching `*IT` (including `*LiveIT`) use separate Maven test selectors. The November 8 live-integration run used `-Dtest=*IT`. The instructor's complete functional demo is separate from these automated suites.

The test totals below refer to their **separate executions**; they are not one combined Maven run.

### General Test Suite

```cmd
mvn test
```

This suite covers the lexer, AST, visitors, parser combinators, parser, semantic analyzer, code generation, frontend, REPL, command routing, and other applicable units and integrations.

### Live MQI Integration Tests

The live-MQI test classes are not automatically included in a normal `mvn test` run.

Execute them explicitly:

```cmd
mvn "-Dtest=*IT" test
```

The earlier `mvn "-Dtest=*LiveIT" test` selector appears in historical October runs. The current verified selector is `-Dtest=*IT`, exercising **65** real MQI integration cases on November 8.

These tests cover real MQI connections, the Prolog engine, CodeGen, and supported graph operations.

### Frontend Integration Tests

The following command runs the main frontend integration test classes:

```cmd
mvn "-Dtest=CommandRouterIntegrationTest,ReplIntegrationTest,ReplLoadIntegrationTest,ReplMultilineIntegrationTest,PrologQueryHandlerIntegrationTest" test
```

### Latest Verified Results — November 8, 2026

| Command / suite | Tests | Failures | Errors | Skipped | Outcome |
|---|---:|---:|---:|---:|---|
| `mvn test` (standard suite) | **438** | 0 | 0 | 0 | BUILD SUCCESS |
| `mvn "-Dtest=*IT" test` (real MQI suite) | **65** | 0 | 0 | 0 | BUILD SUCCESS |
| `mvn clean package` (clean build + standard suite) | **438** | 0 | 0 | 0 | BUILD SUCCESS; shaded JAR created |

**Combined count: 503 passing test cases across two separate suites** (`438 + 65`). The 438 cases executed again during `mvn clean package` are a **repeat**, not an additional 438 tests. The test groups are not one Maven invocation.

The main suite includes 12 `ReplMultilineIntegrationTest` and 8 `SemanticAnalyzerTest` cases. The live integration suite includes 12 `PrologCodeGeneratorMqiLiveIT` cases, among its 65 total cases.

These runs verify the **current local source revision**, but they do **not** establish that it has been committed, pushed or included in a distribution bundle. If any Java or Prolog source changes before delivery, rerun the appropriate suites and a clean build.
---

## Troubleshooting Maven Cleanup on Windows

During development, Windows prevented Maven from deleting files inside `target/test-classes/` during the `clean` phase:

```text
Failed to delete ... target/test-classes/...
```

This means the clean goal failed before a clean compilation and test run could finish.

It does **not** mean that the earlier successful test suites contained failing tests.

Check whether an IDE, terminal, Java process, or another program is holding files in `target/` open.

Do not terminate unrelated processes or delete system files indiscriminately.

The normal suite can still be executed using:

```cmd
mvn test
```

`mvn clean package` **did succeed on November 8, 2026**, after removing the previous `target/`. If a future clean fails due to locked files, investigate that new incident rather than treating the historical problem as current.

---

## Current Scope and Outstanding Verification

Cyphail P2 is an academic prototype, not a complete production implementation of Cypher or a production graph database.

- **Verified:** real REPL → parser → AST → semantic analyzer → Prolog CodeGen → MQI → SWI-Prolog → tabular output; no P1 fake query execution.
- **Verified:** `mvn clean package` on November 8, with 438 standard tests, and a separate 65-case live-MQI suite; real executable JAR startup and a live `MATCH` query.
- **Verified:** C0–C12 and additional 8b/8c with instructor-shaped queries adapted to the engine graph, including updates and deletions. Some test records are created during a REPL session and must be reset/recreated for a repeatable final demo.
- **Verified:** node-variable reuse across multiple patterns, with different labels combined by AND, and distinct physical-edge enforcement across multiple relationship patterns.
- **Verified with an important nuance:** semicolon-free statements execute after a blank line or EOF; first-Enter immediate execution without a semicolon is not established.
- **Historical verification:** `cyphail.bat repl` launched from the project root or its parent folder, preserving graph state within a session.
- **Not established:** permanent graph persistence across REPL restarts. Engine data created within a session should not be assumed to survive a restart.
- **Legacy behavior:** `.use` reads `data/graphs.json`; it does not switch the active graph inside SWI-Prolog.
- **Packaging limitation:** the shaded JAR is executable from the repository root and still expects `src/main/prolog/engine.pl` and `graph_store.pl` in the project layout. A standalone JAR without those source files has not been verified.
- **Execution scope:** although the parser recognizes `RETURN DISTINCT`, `RETURN *`, `ORDER BY`, `SKIP` and `LIMIT`, the current execution pathway rejects these constructs. They are not listed as mandatory *execution* features in the supplied P2 SPEC. The additional `DETACH DELETE` feature is beyond the 10-case sprint sheet, though supported by the current implementation.
- **Example data:** the instructor-provided sample graph may contain more than one age value for a node (e.g., `id3`), resulting in multiple Prolog rows for the same node ID when projecting `age`. Do not edit sample facts or suppress rows without reviewing the actual source and regression effects.
- **Additional review before submission:** confirm `.tree` behavior, inherited P1 commands and edge cases, document all external/AI-assisted sources, review every source change and untracked file, and prepare a self-contained, repeatable demo fixture.
- **Source changes after this point:** rerun `mvn test`, `mvn "-Dtest=*IT" test`, and `mvn clean package`; confirm the final commit, Git bundle, ZIP, and README all describe the same revision.

Passing tests proves the exercised behaviors, not every possible query or environment. **No Java or Maven version change is required by the observed successful build.**
---

## Instructor Review — November 17, 2026

The guide scores functionality in R0/C0–C12 (60 points), code quality/README/model/paradigm/smells (20 points), and the team's defense (20 points). The instructor also requires that the review sheet be provided in PDF format and warns that key missing prerequisites can void the evaluation.

| Criterion | Comparable query or operation | Current development evidence |
|---|---|---|
| R0 | Build from Windows CMD | Fresh `mvn clean package`: BUILD SUCCESS; 88 production/69 test sources; JAR created |
| C0 | `.load` | Exercised with `.cyphail` scripts |
| C1 | `-[r]->` | Manually exercised |
| C2 | `-[:Label]->` | Manually exercised |
| C3 | `<-[r:Label]-` and undirected relation | Manually exercised |
| C4 | Inline edge property and `RETURN r.property` | Manually exercised |
| C5 | WHERE comparisons on node and edge properties | Manually exercised |
| C6 | Boolean expressions including `NOT` | Manually exercised |
| C7 | MATCH two nodes, CREATE relation, reuse IDs | Manually exercised; no node clones observed |
| C8 | CREATE nodes and edge without preceding MATCH | Manually exercised |
| C9 | MATCH/WHERE/DELETE an edge | Manually exercised |
| C10 | Multi-pattern integrated query | Manually exercised |
| C11 | Multiple SET assignments | Manually exercised; verified changes in later MATCH |
| C12 | Multiple REMOVE assignments | Manually exercised; verified removal in later MATCH |

These statuses refer to **manual development REPL runs** adapted to the engine graph, **not** a professor-graded review or a portable finalized demo script. Before presentation, package and rehearse the R0 build, C0–C12 setup/query/expected outputs, and supplemental 8b/8c. A bare query without `;` was run successfully after an empty line, but immediate one-Enter execution is not yet established.

### Verified instructor-shaped query examples

The following commands correspond to the local test data in **one** REPL session (IDs are illustrative and are not stable across restarts):

```cypher
// C1: relationship variable
MATCH (p:Person)-[r]->(q:Person) RETURN r;

// C2: typed relationship without a relation variable
MATCH (a:Person)-[:Amigo]->(b:Person) RETURN a.name, b.name;

// C3: incoming and undirected forms
MATCH (b:Person)<-[r:Amigo]-(a:Person) RETURN a.name, b.name, r;
MATCH (a:Person)-[r:Amigo]-(b:Person) RETURN a.name, b.name, r;

// C4: inline relationship property (requires demo C4 fixture)
MATCH (a:DemoC4)-[r:PRUEBA_C4 {desde: 2015}]->(b:DemoC4)
RETURN a.name, b.name, r.desde;

// C5: WHERE combines node and relationship properties
MATCH (a:DemoC4)-[r:PRUEBA_C4 {desde: 2015}]->(b:DemoC4)
WHERE r.desde > 2010 AND b.edad >= 30
RETURN a.name, b.name, r.desde;

// C6: OR/NOT; more than one relation property
MATCH (a:DemoC4)-[r:PRUEBA_C4 {cargo: 'Dev'}]->(b:DemoC4)
WHERE r.desde >= 2018 OR NOT b.name = 'OtroDestino'
RETURN a.name, b.name, r.cargo, r.desde;

// C7: MATCH two existing nodes and CREATE an edge with properties
MATCH (a:DemoC4 {name: 'OrigenC4'}), (b:DemoC4 {name: 'DestinoC4'})
CREATE (a)-[:PRUEBA_C7 {desde: 2020, nivel: 'amigo'}]->(b);

// C8: standalone CREATE with string and boolean properties
CREATE (a:DemoC8 {name: 'OrigenC8'})
  -[r:PRUEBA_C8 {cargo: 'Dev', activo: true}]->(b:DemoC8 {name: 'DestinoC8'})
RETURN a, r, b;

// C9: delete edge only (requires C7)
MATCH (a:DemoC4)-[r:PRUEBA_C7]->(b:DemoC4)
WHERE r.desde < 2021 DELETE r;

// C10: three patterns with shared variables and decimal/Boolean logic
MATCH (p:DemoC10Persona {activo: true})-[t:TRABAJA_C10 {cargo: 'Dev'}]->(e:DemoC10Empresa),
      (q:DemoC10Persona)-[s:SOCIO_C10]->(e),
      (p)-[k:CONOCE_C10]-(q)
WHERE (t.desde >= 2018 OR s.porcentaje > 25.5) AND NOT k.desde = 2020
RETURN p.name, q.name, e.name, t.cargo, s.porcentaje, k.desde;

// C11: multiple SET assignments
MATCH (p:DemoC10Persona {name: 'AnaC10'})-[t:TRABAJA_C10]->(e:DemoC10Empresa)
SET p.nivel = 2, t.prioridad = 5 RETURN p, p.nivel, t, t.prioridad;

// C12: multiple REMOVE assignments
MATCH (p:DemoC10Persona {name: 'AnaC10'})-[t:TRABAJA_C10]->(e:DemoC10Empresa)
REMOVE p.nivel, t.prioridad RETURN p, t, t.cargo;
```

**Do not execute this block blindly as a script:** it depends on demo data constructed in prior steps and contains mutations. Preparation, cleanup, stable ordering and expected outputs must be packaged in `demo-tests/` before instructor review.

---

## Technologies

- Java (JDK 26.0.1 used locally; Maven compiler target `release 21`)
- Apache Maven
- JUnit 5
- picocli
- Gson
- SWI-Prolog
- MQI
- Git
- GitHub

The parser uses functional parser combinators, and the REPL uses Java stream operations to dispatch complete statements. These are code-level illustrations of FP usage alongside the OO AST/visitor, broker and handler components; instructor assessment of design quality requires source review.

The parser's `map`, `flatMap`, `Parsers.many` and `Parsers.optional` usage, together with the REPL's `statements.stream().allMatch(commandRouter::route)`, were inspected on October 29, 2026. These are source-level examples of functional-programming constructs; passing tests alone does not establish complete SPEC compliance.

---

## Sources and Credits

The project was developed using official course materials and related technical documentation, including:

- Cyphail project introduction and architecture.
- Sprint P1 specification, grammar, and official parsing test cases.
- `EIF400-II-2026-SPEC-Sprint-P2-Preliminar-CLoria.md` / PDF, instructor's preliminary P2 SPEC.
- `EIF400-II-2026-Cypahil-P2-10-Casos_Prueba_Unitarios.md` / PDF, instructor's C1–C10 and supplemental cases/validation rules.
- `EIF400_II_2026_Guia_Revision_Cyphail_P2_CLoria.docx` / PDF, November 17 review guide (R0, C0–C12, README/code/defense).
- `EIF400-II-2026-Cyphail_P2_MQI_Demo.zip`, instructor-provided SWI-Prolog MQI demonstration (reference implementation).
- Java documentation.
- Apache Maven documentation.
- JUnit documentation.
- picocli documentation.
- Gson documentation.
- SWI-Prolog and MQI documentation.

Cyphail is the project name established for EIF400.

---

## Artificial Intelligence Use Declaration

Generative artificial intelligence (ChatGPT) was used as a supporting tool in discussion, explanations of course concepts, code review, debugging, testing recommendations, and documentation drafting. **Code was also proposed with AI assistance and must be declared as such, not represented as exclusively human-authored.**

**Identifiable examples of AI-assisted code and review proposals** (this list is **not** an exhaustive authorship audit):

- `src/main/java/cr/ac/una/eif400/cyphail/compiler/analyzer/SemanticAnalyzer.java`: the duplicate-label check inserted in `analyzeNode()`.
- `src/test/java/cr/ac/una/eif400/cyphail/compiler/analyzer/SemanticAnalyzerTest.java`: duplicate-label and variable-reuse tests proposed/reviewed with assistance.
- `src/main/java/cr/ac/una/eif400/cyphail/frontend/Repl.java` and `src/test/java/cr/ac/una/eif400/cyphail/frontend/ReplMultilineIntegrationTest.java`: blank-line/EOF completion and REPL tests discussed with assistance.
- `src/test/java/cr/ac/una/eif400/cyphail/compiler/codegen/PrologCodeGeneratorMqiLiveIT.java`: three additional real-MQI variable/label reuse tests discussed and corrected with assistance.
- `README.md`: technical documentation has been drafted and revised with ChatGPT assistance, including the present revision.
- Earlier suggestions/reviews also involved `graph_store.pl`, CodeGen, parser, AST and MQI integration. **Before final submission the team must audit each source file against its actual edit history, attribute incorporated assistance and third-party work accurately, and comply with the instructor's restrictions.**

The project team is responsible for individually understanding, independently verifying, testing, and defending all submitted code. The course guide prohibits **agentic AI** and undisclosed AI-generated or third-party code. This README does not certify compliance with that prohibition; the team must check its actual development process and the instructor's rules. The declaration should be expanded with precise source/code attribution before submission.

---

## Sprint P2 Submission

The preliminary SPEC identifies **November 1, 2026** as a projected delivery date; the definitive deadline is subject to the instructor's confirmation.

The submission should follow the official course procedure and naming convention:

```text
EIF400-II-2026-Cyphail-P2-NN-GG.zip
```

The ZIP must contain:

```text
cyphail.bundle
```

The bundle must represent the **last intended, committed revision**. Do not prepare the bundle from uncommitted files and assume those changes are included. The final ZIP and bundle have **not** been generated or verified in the current review.

The **November 17, 2026 instructor review sheet** must also be filled in where required and provided as a PDF. The team must have the R0 build demonstration and C0–C12 comparable scripts ready, explain the project architecture, meet the approximately 15-minute demo expectation, and defend the code. The review guide warns that failure to meet specified threshold requirements can nullify the project.

Before preparing the submission, the team should confirm the final build, general tests, live MQI tests, source changes, documentation, GitHub history, bundle contents, and archive integrity.

The instructor also expects evidence of ongoing Git contributions from all team members and expects every member to understand and defend the complete codebase.

---

## Final Verification and Delivery Checklist

Complete these checks before treating the repository as ready for submission:

- [x] `mvn test` passed **438** standard tests on November 8, 2026 (0 failures/errors/skips).
- [x] `mvn "-Dtest=*IT" test` passed **65** real SWI-Prolog/MQI integration tests (0 failures/errors/skips).
- [x] `mvn clean package` completed with **BUILD SUCCESS**, recompiling 88 production and 69 test sources with `release 21` and creating the shaded JAR.
- [x] The JAR started from CMD with `--help` and `repl`; real MATCH results were printed.
- [x] A query without `;` executed after an empty REPL line; the EOF path also has passing automated tests.
- [x] `.load "prueba_load_p2.cyphail"` executed five statements in order and persisted a property update within the session.
- [x] Instructor-shaped R0/C0–C12 functional tests were exercised manually with graph data prepared in the REPL.
- [x] Supplemental 8b join and 8c disconnected product were exercised manually.
- [x] Semantic error checks passed for repeated relationship variables, node/relationship role conflicts, reserved `__` names and duplicated labels within one node pattern.
- [x] Both positive and negative joins for different labels on a reused node variable were exercised; chosen policy is conjunction of labels.
- [x] Positive and negative tests for distinct physical edges were exercised.
- [ ] Confirm what the instructor expects from a **single Enter without `;`** and verify/improve the REPL if necessary; do not call this behavior already proven.
- [ ] Audit `.tree`, other inherited P1 commands and any remaining exact SPEC/edge cases after source review.
- [ ] Convert locally prepared C0–C12 demo steps into **repeatable, versioned scripts** with data setup/cleanup and expected output (without depending on earlier REPL state).
- [ ] Inspect the content and suitability of all untracked files, including `demo-tests/`, `prueba_load_p2.cyphail` and `informe_Emmanuel_git.txt`.
- [ ] Review each production and test diff, project structure, documentation, OOP/FP and opportunities for DRY improvements.
- [ ] Complete the AI-assisted code and external-source attribution audit; comply with the no-agentic-AI rule and ensure each teammate understands the code.
- [ ] Review the Git contribution history of the five team members; confirm final intended commit and push to the shared repository.
- [ ] Generate and verify `cyphail.bundle` **from the final intended commit**.
- [ ] Create and inspect the group-named ZIP containing the required bundle and any additional requested materials.
- [ ] Fill the November 17 instructor review sheet where required and export it to PDF; do not invent attendance or official scores.
- [ ] Rehearse the approximately 15-minute live demonstration and prepare the team for questions.
- [ ] If production or test code changes, rerun `mvn test`, `mvn "-Dtest=*IT" test` and `mvn clean package`, and ensure docs/commit/bundle refer to the same revision.

Checked items summarize the verified development environment. Unchecked items remain pending; they are not claims that the submission is complete.
---

## License and Academic Use

Cyphail is an academic project developed for **EIF400 — Programming Paradigms** at the **National University of Costa Rica**.

No additional license is asserted here beyond what is explicitly included in the repository.
