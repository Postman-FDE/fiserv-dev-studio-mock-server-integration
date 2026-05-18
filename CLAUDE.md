# Project context

Spring Boot REST service that integrates Postman's mock server with Fiserv Dev Studio. The same repo holds the Spring Boot app **and** a Postman workspace (collections, environments, etc.) so the two are versioned together.

## Stack

- Java **17** (Eclipse Temurin), Spring Boot **3.5.14**, Maven **3.9.3** via wrapper (`./mvnw`)
- Base package: `com.postman.fiserv.mockserver`
- Build: `./mvnw clean package` &nbsp;·&nbsp; Run: `./mvnw spring-boot:run` &nbsp;·&nbsp; Default port: `8080`
- See `README.md` for full setup/run instructions

## Postman workspace — File System Mode

**Critical:** This repo contains `.postman/resources.yaml`, which means the Postman workspace runs in **File System Mode**. Collections, environments, etc. under `postman/` are **directory trees of YAML files** that sync bidirectionally with the Postman UI.

**Do NOT** write Postman Collection v2.1.0 single-JSON files. They will not sync.

**Authoritative format reference:** [`.claude/skills/api-designer/postman-file-system-reference-http.md`](.claude/skills/api-designer/postman-file-system-reference-http.md) — read this before creating or editing any file under `postman/`. Covers directory layout, `$kind` values, YAML quoting rules (`{{variables}}` must be single-quoted, multi-line bodies use `|-`, `order` is a bare number, etc.), and the request/example/environment/definition file formats.

For non-HTTP protocols (gRPC, GraphQL, WebSocket, MQTT, etc.), fetch the extended reference from the source skill at `/Users/shyam.bahety@postman.com/Postman/guardian-life/api-designer-validator-skill/.claude/skills/api-designer/postman-file-system-reference-other-protocols.md`.

## Layout

```
src/main/java/com/postman/fiserv/mockserver/   Spring Boot source
  controller/                                  REST endpoints
  model/  model/persistence/                   Domain + persistence DTOs
  service/  service/persistence/               Business logic
postman/                                       File System Mode workspace
  collections/<Collection Name>/               One folder per collection
    .resources/definition.yaml                 Collection metadata
    <Request>.request.yaml                     One file per request
    .resources/<Request>.resources/examples/   Saved example responses
  environments/<Name>.environment.yaml         One file per environment
  globals/workspace.globals.yaml               Workspace-level globals
.postman/resources.yaml                        File System Mode manifest
```

## Handoff context

The Spring Boot code (`controller/`, `model/`, `service/`) will eventually be lifted into a new service owned by the Fiserv team. Favor stock Spring conventions over local cleverness so the code is easy to extract.
