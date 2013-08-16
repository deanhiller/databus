#!/bin/bash
#test
export DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ "$#" -eq "0" ]
then
	echo
              #2345678901234567890123456789012345678901234567890123456789012345678901234567890
        echo "SDI Build Wrapper v0.000154"
        echo
        echo "Wrapper script for more advanced SDI build commands.  Since Gradle cannot"
        echo "guarantee order on dependencies, this wrapper will correctly execute more"
        echo "commonly chained build commands."
        echo
        echo "Normal Gradle commands can also still be used by this wrapper."
        echo
        echo "Usage: ./build.sh [gradle command] or [advanced command] ..."
        echo
        echo "Available Advanced Commands:"
        echo
        echo "    buildall      - executes the gradle build with build and eclipse"
        echo "    rebuild       - executes the gradle build with clobber, build and eclipse"
        echo "    full          - executes the gradle build with build, eclipse and runSDITasks"
        echo
        echo "Example:"
        echo
        echo "  To execute a normal build:"
        echo "          ./build.sh build"
        echo
        echo "  To execute an advanced command:"
        echo "          ./build.sh rebuild"
        echo
        echo "Options:"
        echo
        echo "    -h          - this help doc"
        echo	
	$DIR/tools/gradle-1.4/bin/gradle 
elif [ "$#" -gt "1" ]
then
	echo
	echo "Running SDI Build for task $@"
	echo
	$DIR/tools/gradle-1.4/bin/gradle clean $@	
else
	GRADLE_ARG=$@
	
	if [ "$@" == "rebuild" ]
	then
		GRADLE_ARG="clobber build eclipse"
		echo
        	echo "Running SDI Build for task $@ ($GRADLE_ARG)"
        	echo
		$DIR/tools/gradle-1.4/bin/gradle $GRADLE_ARG
	elif [ "$@" == "buildall" ]
	then
		GRADLE_ARG="clean build eclipse"
		echo
                echo "Running SDI Build for task $@ (build eclipse)"
                echo
		$DIR/tools/gradle-1.4/bin/gradle $GRADLE_ARG
	elif [ "$@" == "full" ]
        then
                GRADLE_ARG="clean build eclipse"
                echo
                echo "Running SDI Build for task $@ (build eclipse runSDITests)"
                echo
                $DIR/tools/gradle-1.4/bin/gradle $GRADLE_ARG
                $DIR/tools/gradle-1.4/bin/gradle clean runSDITests
		$DIR/tools/gradle-1.4/bin/gradle clean build
        elif [ "$@" == "-h" ]
        then
                echo
		      #2345678901234567890123456789012345678901234567890123456789012345678901234567890
                echo "SDI Build Wrapper v0.000154"
                echo
                echo "Wrapper script for more advanced SDI build commands.  Since Gradle cannot"
                echo "guarantee order on dependencies, this wrapper will correctly execute more"
                echo "commonly chained build commands."
                echo
                echo "Normal Gradle commands can also still be used by this wrapper."
                echo
                echo "Usage: ./build.sh [gradle command] or [advanced command] ..."
                echo
                echo "Available Advanced Commands:"
                echo
                echo "    buildall	- executes the gradle build with build and eclipse"
                echo "    rebuild	- executes the gradle build with clobber, build and eclipse"
                echo "    full		- executes the gradle build with build, eclipse and runSDITasks"
                echo
                echo "Example:"
                echo
                echo "	To execute a normal build:"
                echo "		./build.sh build"
                echo
                echo "	To execute an advanced command:"
                echo "		./build.sh rebuild"
                echo
                echo "Options:"
		echo
		echo "    -h	      - this help doc"
		echo
		$DIR/tools/gradle-1.4/bin/gradle
	else
		echo
                echo "Running SDI Build for task $@"
                echo
		$DIR/tools/gradle-1.4/bin/gradle clean $@
	fi
fi

#for (( i=1; i<=$#; i++ )); do
#	eval arg=\$$i
#	GRADLE_ARG="$GRADLE_ARG $arg"
#done

