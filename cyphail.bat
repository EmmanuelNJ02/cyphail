@echo off
setlocal

REM ============================================================
REM Cyphail launcher
REM EIF400 - Paradigmas de Programacion
REM Universidad Nacional de Costa Rica
REM Work Group: 04
REM Schedule: 10:00 a.m.
REM Group Code: 04-10am
REM ============================================================

where java >nul 2>&1

if errorlevel 1 (
    echo ERROR. Java is not installed or is not available in PATH.
    exit /b 1
)

set "CYPHAIL_JAR=%~dp0target\cyphail-0.1.0.jar"

if not exist "%CYPHAIL_JAR%" (
    echo ERROR. Cyphail has not been built yet.
    echo Run "mvn clean package" from the project directory first.
    exit /b 1
)

java -jar "%CYPHAIL_JAR%" %*

endlocal