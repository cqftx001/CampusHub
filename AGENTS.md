# CampusHub Collaboration Rules


## Coding Standards

- Use Java 21.
- Prefer constructor injection.
- Use Lombok only where it improves readability.
- Add comments only for non-obvious logic.
- Do not introduce a new dependency without explaining why.
- Follow existing naming, package, DTO, exception, and persistence conventions
  before introducing new patterns.
- Do not modify any files yet. Exception for api.md / README.md.
  Inspect the existing implementation and propose a plan only.

## Architecture

- Build CampusHub as a modular monolith first while preserving boundaries that
  could later be extracted into services.
- `campushub-app` is the composition root.
- `shared-kernel` contains only shared framework templates and stable
  cross-module contracts.
- Never place a feature module's business error codes in `shared-kernel`.
- A feature module must not depend on another feature module unless an explicit
  use case and contract require that dependency.
- Do not create `api` / `impl` child modules speculatively.
- Keep business logic out of controllers.
- Prefer the smallest complete vertical slice over prebuilding future features.

## Business Work

- Framework code and module structure may be edited when required by the task.
- Implement business behavior only when the current task explicitly authorizes
  that behavior.
- Before implementing an endpoint, identify:
  - request DTO
  - response VO
  - authentication and authorization requirements
  - expected status codes
  - business error cases
  - idempotency requirements, if applicable
- If these requirements are already defined in the task, proceed without asking
  for confirmation.
- If a materially important requirement is ambiguous, call it out before
  implementation.
- Each feature module owns its business `ErrorCode` enum and exception type.
- Do not prebuild functionality for future requirements.

## Marketplace Delivery Priorities

- Treat Marketplace as a resume-project feature whose priority is reaching a
  small, coherent, end-to-end deliverable quickly.
- Prefer the simplest implementation that satisfies the agreed Marketplace V1
  behavior. Avoid unnecessary infrastructure, generic frameworks, and future-
  proofing that delays delivery.
- Before increasing the Marketplace's technical depth or architectural
  complexity, explain the benefit, implementation cost, and delivery impact,
  then discuss the choice with the user. Implement it only after agreement.
- Required correctness, authentication, authorization, validation, transaction,
  and data-integrity safeguards are not considered optional over-engineering.

## API Documentation

- Maintain `/api.md` whenever an endpoint is added or changed.
- Document:
  - HTTP method
  - path
  - authentication requirements
  - request
  - response
  - success status code
  - error cases

## Safety Rules

- Never silently change the database schema.
- Never remove or change existing API behavior without calling it out.
- Do not make breaking architectural changes without explicit approval.
- Do not modify unrelated files.
- Do not delete existing tests merely to make the test suite pass.
- Do not weaken validation, authentication, authorization, or security checks
  merely to satisfy a failing test.

## Before Coding

1. Inspect relevant existing classes and tests.
2. Read the applicable `AGENTS.md` instructions.
3. Identify the existing architectural and coding patterns.
4. Explain the implementation plan before making substantial changes.
5. Identify relevant race conditions, transaction boundaries, idempotency
   concerns, and security implications.
6. Prefer reusing existing abstractions over creating parallel implementations.

## Testing
Develop the tests only after a module is completed and confirmed ready for delivery.
Before completing a task:

1. Add or update unit tests for changed business logic.
2. Add integration tests when behavior crosses persistence, transaction,
   security, or module boundaries.
3. Add concurrent-request tests when the feature has meaningful race-condition
   or idempotency risk.
4. Run relevant unit tests.
5. Run affected-module integration tests.
6. Check compilation and formatting.
7. Report any failing tests instead of hiding or bypassing them.

## After Coding

1. Review the implementation for correctness and unnecessary complexity.
2. Summarize the files changed.
3. Explain important design decisions.
4. Report tests executed and their results.
5. Call out any remaining risks, assumptions, or follow-up work.
