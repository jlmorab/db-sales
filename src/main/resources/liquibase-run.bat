@echo off
setlocal

SET CHANGELOG=db-sales-changelog.xml
SET DB_URL="jdbc:postgresql://localhost:5432/postgres"

set PARAMS=--changeLogFile=%CHANGELOG% --url=%DB_URL% --username=%DB_ADMIN% --password=%DB_ADMIN_PASS%

set COMMAND_ARGS=
:parse_args
if "%~1"=="" goto execute
set COMMAND_ARGS=%COMMAND_ARGS% %1
shift
goto parse_args

:execute
liquibase %PARAMS% %COMMAND_ARGS%

endlocal
