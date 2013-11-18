@echo off

if "%OS%" == "Windows_NT" setlocal

if NOT DEFINED JAVA_HOME goto :err

set DIR=%CD%
set var2= 
set argC=0
for %%x in (%*) do Set /A argC+=1

IF %argC% == 0  GOTO :zeroArg
IF %argC% == 2  GOTO :twoArg
IF %argC% == 1  GOTO :oneArg 

:zeroArg
        echo SDI Build Wrapper v0.000154
        echo Wrapper script for more advanced SDI build commands.  Since Gradle cannot
        echo guarantee order on dependencies, this wrapper will correctly execute more
        echo commonly chained build commands.
        echo Normal Gradle commands can also still be used by this wrapper.
        echo -----------------------------------------------------------------------
        echo Usage: build.bat [gradle command] or [advanced command] ...
        echo -----------------------------------------------------------------------
        echo Available Advanced Commands:
        echo ------------------------------
        echo     buildall      - executes the gradle build with build and eclipse
        echo     rebuild       - executes the gradle build with clobber, build and eclipse
        echo     full          - executes the gradle build with build, eclipse and runSDITasks
        echo ------------------------------------------------------------------------------------
        echo Example:
        echo 1. To execute a normal build:
        echo           build.bat build
        echo 2. To execute an advanced command:
        echo           build.bat rebuild
        echo --------------------------------------------------------------------------------------
        echo Options:
        echo ---------------
        echo -h          - this help doc
        echo ---------------
	%DIR%/tools/gradle-1.4/bin/gradle 
goto finally
	
:twoArg
        echo *****************************
	echo Running SDI Build for task %*
        echo *****************************
	%DIR%/tools/gradle-1.4/bin/gradle clean %*	
goto finally

:oneArg
	if "%1" == "rebuild" goto :reBuild
	if "%1" == "buildall" goto :buildAll
	if "%1" == "full" goto :full
	if "%1" == "build" goto :build
	if "%1" == "-h" goto :zeroArg
goto finally

:reBuild
		SET GRADLE_ARG="clobber build eclipse"
        echo *****************************
        	echo Running SDI Build for task %1 %GRADLE_ARG%
        echo *****************************
		%DIR%/tools/gradle-1.4/bin/gradle %GRADLE_ARG%

goto finally

:buildAll
		SET GRADLE_ARG="clean build eclipse"
        echo *****************************
                echo Running SDI Build for task %1 (build eclipse)
        echo *****************************
		%DIR%/tools/gradle-1.4/bin/gradle %GRADLE_ARG%
goto finally

:full
        SET GRADLE_ARG="clean build eclipse"
        echo *****************************
				echo Running SDI Build for task %1 (build eclipse runSDITests)
        echo *****************************
                %DIR%/tools/gradle-1.4/bin/gradle %GRADLE_ARG%
                %DIR%/tools/gradle-1.4/bin/gradle clean runSDITests
		%DIR%/tools/gradle-1.4/bin/gradle clean build

goto finally

:build
        echo *****************************
                echo Running SDI Build for task %1
        echo *****************************
		%DIR%/tools/gradle-1.4/bin/gradle clean %1

goto finally

:err
echo The JAVA_HOME environment variable must be set to run this program!
pause

:finally

ENDLOCAL

