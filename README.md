# CampusHub

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=flat&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-FF4438?style=flat&logo=redis&logoColor=white)
[![CI](https://github.com/cqftx001/CampusHub/actions/workflows/ci.yml/badge.svg)](https://github.com/cqftx001/CampusHub/actions/workflows/ci.yml)

CampusHub is a **modular-monolith backend for a campus marketplace**, built
with Java and Spring Boot.

The project focuses on production-oriented backend concerns such as
authentication, session security, transactional boundaries, database
migrations, module isolation, and testable service design.

---

## Engineering Highlights

- **Secure authentication** — BCrypt credentials, JWT access tokens, email
  verification, login rate limiting, and password recovery.
- **Session lifecycle** — persisted login sessions, single-use rotating refresh
  tokens, token hashing, and session revocation.
- **Redis-backed security state** — active JWT tracking, verification tokens,
  cooldowns, and login rate limiting.
- **Modular architecture** — Maven multi-module design with `campushub-app`
  acting as the composition root and feature modules kept isolated by default.
- **Database ownership** — PostgreSQL schemas and versioned Flyway migrations
  owned by individual feature modules.
- **Automated verification** — JUnit 5 tests and GitHub Actions CI.

---

## Architecture

```text
                    campushub-app
                         │
        ┌────────────────┼────────────────┐
        │                │                │
       auth             user         marketplace
        │                │                │
        └────────────────┼────────────────┘
                         │
                   shared-kernel

        Future module boundaries:
        order · messaging · payment · assistant
