


# Cyphail - Sprint P2

## Documentación técnica del módulo de integración MQI con SWI-Prolog

### Información del proyecto

- **Proyecto:** Cyphail.
- **Curso:** EIF400 - Paradigmas de Programación.
- **Universidad:** Universidad Nacional de Costa Rica.
- **Escuela:** Escuela de Informática.
- **Grupo:** 04-10am.
- **Sprint:** P2.
- **Responsable de esta contribución:** Emmanuel Núñez Jiménez.
- **Fecha de verificación:** 17 de octubre de 2026.

### Integrantes del equipo

- Emmanuel Núñez Jiménez.
- Julissa Solano Valverde.
- Valery Alfaro Morales.
- Keynell Molina Mora.
- Roy Arias Mejía.

Este documento describe la contribución individual de Emmanuel al proyecto grupal Cyphail durante el Sprint P2, específicamente la implementación de la comunicación entre Java y SWI-Prolog mediante Machine Query Interface (MQI).

El documento registra las funcionalidades implementadas, su arquitectura, las pruebas realizadas, los commits publicados y los trabajos que todavía deben coordinarse con los demás integrantes.

**Importante:** este documento representa un avance técnico individual. No significa que la implementación completa del Sprint P2 esté terminada.

---

## 1. Objetivo de la contribución

El objetivo principal es implementar la infraestructura de comunicación necesaria para que Cyphail pueda ejecutar consultas Prolog desde una aplicación Java utilizando Machine Query Interface (MQI).

El módulo desarrollado permite:

1. Iniciar un proceso SWI-Prolog desde Java.
2. Obtener los datos necesarios para establecer una conexión MQI.
3. Establecer la comunicación mediante sockets TCP.
4. Autenticar la conexión con el servidor MQI.
5. Enviar goals Prolog para su ejecución.
6. Recibir las respuestas producidas por SWI-Prolog.
7. Interpretar las respuestas JSON mediante Gson.
8. Obtener los bindings de las variables resultantes.
9. Identificar consultas exitosas, consultas sin soluciones y errores.
10. Reutilizar una misma conexión para varias consultas.
11. Cerrar adecuadamente la conexión y el proceso SWI-Prolog.

La implementación constituye la base de comunicación que permitirá integrar el motor Prolog con el resto de los componentes de Cyphail.

El desarrollo se realizó tomando como referencia la arquitectura del proyecto y los materiales de MQI proporcionados durante el curso.

---

## 2. Arquitectura del módulo MQI

### 2.1. Arquitectura general

El módulo está formado por cuatro componentes principales:

- `PrologResult.java`
- `MqiResponseParser.java`
- `MqiClient.java`
- `MqiBroker.java`

El flujo de comunicación es el siguiente:

```text
+------------------------------------+
| Componente que solicita una        |
| consulta Prolog                    |
+------------------------------------+
                 |
                 v
+------------------------------------+
| MqiBroker                          |
| Administración de la sesión        |
+------------------------------------+
                 |
                 v
+------------------------------------+
| MqiClient                          |
| Comunicación mediante MQI          |
+------------------------------------+
                 |
                 v
+------------------------------------+
| Socket TCP / Protocolo MQI         |
+------------------------------------+
                 |
                 v
+------------------------------------+
| SWI-Prolog                         |
| Ejecución del goal                 |
+------------------------------------+
                 |
                 v
+------------------------------------+
| Respuesta JSON de MQI              |
+------------------------------------+
                 |
                 v
+------------------------------------+
| MqiResponseParser                  |
| Interpretación del JSON            |
+------------------------------------+
                 |
                 v
+------------------------------------+
| PrologResult                       |
| Resultado estructurado en Java     |
+------------------------------------+
```

### 2.2. Responsabilidad de cada componente

**MqiBroker**

Administra el ciclo de vida de la sesión Prolog. Inicia el proceso, establece la conexión y permite ejecutar consultas sin tener que iniciar SWI-Prolog nuevamente para cada operación.

**MqiClient**

Se encarga de la comunicación con el servidor MQI. Envía las solicitudes, recibe las respuestas y mantiene el protocolo de comunicación necesario.

**MqiResponseParser**

Interpreta las respuestas JSON recibidas mediante MQI y las transforma en representaciones utilizables desde Java.

**PrologResult**

Representa los diferentes resultados que puede devolver una consulta Prolog.

### 2.3. Separación de responsabilidades

La separación entre estos componentes permite mantener desacopladas la administración de procesos, la comunicación por sockets, la interpretación del protocolo y la representación de resultados.

De esta forma, otros módulos de Cyphail podrán utilizar el broker sin implementar directamente el protocolo MQI.

---

## 3. Clases implementadas

Las clases principales están ubicadas en:

```text
src/main/java/cr/ac/una/eif400/cyphail/engine/mqi/
```

### 3.1. PrologResult.java

Esta clase proporciona un modelo para representar los resultados de las consultas ejecutadas en SWI-Prolog.

Contempla tres tipos principales:

**PrologResult.Success**

Representa una consulta ejecutada exitosamente y contiene las soluciones obtenidas.

Las soluciones se representan mediante estructuras que asocian los nombres de las variables Prolog con sus respectivos valores.

**PrologResult.Failure**

Representa una consulta que no produjo soluciones.

Este resultado no debe confundirse con una falla técnica de comunicación.

**PrologResult.PrologError**

Representa un error producido durante la ejecución de una consulta Prolog.

Esta distinción facilita que las capas superiores de Cyphail puedan tratar adecuadamente cada situación.

### 3.2. MqiResponseParser.java

Esta clase procesa las respuestas JSON recibidas desde SWI-Prolog.

Sus responsabilidades incluyen:

- Interpretar respuestas de ejecución exitosa.
- Identificar consultas sin soluciones.
- Extraer los bindings de las variables.
- Procesar respuestas con múltiples soluciones.
- Conservar la información de los valores recibidos.
- Interpretar números, cadenas y booleanos.
- Procesar errores reportados por Prolog.
- Detectar respuestas inválidas o no reconocidas.
- Recuperar información de variables como `Rows`.

Para procesar el contenido JSON se utiliza la biblioteca Gson.

El propósito de esta clase es evitar que los demás componentes del sistema tengan que manipular directamente las respuestas JSON del protocolo.

### 3.3. MqiClient.java

Esta clase implementa la comunicación de bajo nivel con SWI-Prolog mediante MQI.

Sus responsabilidades principales son:

1. Utilizar los flujos de entrada y salida asociados con la conexión.
2. Realizar la autenticación correspondiente.
3. Enviar las solicitudes respetando el formato del protocolo.
4. Recibir las respuestas enviadas por el servidor.
5. Ejecutar goals Prolog.
6. Administrar las operaciones necesarias para cerrar la comunicación.

Entre los métodos utilizados se encuentran:

```java
MqiClient(InputStream, OutputStream)

authenticate(String password)

run(String goal)

run(String goal, int timeoutSeconds)

close()
```

Las pruebas implementadas verifican el comportamiento del cliente tanto con datos controlados como mediante comunicación real con SWI-Prolog.

### 3.4. MqiBroker.java

Esta clase se encarga de administrar el proceso SWI-Prolog y la sesión de comunicación MQI.

Sus responsabilidades incluyen:

- Iniciar automáticamente SWI-Prolog.
- Obtener los datos de conexión del servidor MQI.
- Establecer la comunicación con el servidor.
- Autenticar la sesión.
- Mantener disponible una conexión reutilizable.
- Enviar los goals recibidos desde otros componentes.
- Devolver el resultado de la ejecución.
- Identificar el estado de la conexión.
- Cerrar la conexión y finalizar el proceso correspondiente.

Sus operaciones principales incluyen:

```java
MqiBroker.start()

MqiBroker.start(Path engineFile)

execute(String goal)

execute(String goal, int timeoutSeconds)

isOpen()

close()
```

El método `start()` permite iniciar una sesión de SWI-Prolog sin especificar un archivo adicional.

El método `start(Path engineFile)` permite cargar un archivo Prolog durante el inicio de la sesión.

Esta segunda opción será importante cuando el equipo integre el motor Prolog definitivo de Cyphail.

---

## 4. Ejemplos de utilización

### 4.1. Inicio y ejecución de una consulta

El broker puede administrarse utilizando `try-with-resources`, lo que permite cerrar la sesión al salir del bloque.

Ejemplo:

```java
try (MqiBroker broker = MqiBroker.start()) {
    PrologResult result = broker.execute("X is 2+3");

    System.out.println(result);
}
```

En este ejemplo:

1. Se inicia una sesión MQI.
2. Se ejecuta el goal `X is 2+3`.
3. SWI-Prolog procesa la consulta.
4. Java recibe el resultado.
5. Se muestra la representación del resultado.
6. La sesión se cierra al finalizar el bloque.

El ejemplo ilustra la comunicación con SWI-Prolog; no corresponde todavía a una consulta completa escrita en el lenguaje Cyphail.

### 4.2. Inicio con un archivo Prolog

También es posible indicar un archivo Prolog que deba cargarse al iniciar la sesión.

```java
Path engine = Path.of("ruta/al/engine.pl");

try (MqiBroker broker = MqiBroker.start(engine)) {
    PrologResult result = broker.execute("X is 2+3");

    System.out.println(result);
}
```

La ruta del ejemplo debe sustituirse por una ruta válida correspondiente al archivo Prolog que se utilizará.

Este mecanismo facilita la incorporación futura del motor desarrollado por el equipo.

### 4.3. Reutilización de la sesión

El broker está diseñado para permitir varias consultas utilizando la misma sesión.

Ejemplo:

```java
try (MqiBroker broker = MqiBroker.start()) {
    PrologResult primero = broker.execute("X is 2+3");

    PrologResult segundo = broker.execute("Y is 10*2");

    System.out.println(primero);
    System.out.println(segundo);
}
```

Esta característica evita crear una nueva conexión para cada goal dentro del mismo bloque de ejecución.

---

## 5. Pruebas implementadas

El módulo dispone de pruebas unitarias y pruebas de integración reales con SWI-Prolog.

### 5.1. MqiResponseParserTest.java

Contiene ocho pruebas unitarias para verificar el procesamiento de las respuestas MQI.

Entre los escenarios comprobados están:

- Respuestas sin soluciones.
- Respuestas exitosas sin bindings.
- Extracción del contenido asociado con `Rows`.
- Respuestas con múltiples soluciones.
- Procesamiento de valores numéricos.
- Procesamiento de valores booleanos.
- Manejo de errores de Prolog.
- Rechazo de respuestas inválidas o no reconocidas.

**Resultado verificado:**

- Pruebas ejecutadas: 8.
- Fallos: 0.
- Errores: 0.

### 5.2. MqiClientTest.java

Contiene ocho pruebas unitarias para comprobar el comportamiento del cliente MQI.

Estas pruebas utilizan datos y flujos controlados para validar el intercambio de mensajes, la autenticación y el procesamiento de las respuestas.

**Resultado verificado:**

- Pruebas ejecutadas: 8.
- Fallos: 0.
- Errores: 0.

### 5.3. MqiLiveIT.java

Corresponde a una prueba de integración real entre Java y SWI-Prolog.

Verifica que el cliente pueda comunicarse con una instancia real de SWI-Prolog mediante MQI y ejecutar una consulta.

**Resultado verificado:**

- Pruebas ejecutadas: 1.
- Fallos: 0.
- Errores: 0.

### 5.4. MqiBrokerLiveIT.java

Contiene dos pruebas de integración reales relacionadas con la administración del broker.

Los escenarios verificados incluyen:

- Inicio del proceso SWI-Prolog.
- Establecimiento y autenticación de la conexión MQI.
- Ejecución de consultas.
- Reutilización de la sesión.
- Carga de un archivo Prolog.
- Cierre de los recursos utilizados.

**Resultado verificado:**

- Pruebas ejecutadas: 2.
- Fallos: 0.
- Errores: 0.

### 5.5. MqiRowsLiveIT.java

Esta prueba verifica la recepción real de una variable Prolog llamada `Rows`.

El objetivo es comprobar que Java pueda recuperar correctamente el texto devuelto por SWI-Prolog, incluyendo los saltos de línea.

Ejemplo del contenido comprobado:

```text
nombre edad
Jorge 43
```

Esta prueba es importante porque el Sprint P2 requiere presentar resultados en formato tabular.

**Resultado verificado:**

- Pruebas ejecutadas: 1.
- Fallos: 0.
- Errores: 0.

La prueba demuestra que el texto de `Rows` puede transmitirse mediante MQI, pero todavía no verifica su presentación final desde el REPL de Cyphail.

---

## 6. Comandos para compilar, probar y ejecutar

Los siguientes comandos deben ejecutarse desde la carpeta raíz del repositorio Cyphail.

### 6.1. Ejecutar las pruebas generales

```cmd
mvn test
```

Resultado verificado durante el desarrollo:

```text
Tests run: 79, Failures: 0, Errors: 0, Skipped: 0
```

Estas 79 pruebas corresponden a la selección habitual de Maven Surefire.

### 6.2. Ejecutar la prueba real del cliente MQI

```cmd
mvn -Dtest=MqiLiveIT test
```

Resultado verificado:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

### 6.3. Ejecutar las pruebas reales del broker

```cmd
mvn -Dtest=MqiBrokerLiveIT test
```

Resultado verificado:

```text
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```

### 6.4. Ejecutar la prueba real de Rows

```cmd
mvn -Dtest=MqiRowsLiveIT test
```

Resultado verificado:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

**Nota:** las pruebas cuyos nombres terminan en `IT` se ejecutaron explícitamente. No forman parte de las 79 pruebas seleccionadas por el comando habitual `mvn test`.

Para ejecutar estas pruebas de integración reales debe estar instalado y disponible SWI-Prolog.

### 6.5. Empaquetar el proyecto

Durante las verificaciones de P2 se utilizó:

```cmd
mvn -Dmaven.jar.forceCreation=true package
```

Este comando permite reconstruir el JAR antes de que Maven Shade incorpore las dependencias.

El comando terminó con:

```text
BUILD SUCCESS
```

### 6.6. Comprobar la ayuda del JAR ejecutable

```cmd
java -jar "target\cyphail-0.1.0.jar" --help
```

Resultado verificado:

```text
Usage: cyphail [-hV] [COMMAND]
Cyphail graph query language prototype.
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  repl  Starts the interactive Cyphail REPL.
```

### 6.7. Iniciar el REPL

```cmd
java -jar "target\cyphail-0.1.0.jar" repl
```

Se verificó que la aplicación muestra su mensaje de bienvenida y permite ingresar comandos.

Para salir:

```text
.exit
```

La ejecución del REPL y su cierre se comprobaron correctamente.

**Aclaración:** esta verificación confirma el funcionamiento del REPL existente, pero no la integración completa del lenguaje Cyphail con el motor Prolog definitivo.

---

## 7. Verificación del empaquetado Maven

Durante el desarrollo se detectaron advertencias producidas por `maven-shade-plugin` relacionadas con clases duplicadas.

El problema aparecía al volver a empaquetar un JAR que ya contenía dependencias incorporadas, como Gson y Picocli.

Se realizaron verificaciones para identificar y controlar esta situación.

### 7.1. Comprobación del JAR original

Se verificó que el JAR original reconstruido no contenía las clases de las dependencias examinadas.

El comando utilizado fue:

```cmd
jar tf "target\original-cyphail-0.1.0.jar" | findstr /i /c:"com/google/gson/Gson.class" /c:"picocli/CommandLine.class"
```

El comando terminó sin mostrar coincidencias.

### 7.2. Reconstrucción repetida

Se ejecutó dos veces consecutivas:

```cmd
mvn -Dmaven.jar.forceCreation=true package
```

En ambas ejecuciones se obtuvo:

- Compilación exitosa.
- 79 pruebas generales aprobadas.
- Cero fallos y cero errores.
- Reconstrucción del JAR.
- Ausencia de las advertencias masivas de clases duplicadas.

Persistieron algunas advertencias sobre recursos compartidos de las dependencias, como `MANIFEST.MF` y `module-info`.

Estas advertencias no impidieron la generación del JAR ejecutable.

### 7.3. Estado de la configuración

Durante estas verificaciones no se modificó el archivo `pom.xml`.

El comando con `forceCreation` se utiliza como medida verificada para el empaquetado incremental.

Cualquier ajuste permanente de la configuración Maven deberá coordinarse con el resto del equipo, debido a que `pom.xml` es un archivo compartido.

---

## 8. Commits de la contribución individual

### 8.1. Repositorio

```text
https://github.com/EmmanuelNJ02/cyphail
```

### 8.2. Rama utilizada

```text
feature/p2-integracion-mqi-emmanuel
```

### 8.3. Commits publicados

**Commit 1**

```text
66c5d8b
```

Implementación del modelo y el parser de respuestas MQI, incluyendo las pruebas correspondientes.

**Commit 2**

```text
df0cfcc
```

Implementación del cliente MQI y pruebas de integración con SWI-Prolog.

**Commit 3**

```text
e706f69
```

Implementación del broker MQI y pruebas de integración con SWI-Prolog.

**Commit 4**

```text
14be033
```

Incorporación de una prueba de integración real para verificar la respuesta tabular `Rows`.

Los cuatro commits fueron publicados en GitHub desde la rama individual de desarrollo.

### 8.4. Verificación del estado de Git

Se ejecutó:

```cmd
git status
```

Y se obtuvo:

```text
On branch feature/p2-integracion-mqi-emmanuel
Your branch is up to date with 'origin/feature/p2-integracion-mqi-emmanuel'.

nothing to commit, working tree clean
```

Este resultado corresponde a la verificación realizada antes de crear el presente documento de documentación.

La incorporación de este archivo generará un cambio adicional que deberá registrarse mediante un nuevo commit.

---

## 9. Integración pendiente con el Sprint P2

Aunque la comunicación MQI ya fue implementada y probada, todavía no está conectada con el flujo completo de ejecución del lenguaje Cyphail.

### 9.1. Arquitectura final esperada

```text
+------------------------------------+
| REPL de Cyphail                    |
+------------------------------------+
                 |
                 v
+------------------------------------+
| Lexer / Parser                     |
+------------------------------------+
                 |
                 v
+------------------------------------+
| AST / Análisis semántico           |
+------------------------------------+
                 |
                 v
+------------------------------------+
| CodeGen                            |
| Generación de goals Prolog         |
+------------------------------------+
                 |
                 v
+------------------------------------+
| MqiBroker                          |
+------------------------------------+
                 |
                 v
+------------------------------------+
| MqiClient                          |
+------------------------------------+
                 |
                 v
+------------------------------------+
| Motor SWI-Prolog                   |
+------------------------------------+
                 |
                 v
+------------------------------------+
| Respuesta JSON de MQI              |
+------------------------------------+
                 |
                 v
+------------------------------------+
| MqiResponseParser / PrologResult   |
+------------------------------------+
                 |
                 v
+------------------------------------+
| Presentación tabular en el REPL    |
+------------------------------------+
```

### 9.2. Actividades pendientes

Para completar esta integración será necesario:

1. Conectar la salida del CodeGen con la entrada del broker MQI.
2. Incorporar el archivo del motor Prolog definitivo.
3. Determinar cuándo debe iniciarse la sesión MQI.
4. Mantener disponible la sesión durante las operaciones que la requieran.
5. Procesar los resultados devueltos por el motor.
6. Presentar adecuadamente los resultados tabulares.
7. Reemplazar la ejecución simulada utilizada durante P1.
8. Integrar las funcionalidades del lenguaje requeridas para P2.
9. Verificar el funcionamiento completo mediante pruebas de extremo a extremo.
10. Coordinar los cambios de los componentes compartidos.

También deberán considerarse las funcionalidades nuevas requeridas por la especificación oficial del Sprint P2, incluyendo la ampliación de la gramática y el procesamiento de sentencias.

### 9.3. Coordinación del equipo

El proyecto es desarrollado por:

- Emmanuel.
- Julissa.
- Valery.
- Keynell.
- Roy.

La distribución definitiva de responsabilidades de integración debe coordinarse entre los integrantes.

Es importante que cada persona realice contribuciones verificables, registre sus cambios mediante Git y pueda explicar el funcionamiento del código desarrollado.

### 9.4. Limitaciones actuales

El módulo MQI ya permite ejecutar goals Prolog desde Java.

Sin embargo, esto no implica que actualmente se puedan ejecutar todas las sentencias Cyphail desde el REPL utilizando un motor real.

Entre los pendientes más importantes están:

- CodeGen integrado al broker.
- Motor Prolog definitivo integrado.
- Ejecución real de consultas Cyphail.
- Presentación tabular final.
- Pruebas completas de integración del sistema.

Por lo tanto, el estado correcto del módulo es:

**Infraestructura MQI implementada y probada; integración completa del Sprint P2 pendiente.**

---

## 10. Preguntas técnicas para el profesor

Durante el desarrollo y la planificación de la integración surgieron varias preguntas que pueden servir para validar decisiones arquitectónicas con el profesor.

### Consulta 1. Inicio de la sesión MQI

¿El broker debe iniciar SWI-Prolog cuando se abre el REPL de Cyphail o únicamente cuando el usuario ejecuta la primera consulta que requiere el motor?

### Consulta 2. Integración con CodeGen

¿El componente CodeGen debe entregar directamente un `String` que contenga el goal Prolog, o se espera utilizar alguna estructura intermedia para comunicarse con el broker?

### Consulta 3. Presentación de resultados tabulares

¿El motor Prolog debe devolver el resultado tabular como una variable `Rows` que contenga texto formateado, o se espera que Java transforme los bindings recibidos para construir la tabla?

### Consulta 4. Ubicación del motor Prolog

¿El archivo principal `engine.pl` debe cargarse automáticamente desde una ubicación fija del proyecto, o su ruta debe poder configurarse?

### Consulta 5. Ciclo de vida de la conexión

¿Es adecuado mantener una única conexión MQI durante toda la sesión del REPL y cerrarla cuando el usuario ejecuta `.exit`?

### Consulta 6. Responsabilidad del broker

¿El broker debe limitarse a ejecutar goals y devolver resultados estructurados, dejando la presentación tabular a otro componente del frontend?

Estas preguntas no representan problemas ya resueltos. Son puntos que conviene confirmar para asegurar que la integración siga la arquitectura esperada por el profesor.

---

## 11. Conclusiones y estado de la contribución

La contribución individual de Emmanuel permitió implementar la base de comunicación real entre Java y SWI-Prolog mediante MQI.

Los resultados más relevantes son:

1. Implementación del modelo `PrologResult`.
2. Implementación del procesador de respuestas `MqiResponseParser`.
3. Implementación del cliente `MqiClient`.
4. Implementación del administrador de sesiones `MqiBroker`.
5. Desarrollo de pruebas unitarias.
6. Ejecución de cuatro pruebas de integración reales.
7. Verificación de las 79 pruebas generales del proyecto.
8. Comprobación del empaquetado mediante Maven.
9. Verificación de la ejecución del JAR y del REPL.
10. Publicación de cuatro commits individuales en GitHub.

El módulo proporciona una infraestructura reutilizable para la integración con los demás componentes de Cyphail.

El siguiente objetivo técnico consiste en conectar esta infraestructura con CodeGen, el motor Prolog y la presentación de resultados del REPL.

Para completar el Sprint P2 será necesario continuar con el trabajo coordinado de los cinco integrantes del equipo.

### Estado final de este documento

- **Contribución individual MQI:** implementada y probada.
- **Pruebas generales:** 79 aprobadas.
- **Pruebas de integración MQI reales:** 4 aprobadas.
- **Commits individuales publicados:** 4.
- **JAR y REPL:** verificados.
- **Integración completa del Sprint P2:** pendiente.
- **Documentación:** avance técnico individual.

Este documento no sustituye el README general ni la documentación final de entrega del proyecto.

---

## 12. Declaración sobre el uso de herramientas de apoyo

Durante el desarrollo se utilizaron herramientas de apoyo, incluyendo inteligencia artificial generativa, para explicar conceptos, revisar código, proponer pruebas, analizar errores, estructurar documentación y apoyar la resolución de problemas técnicos.

Las soluciones propuestas fueron revisadas mediante compilación, pruebas y ejecuciones desde Windows CMD.

El equipo es responsable de comprender, mantener, integrar y defender el código entregado.

---

**Fin del documento de contribución individual MQI - Sprint P2 de Cyphail.**
