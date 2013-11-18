@echo off

if "%OS%" == "Windows_NT" setlocal

if NOT DEFINED JAVA_HOME goto :err

REM Ensure that any user defined CLASSPATH variables are not used on startup
set CLASSPATH=

REM For each jar in the lib directory call append to build the CLASSPATH variable.
for %%i in ("..\conf\*.*") do call :append "%%i"
for %%j in ("..\lib\*.jar") do call :append "%%j"

goto runModbus

:append
set CLASSPATH=%CLASSPATH%;%1
goto :eof

:runModbus
"%JAVA_HOME%\bin\java" -cp %CLASSPATH% gov.nrel.modbusclient.ModBusClient ../conf/meters.csv ../conf/ModBusClient.properties ../conf/meterModel.json
goto finally

:err
echo The JAVA_HOME environment variable must be set to run this program!
pause

:finally

ENDLOCAL
