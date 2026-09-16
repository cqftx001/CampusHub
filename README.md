# CampusHub

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=flat&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat&logo=springsecurity&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-FF4438?style=flat&logo=redis&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?style=flat&logo=flyway&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=flat&logo=apachemaven&logoColor=white)
![JUnit](https://img.shields.io/badge/JUnit-5-25A162?style=flat&logo=junit5&logoColor=white)
![OSS](https://img.shields.io/badge/Cloud-Object_Storage-blue?style=flat)
[![CI](https://github.com/cqftx001/CampusHub/actions/workflows/ci.yml/badge.svg)](https://github.com/cqftx001/CampusHub/actions/workflows/ci.yml)

CampusHub is the backend for a **full-stack campus marketplace platform**, built with Java and Spring Boot.

The project is designed as a **modular monolith** with clearly separated domain boundaries for authentication, users, and marketplace functionality. It focuses on production-oriented backend concerns such as secure authentication, session lifecycle management, transactional consistency, database migrations, Redis-backed security state, object storage, and automated testing.

> Frontend repository: [CampusHub-Web](https://github.com/cqftx001/CampusHub-Web)

---

## API Overview

### Authentication

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/logout
GET  /api/auth/me
POST /api/auth/email-verification/confirm
```

### Marketplace

```text
GET   /api/marketplace/catalog
GET   /api/marketplace/listings
GET   /api/marketplace/listings/{listingId}
GET   /api/marketplace/listings/mine
POST  /api/marketplace/listings
PATCH /api/marketplace/listings/{listingId}/status
POST  /api/marketplace/images
```

The frontend communicates with these APIs through a typed React/TypeScript API layer in [CampusHub-Web](https://github.com/cqftx001/CampusHub-Web).

---

## Architecture

```text
                         CampusHub
                      Spring Boot API
                            │
          ┌─────────────────┼─────────────────┐
          │                 │                 │
        Auth               User          Marketplace
          │                 │                 │
          └─────────────────┼─────────────────┘
                            │
                      Shared Kernel
                            │
          ┌─────────────────┼─────────────────┐
          │                 │                 │
     PostgreSQL           Redis         Object Storage
          │                                   │
       Flyway                              OSS / S3

        Future domain boundaries:
        order · messaging · payment · assistant
```

The application currently follows a modular-monolith architecture rather than prematurely splitting domains into independent services. The module boundaries are designed so that domains can evolve independently and can be extracted later if scaling or deployment requirements justify it.

---

## Engineering Highlights

- **Secure session lifecycle** — login sessions are persisted, refresh tokens are single-use and rotated after use, and token values are stored as hashes instead of plaintext.
- **Redis-backed ephemeral state** — Redis is used for short-lived security state such as active JWT tracking, verification tokens, cooldowns, and rate limiting.
- **Modular architecture** — Maven multi-module design keeps feature boundaries explicit, with `campushub-app` acting as the composition root.
- **Module-owned persistence** — feature modules own their persistence logic and versioned Flyway migrations, reducing cross-module coupling.
- **Stateless application design** — marketplace images are stored outside application instances through object storage, making persistent files accessible independently of any single backend instance.
- **Transactional service boundaries** — state-changing operations are handled within explicit service-layer transaction boundaries.
- **Automated verification** — JUnit 5 tests and GitHub Actions CI are used to validate application behavior and builds.

---

## Module Structure

```text
CampusHub/
├── campushub-app/      # Application entry point and composition root
├── auth/               # Authentication, sessions, JWT, verification
├── user/               # User domain
├── marketplace/        # Listings, search, seller operations, images
└── shared-kernel/      # Shared abstractions and cross-module types
```

---

## Roadmap

- [x] Authentication and session management
- [x] Email verification and password recovery
- [x] Redis-backed security state
- [x] Marketplace listing APIs
- [x] Search, filtering, and pagination
- [x] Seller listing management
- [x] Marketplace image storage
- [ ] Listing update and deletion
- [ ] Campus/location-based marketplace discovery
- [ ] Order lifecycle
- [ ] Real-time messaging
- [ ] Payment integration
- [ ] AI marketplace assistant
- [ ] Personalized recommendation system

---

## Security Design

CampusHub separates short-lived access-token authentication from persisted session state.

Key design choices include:

- Passwords are hashed with BCrypt.
- JWT access tokens are used for authenticated API requests.
- Refresh tokens are rotated after use.
- Refresh-token values are stored as hashes rather than plaintext.
- Sessions can be revoked independently.
- Redis is used for time-sensitive security state and rate limiting.
- Seller operations are protected by authentication and ownership checks.

---

## Getting Started

### Prerequisites

- Java 21
- Maven
- PostgreSQL 16+
- Redis 7+
- Object-storage credentials when marketplace image storage is enabled

### Build

```bash
mvn clean verify
```

### Run

```bash
mvn spring-boot:run -pl campushub-app
```

Environment-specific database, Redis, JWT, email, and object-storage settings should be configured outside source control.

---
