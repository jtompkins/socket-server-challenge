# coding-challenge

Coding Challenge Build Framework

## Starter build framework for the coding challenge

First, you do not need to use this starter framework for your project.
If you would rather use a different build system (maven, javac, ...)
you are free to so long as you provide clear commands to build your
project and start your server.  Failure to do so will invalidate your
submission.


## Install Java

This coding challenge is in Java so it is recommended you install Java
1.8 from Oracle.


## Gradle

The build framework provided here uses gradle to build your project
and manage your dependencies.  The `gradlew` command used here will
automatically download gradle for you so you shouldn't need to install
anything other than java.


### Project Layout

All source code should be located in the `src/main/java` folder.
If you wish to write any tests (not a requirement) they should be
located in the `src/test/java` folder.

A starter `Main.java` file has been provided in the `com/newrelic/codingchallenge` package under `src/main/java`.


### Dependencies

If your project has any dependencies you can list them in the
`build.gradle` file in the `dependencies` section.


### Building your project from the command line

To build the project on Linux or MacOS run the command `./gradlew build` in a shell terminal.  This will build the source code in
`src/main/java`, run any tests in `src/test/java` and create an output
jar file in the `build/libs` folder.

To clean out any intermediate files run `./gradlew clean`.  This will
remove all files in the `build` folder.


### Running your application from the command line

You first must create a shadow jar file.  This is a file which contains your project code and all dependencies in a single jar file.  To build a shadow jar from your project run `./gradlew shadowJar`.  This will create a `codeing-challenge-shadow.jar` file in the `build/libs` directory.

You can then start your application by running the command
`java -jar ./build/lib/coding-challenge-shadow.jar`

## IDEA

You are free to use whichever editor or IDE you want providing your
projects build does not depend on that IDE.  Most of the Java
developers at New Relic use IDEA from
[JetBrains](https://www.jetbrains.com/).  JetBrains provides
a community edition of IDEA which you can download and use without
charge.

If you are planning to use IDEA you can generate the IDEA project files
by running `./gradlew idea` and directly opening the project folder
as a project in idea.

