# CampusHub

CampusHub is a campus C2C second-hand marketplace built as a modular monolith that is intentionally shaped for later extraction into microservices. This repository contains only the architectural skeleton and one thin end-to-end slice so we can verify boundaries, packaging, persistence isolation, and module wiring before filling in business logic.

## Architecture

```text
campushub-bootstrap
        |
        +--> identity-impl --> identity-api --> shared-kernel
        +--> catalog-impl  --> catalog-api  --> shared-kernel
        +--> trading-impl  --> trading-api  --> shared-kernel
        +--> messaging-impl --> messaging-api --> shared-kernel
        +--> media-impl --> media-api --> shared-kernel

catalog-impl --> identity-api
trading-impl --> identity-api + catalog-api
```

## Module Rules

1. `*-api` modules expose only public module interfaces, DTO records, and domain events. They depend only on `shared-kernel`.
2. `*-impl` modules keep services, entities, repositories, controllers, and mappers package-private whenever Spring allows it.
3. `*-impl` modules may depend on other modules' `*-api`, never another module's `*-impl`. Only `campushub-bootstrap` depends on all impl modules.
4. Cross-module sync calls go through `XxxModuleApi`. Async integration uses Spring application events with `@TransactionalEventListener`.
5. Each module owns its own PostgreSQL schema and Flyway migrations. No cross-schema foreign keys and no cross-schema joins.

## Quick Start

```bash
docker-compose up -d
mvn clean install
cd campushub-bootstrap
mvn spring-boot:run
```

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Add A New Module

1. Create `module-<name>` with `<name>-api` and `<name>-impl`.
2. Add both child modules to the parent aggregator and root `<modules>`.
3. Put only interfaces, DTO records, and events in `<name>-api`.
4. Put entities, repositories, services, controllers, mappers, and migrations in `<name>-impl`.
5. Give the module its own PostgreSQL schema and `db/migration/<name>/V1__init.sql`.
6. Add ArchUnit boundary coverage if the new module introduces a new impl package.
7. Wire the impl module into `campushub-bootstrap` only after its boundaries and tests are in place.
# CampusHub
# CampusHub
# CampusHub
# CampusHub
# CampusHub
