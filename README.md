# Databus

NREL works off of this repository for databus development

There is also documentation(and support available) located at http://buffalosw.com/products/databus/databus-documentation/

## Getting Started

```sh
$ git clone git@github.com:NREL/SDI.git
$ cd databus
$ ./build.sh build
$ cd webapp
$ ./play run
```

The app should be running at: [http://localhost:9000/](http://localhost:9000/).    

<br>
## Unit Tests - Gradle

The build must be run at least once in order to acquire needed dependencies.  From main databus directory:
```sh
$ cd ${CLONE_DIR}/databus
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
$ cd ${CLONE_DIR}/databus
$ ./build.sh build
$ cd webapp
$ ./play eclipsify
```

Doing these 2 steps will ensure that:
- Project dependencies have been correctly resolved
- Play's framework has correctly been set up for the Eclipse IDE

To import into Eclipse, just open Import->Existing Projects into Workspace and use the following path as the root directory:
```sh
${CLONE_DIR}/databus/webapp
```
