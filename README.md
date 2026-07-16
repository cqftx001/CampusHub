# CampusHub

CampusHub is a modular monolith being developed one vertical slice at a time.
The Auth Module is the first implemented slice; all other feature modules remain
empty boundaries.

## Project Structure

```text
CampusHub
├── campushub-app   # Spring Boot entry point and framework assembly
├── shared-kernel   # Shared response/error/request templates only
├── auth            # Authentication boundary
├── user            # User account boundary
├── marketplace     # Listing, product, and inventory boundary
├── order           # Order boundary
├── messaging       # Messaging boundary
├── payment         # Payment boundary
└── assistant       # Conversational assistant boundary
```

All feature modules are single Maven modules for now. An `api` / `impl` split is
introduced only when a real cross-module contract needs that separation.

```text
                 campushub-app
                       |
       +---------------+---------------+
       |       |       |       |       |
      auth    user  marketplace order  ...
       |       |       |       |       |
       +---------------+---------------+
                       |
                 shared-kernel
```

Feature modules must not depend on one another directly by default. A dependency
is added only after its use case and public contract are agreed. The application
module is the composition root and may depend on every feature module.

## Current Technology Baseline

- Java 21
- Spring Boot 3.x and Spring MVC
- Spring Security with stateless JWT access tokens
- Spring Data JPA and Flyway
- Maven multi-module build
- PostgreSQL and Redis local containers
- JUnit 5 through Spring Boot's test starter
- GitHub Actions verification

OpenAPI, Testcontainers, model adapters, and the React frontend remain planned
technologies. They will be introduced with the first behavior that genuinely
needs them, rather than as unused framework code.

## Development Rules

1. Framework and module-structure changes may be written directly.
2. Business behavior is designed and reviewed one feature at a time.
3. Each feature module owns its own error code enum and exception when business
   behavior is added. `shared-kernel` provides only the templates.
4. Agree request DTOs and response VOs before implementing an endpoint.
5. Update [api.md](api.md) whenever an endpoint contract is approved.
6. Do not add speculative layers, dependencies, tables, or integrations.

## Commands

```bash
docker compose up -d
mvn verify
docker compose config
java -jar campushub-app/target/campushub-app-0.1.0-SNAPSHOT.jar
```

The application starts on <http://localhost:8080>. Auth contracts are documented
in [api.md](api.md).
