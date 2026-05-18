# fiserv-dev-studio-mock-server-integration

Spring Boot REST app integrating Postman's mock server with Fiserv Dev Studio.

- Spring Boot **3.5.14**, Java **17**, Maven **3.9.3** (via wrapper)

## Prerequisites

**JDK 17** must be installed. Eclipse Temurin is the recommended distribution:

```sh
brew install --cask temurin@17
```

Set `JAVA_HOME` (add to `~/.zshrc`, then `source ~/.zshrc`):

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

Verify:

```sh
java -version    # openjdk version "17.x.x"
echo $JAVA_HOME  # non-empty
```

Maven is **not** required — the Maven wrapper (`./mvnw`) downloads and pins Maven 3.9.3 on first use.

## Build

```sh
./mvnw clean package
```

Produces `target/mock-server-integration-0.0.1-SNAPSHOT.jar`. Runs unit tests as part of the build.

## Run

Either approach works:

```sh
# Via the Maven wrapper (convenient during development)
./mvnw spring-boot:run

# Or run the packaged jar directly
java -jar target/mock-server-integration-0.0.1-SNAPSHOT.jar
```

The app listens on `http://localhost:8080` (configurable in `application.properties`).

## Verify

```sh
curl localhost:8080/hello
# {"message":"hello from mock-server-integration"}
```

## Stop

`Ctrl+C` in the running terminal, or:

```sh
kill $(lsof -ti tcp:8080)
```

## Configuration

App settings live in `src/main/resources/application.properties`. Override at runtime with flags or env vars, e.g.:

```sh
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=9090
SERVER_PORT=9090 java -jar target/mock-server-integration-0.0.1-SNAPSHOT.jar
```

## Project layout

```
src/main/java/com/postman/fiserv/mockserver/
├── MockServerIntegrationApplication.java   Spring Boot entry point
├── controller/                             REST endpoints
├── model/                                  Request/response/domain types
│   └── persistence/
└── service/                                Business logic
    └── persistence/
```
