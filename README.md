# CampusHub

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=flat&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-FF4438?style=flat&logo=redis&logoColor=white)
[![CI](https://github.com/cqftx001/CampusHub/actions/workflows/ci.yml/badge.svg)](https://github.com/cqftx001/CampusHub/actions/workflows/ci.yml)

CampusHub is the backend for a **full-stack campus marketplace platform**, built with Java and Spring Boot.

The project is designed as a **modular monolith** with clearly separated domain boundaries for authentication, users, and marketplace functionality. It focuses on production-oriented backend concerns such as secure authentication, session lifecycle management, transactional consistency, database migrations, Redis-backed security state, object storage, and automated testing.

> Frontend repository: [CampusHub-Web](https://github.com/cqftx001/CampusHub-Web)

---

## Features

### Authentication & Security

- User registration and login
- BCrypt password hashing
- JWT access-token authentication
- Persisted login sessions
- Single-use rotating refresh tokens
- Refresh-token hashing
- Session revocation
- Email verification
- Password recovery
- Login rate limiting
- Redis-backed authentication and security state

### Marketplace

- Create marketplace listings
- Browse and retrieve listing details
- Search listings by keyword
- Filter by category, brand, condition, and price range
- Paginated marketplace results
- Seller-specific listing management
- Listing status lifecycle management
- Ownership-based authorization for seller operations

### Image Storage

- Multi-image upload for marketplace listings
- External object-storage integration
- Image metadata associated with marketplace listings
- Persistent files decoupled from application instances

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

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Security | Spring Security, JWT, BCrypt |
| Persistence | Spring Data JPA |
| Database | PostgreSQL 16 |
| Cache / Security State | Redis 7 |
| Database Migrations | Flyway |
| Object Storage | Alibaba Cloud OSS / S3-compatible object-storage pattern |
| Build Tool | Maven |
| Testing | JUnit 5 |
| CI | GitHub Actions |

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

## Persistence & Infrastructure

### PostgreSQL

PostgreSQL stores durable domain data such as users, sessions, marketplace listings, and related metadata.

### Flyway

Database schema changes are versioned with Flyway migrations so schema evolution remains reproducible across environments.

### Redis

Redis stores short-lived application and security state where expiration and fast lookup are important, including token-related state, verification data, cooldowns, and rate limits.

### Object Storage

Marketplace images are stored outside the application filesystem. This keeps application instances independent from persistent file storage and supports future horizontal scaling without relying on local disk state.

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

## Testing & CI

The project uses **JUnit 5** for automated tests and **GitHub Actions** for continuous integration.

CI is intended to validate the Maven build and test suite on repository changes before code is treated as integration-ready.

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

## Frontend

CampusHub has a separate React/TypeScript frontend:

[CampusHub-Web](https://github.com/cqftx001/CampusHub-Web)

The frontend provides authentication flows, protected routes, marketplace browsing and filtering, listing creation, image upload, listing details, and seller listing management.

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

## Project Direction

CampusHub is intentionally being developed as a modular monolith first. The current goal is to keep domain ownership and module boundaries clear while avoiding unnecessary distributed-system complexity.

Future modules such as orders, messaging, payments, AI-assisted marketplace search, and recommendations can be introduced behind the same architectural boundaries and extracted into separate services later if operational requirements justify that change.
