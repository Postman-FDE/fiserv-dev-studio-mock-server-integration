# Postman File System Reference — HTTP Protocol

This workspace runs in **File System Mode** — files in the `postman/` folder sync bidirectionally with the Postman UI. Every file you create or modify MUST follow the exact structure below, or it will break Postman's parsing.

**Do NOT add pre-request or test scripts to `Use case - *`, `Reference - *`, or any other design-time collection — not unless the user explicitly asks.** Test scripts ARE expected in `Use case tests - *` collections: those are authored by the `validator` skill (see `.claude/skills/validator/SKILL.md`), not by the api-designer skill. Do not hand-write or modify scripts in `Use case tests - *` collections — let the validator regenerate them.

**Use realistic data.** Example request/response bodies should use domain-appropriate data that tells a story, not generic placeholders.

## Directory Structure

```
postman/
  collections/                           # All collections
    <collection-name>/                   # Each collection = a folder
      .resources/                        # Optional metadata directory
        definition.yaml                  # Collection metadata (variables, auth, scripts)
        <request-name>.resources/
          examples/
            <example-name>.example.yaml  # Request examples
      <request-name>.request.yaml        # Individual requests
      <subfolder-name>/                  # Postman folders (nested grouping)
        .resources/
          definition.yaml                # Folder metadata
        <request-name>.request.yaml
  environments/                          # All environments
    <EnvironmentName>.environment.yaml
  specs/                                 # OpenAPI specifications
    <API Name>/
      index.yaml
  globals/
    workspace.globals.yaml               # Workspace-level global variables
  flows/                                 # Postman Flows (visual use case diagrams)
    <flow-name>.flow                     # Flow definition (JSON)
    flow-resources.json                  # Index of all flows (maps UUID → path)
```

## YAML Rules (CRITICAL — invalid YAML breaks Postman)

1. **Single-quote values with `{{variables}}`:** `url: '{{base_url}}/users'` — NEVER leave unquoted
2. **Single-quote values containing special characters:** `: # & * ! [ ] { } > |` — e.g., `name: 'Health check: v2'`
3. **Multi-line content MUST use `|-` block scalar:**
   ```yaml
   body:
     type: json
     content: |-
       {
         "name": "example"
       }
   ```
4. **Quote strings resembling booleans/numbers** when intended as strings: `value: "true"`, `value: "123"`
5. **`order` field MUST be a bare number**, never quoted: `order: 1000`
6. **Single-quote file paths, forward slashes only:** `examples: './.resources/name.resources/examples'`

## Naming Rules

- `<request-name>` (filename stem before `.request.yaml`) must NOT contain: `/ \ : * ? " < > |` — sanitize unsafe characters to `-`
- Include the `name` field in the YAML ONLY when it differs from `<request-name>` (e.g., `name: 'Health/check'` in file `Health-check.request.yaml`)
- Files must be unique (case-insensitive) per directory
- Never place request files inside `.resources/` directories

## Order Field Convention

The `order` field controls display position in the Postman UI. Use values ~1000 apart for easy insertion:
```
order: 1000   # First item
order: 2000   # Second item
order: 3000   # Third item
```

## Variable Substitution

Use `{{variableName}}` (double curly braces) for variable references in any string field — URLs, headers, body content, auth credentials. Always single-quote values containing variables.

**Variable naming:** all variables — both `{{variables}}` and `:pathVariables` — must follow the same naming convention. Use the convention defined in `api-guidelines.md` (at the repo root) and enforced by governance linting. If no convention is defined, derive from what has been validated with the user during the design process. Be consistent throughout.

**Variable scopes (narrowest to broadest):** Request-level > Collection-level (definition.yaml) > Environment-level > Global-level

## Path Variables

URLs with dynamic segments MUST use `:paramName` syntax — **never hardcode values in the URL path**. Always define a corresponding `pathVariables` block with the value. This applies to both request files and example files. In request files, use `{{variables}}` for path variable values. In example files, use concrete values to illustrate specific scenarios.

**Request file:**
```yaml
url: '{{base_url}}/orders/:order_id/items/:item_id'
pathVariables:
  - key: order_id
    value: 'ORD-2024-048271'
    description: 'Order identifier'
  - key: item_id
    value: 'ITM-2024-081104'
    description: 'Item identifier'
```

**Example file:**
```yaml
request:
  url: '{{base_url}}/orders/:order_id/items/:item_id'
  method: GET
  pathVariables:
    - key: order_id
      value: 'ORD-2024-048271'
    - key: item_id
      value: 'ITM-2024-081104'
```

## Query Parameters

Always define query parameters in the `queryParams` block. This applies to both request files and example files.

- **Enabled** (default): the query parameter must appear in BOTH the URL string and the `queryParams` block
- **Disabled** (`disabled: true`): the query parameter appears ONLY in the `queryParams` block, NOT in the URL

In request files, use `{{variables}}` for values. In example files, use concrete values to illustrate specific scenarios.

**Request-level contract:** all query parameters defined in the OpenAPI spec for an operation must be present in the `queryParams` block of the corresponding request — in both the reference collection and use case collections. Required parameters are enabled; optional parameters are `disabled: true`. This makes the full interface visible on every request.

**Example-level scenarios:** examples must include ALL query parameters from the parent request. Parameters used in that scenario are enabled with concrete values; unused parameters remain `disabled: true`. This makes every example self-documenting — the full interface is visible, with the active parameters highlighted.

**Request file (required param enabled):**
```yaml
url: '{{base_url}}/resources?status={{status}}'
method: GET
queryParams:
  - key: status
    value: '{{status}}'
    description: 'Filter by order status'
```

**Request file (optional param disabled):**
```yaml
url: '{{base_url}}/resources'
method: GET
queryParams:
  - key: status
    value: '{{status}}'
    disabled: true
    description: 'Filter by order status'
```

**Example file (all params present, active ones enabled):**
```yaml
request:
  url: '{{base_url}}/resources?status=denied'
  method: GET
  queryParams:
    - key: status
      value: 'denied'
    - key: page
      value: '1'
      disabled: true
```

## Headers

Same rules as query parameters — except headers do not appear in the URL. All headers defined in the OpenAPI spec for an operation must be present in the `headers` block of the corresponding request in both collection types. Optional headers are `disabled: true`. Examples must include ALL headers from the parent request — active ones enabled with concrete values, unused ones `disabled: true`. In request files, use `{{variables}}` for header values. In example files, use concrete values.

**Request file:**
```yaml
headers:
  - key: Accept
    value: application/json
  - key: Request-Id
    value: '{{request_id}}'
    disabled: true
    description: 'Optional correlation ID for tracing'
```

## Collection Definition File

Path: `<collection-or-folder>/.resources/definition.yaml` (optional)

```yaml
$kind: collection                          # Required — use "collection" for BOTH collections and folders
name: 'My API'                             # Optional — defaults to filesystem folder name
description: 'Description of the API'      # Optional
variables:                                 # Optional — collection-level variables
  - key: base_url
    value: 'https://api.example.com/v1'
    description: 'Base URL'                # Optional
    disabled: false                        # Optional
auth:                                      # Optional
  type: bearer
  credentials:
    - key: token
      value: '{{auth_token}}'
scripts:                                   # Optional — only add if user explicitly asks
  - type: 'http:beforeRequest'
    code: 'console.log("pre-request")'
    language: 'text/javascript'
order: 1000                                # Optional — for folder ordering
```

## HTTP Request

File: `<collection>/<request-name>.request.yaml`

```yaml
$kind: http-request                         # Required
name: 'Display name'                        # Optional — only if different from filename
order: 2000                                 # Optional — bare number, ~1000 apart
method: GET                                 # GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS
url: '{{base_url}}/resources/:id'           # Single-quote if contains {{variables}}
headers:                                    # Optional
  - key: Content-Type
    value: application/json
    description: 'Content type'             # Optional
    disabled: false                         # Optional
queryParams:                                # Optional
  - key: page
    value: '1'
    description: 'Page number'
    disabled: false
pathVariables:                              # Optional
  - key: id
    value: '1'
    description: 'Resource ID'
body:                                       # Optional
  type: json                                # json|formdata|urlencoded|text|xml|html|javascript|file|none
  content: |-                               # Use |- for multi-line
    {
      "title": "Example",
      "author": "Jane Doe"
    }
auth:                                       # Optional
  type: bearer
  credentials:
    - key: token
      value: '{{auth_token}}'
scripts:                                    # Optional — only add if user explicitly asks
  - type: beforeRequest
    code: 'console.log("running")'
    language: 'text/javascript'
  - type: afterResponse
    code: |-
      pm.test("Status is 200", function() {
        pm.response.to.have.status(200);
      });
    language: 'text/javascript'
examples: './.resources/<request-name>.resources/examples'  # Optional
```

**Body type details:**
- `json`, `text`, `xml`, `html`, `javascript`: content is a string (use `|-` for multi-line)
- `formdata`: content is array of `{key, type: "text"|"file", value or src, contentType?, description?}`
- `urlencoded`: content is array of `{key, value, description?}`

## HTTP Example

File: `.resources/<request-name>.resources/examples/<example-name>.example.yaml`

```yaml
$kind: http-example                         # Required
name: '200 OK'                              # Optional
request:
  url: '{{base_url}}/resources/1'
  method: GET
response:
  statusCode: 200
  statusText: OK
  headers:
    - key: Content-Type
      value: application/json
  body:
    type: json
    content: |-
      {
        "id": 1,
        "title": "Example"
      }
order: 1000                                 # Optional
```

## Environment

File: `postman/environments/<EnvironmentName>.environment.yaml`

```yaml
name: Development
values:
  - key: base_url
    value: 'https://dev.api.example.com'
    enabled: true
    type: default
  - key: api_key
    value: 'dev-key-12345'
    enabled: true
    type: secret
```

Rules: `value` must always be a string. `enabled` is boolean. `type` is string (`"default"` or `"secret"`).

## OpenAPI Specification

Specs live in `postman/specs/<API Name>/index.yaml`. Write standard OpenAPI 3.x YAML.

## Auth Types

Auth can be defined at collection, folder, or request level.

| Type | Credential Keys |
|------|----------------|
| `bearer` | `token` |
| `basic` | `username`, `password` |
| `apikey` | `key`, `value`, `in` (`"header"` or `"query"`) |
| `oauth2` | Various keys depending on grant type |
| `digest` | Protocol-specific |
| `hawk` | Protocol-specific |
| `ntlm` | Protocol-specific |
| `aws` | Protocol-specific |

**Multi-auth** (at collection/folder level in `definition.yaml`):
```yaml
auth:
  - id: 'auth-1'
    name: 'API Key Auth'
    type: apikey
    credentials:
      - key: key
        value: 'X-API-Key'
      - key: value
        value: '{{api_key}}'
      - key: in
        value: header
    rules: []                               # Optional
```

## Other Request Types

For non-HTTP protocols (GraphQL, gRPC, WebSocket, Socket.IO, MQTT, MCP, LLM), read the extended reference at `.claude/skills/api-designer/postman-file-system-reference-other-protocols.md`.

## Postman Flows (Visual Use Case Diagrams)

Flows are visual, canvas-based workflow diagrams that sync to the Postman UI. They provide a business-readable view of use cases alongside the technical collections. Flow files are JSON (`.flow` extension) in `postman/flows/`. Read the format reference at `.claude/skills/api-designer/postman-flows-reference.md` before creating or modifying flows.
