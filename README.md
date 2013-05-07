# Databus

This is a fork of NREL's open source databus project (which has also ended up becoming the main development location now as well)

## Getting Started

```sh
$ git clone git@github.com:NREL/SDI.git
$ cd SDI
$ ./build.sh build
$ cd webapp
$ ./play run
```

The app should be running at: [http://localhost:9000/](http://localhost:9000/).    

<br>
## Unit Tests - Gradle

The build must be run at least once in order to acquire needed dependencies.  From main SDI directory:
```sh
$ cd ${CLONE_DIR}/SDI
$ ./build.sh build
```
Once the normal build is run, kick off unit tests with:
```sh
$ ./build.sh runSDITests
```

<br>
## Setting up the Project - Eclipse

In order to use Eclipse for this project, a couple commands must be executed before importing into the IDE.
```sh
$ cd ${CLONE_DIR}/SDI
$ ./build.sh build
$ cd webapp
$ ./play eclipsify
```

Doing these 2 steps will ensure that:
- Project dependencies have been correctly resolved
- Play's framework has correctly been set up for the Eclipse IDE

To import into Eclipse, just open Import->Existing Projects into Workspace and use the following path as the root directory:
```sh
${CLONE_DIR}/SDI/webapp
```