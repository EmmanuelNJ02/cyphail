
@echo off
setlocal

REM ============================================================
REM Cyphail Launcher - Sprint P2
REM Version: 0.1.0
REM Course: EIF400 - Programming Paradigms
REM University: National University of Costa Rica
REM School: School of Informatics
REM Work Group: 04
REM Schedule: 10:00 a.m.
REM Group Code: 04-10am
REM Date: November 2026
REM ============================================================

REM Verify that Java is available.
where java >nul 2>&1

if errorlevel 1 (
    echo ERROR: Java is not installed or is not available in PATH.
    exit /b 1
)

REM Resolve the project root relative to this launcher.
set "CYPHAIL_ROOT=%~dp0"
set "CYPHAIL_JAR=%CYPHAIL_ROOT%target\cyphail-0.1.0.jar"

REM Verify that the application has been built.
if not exist "%CYPHAIL_JAR%" (
    echo ERROR: Cyphail has not been built yet.
    echo Run "mvn package" from the project directory first.
    exit /b 1
)

REM Switch to the project root so the Prolog engine can be found.
pushd "%CYPHAIL_ROOT%" >nul

if errorlevel 1 (
    echo ERROR: Unable to access the Cyphail project directory.
    exit /b 1
)

REM Execute Cyphail with the supplied command-line arguments.
java -jar "%CYPHAIL_JAR%" %*

REM Preserve the application's exit code.
set "CYPHAIL_EXIT_CODE=%ERRORLEVEL%"

REM Restore the original working directory.
popd

endlocal & exit /b %CYPHAIL_EXIT_CODE%
