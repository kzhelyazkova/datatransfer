# Data transfer between external systems

This application provides a command line interface for transferring data between two external systems.
Currently supported external systems include GitHub (only as a source) and Freshdesk (only as a destination).
The app only supports transferring data for a specific user currently.

## Prerequisites

In order to build and run this project, you need:
- Java 17+
- Maven 

## Building the project

The project uses Maven so in order to build it, go to the project's root directory and execute the following command:
```
mvn clean package -DskipTests
```
This builds a JAR for you and saves it in the target directory.

## Running the application

To run the application, use the `java` command followed by the path to the JAR file built on the previous step:
```
java -jar ./target/datatransfer-0.0.1-SNAPSHOT.jar
```
This starts the Spring Shell application and you should be able to see:
```
shell:>
```
in a few seconds and start executing commands.

## CLI

Spring Shell comes with some built-in commands, including `help` so you can start by calling `help` to see a list
of all available commands. There is also support for autocompletion.

The commands provided by this application also have a `--help` option which gives you a description of the command and
its options.

Example usage of the data transfer command for transferring user data for a GitHub user with username 'wayneeseguin'
to a Freshdesk domain 'bluesky':
```
transfer user --source-system github --source-params username=wayneeseguin --destination-system freshdesk --destination-params domain=bluesky
```

Use the built-in `quit` command to quit the application.

## Running the tests

To run all of the project's tests, execute:
```
mvn test
```

To run a single test class, for example GitHubGatewayServiceTest:
```
mvn test -Dtest=GitHubGatewayServiceTest
```

To run a single test method, for example testDownloadUserData_success in GitHubGatewayServiceTest:
```
mvn test -Dtest=GitHubGatewayServiceTest#testDownloadUserData_success
```

## Application logs

You can find the application logs in the 'logs' directory.
The log file is rotated daily and a maximum of 30 days of logs is preserved.