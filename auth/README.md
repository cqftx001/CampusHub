# Auth Module

The Auth Module owns credentials, registration, login sessions, access-token
issuance and verification, refresh-token rotation, and logout revocation.

## Current Scope

- Register with `username`, `email`, and `password`.
- Login through one `identifier` field containing either email or username.
- Normalize email and username to lowercase before storage and lookup.
- Enforce unique email and username in both the service and database.
- Hash passwords with BCrypt.
- Send an email-verification link after registration commits.
- Store expiring verification-token digests and resend cooldowns in Redis.
- Enable an account after idempotent email confirmation.
- Return the same resend response for unknown, verified, and rate-limited
  addresses to avoid account enumeration.
- Issue 15-minute JWT access tokens containing account ID, session ID, JWT ID,
  and roles.
- Create an independent persisted login session for every successful login.
- Persist only hashed, rotating refresh tokens in PostgreSQL.
- Allow each refresh token to be used once and return its replacement.
- Keep refresh-token chains valid only until the original session expiration;
  rotation does not extend the absolute session lifetime.
- Store each login session's current JWT ID in Redis without storing raw JWTs.
- Reject correctly signed JWTs that are no longer current for their session.
- Atomically rotate the current Redis JWT ID during refresh.
- Keep different login sessions for the same account independent.
- Revoke the persisted session and active refresh token during logout.
- Authenticate `/api/auth/me` and `/api/auth/logout` with a Bearer token.

Usernames deliberately cannot contain `@`, which keeps the email and username
identifier namespaces unambiguous.

## Deferred

- Immediate Redis access-token invalidation during logout
- Account recovery
- OAuth providers
- Profile, address, and phone data (owned by the User Module)

See `/api.md` for the HTTP contracts.
