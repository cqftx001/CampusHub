# CampusHub API

## Response Envelope

All API endpoints use the shared response envelope:

```json
{
  "code": "0000",
  "message": "Success",
  "data": {},
  "timestamp": "2026-07-15 15:00:00",
  "requestId": "uuid"
}
```

## Auth

### Register

- Method: `POST`
- Path: `/api/auth/register`
- Authentication: none
- Success status: `201 Created`

Request:

```json
{
  "username": "campus.user",
  "email": "campus.user@example.com",
  "password": "password123"
}
```

Rules:

- `username`: 3-32 characters; letters, numbers, `.`, `_`, and `-` only.
- `email`: valid email, at most 254 characters.
- `password`: 8-72 characters and at most 72 UTF-8 bytes (BCrypt limit).
- Username and email are normalized to lowercase.
- Username and email are each unique.

Response data:

```json
{
  "accountId": "uuid",
  "username": "campus.user",
  "email": "campus.user@example.com",
  "emailVerificationRequired": true
}
```

Registration commits the account before the verification email is sent. A mail
delivery failure does not roll back registration; the account remains disabled
until its email address is verified.

Errors:

- `400 COMMON_1001`: request validation failed.
- `409 AUTH_1001`: email already registered.
- `409 AUTH_1002`: username already taken.
- `409 AUTH_1003`: database uniqueness conflict during a concurrent request.

### Login

- Method: `POST`
- Path: `/api/auth/login`
- Authentication: none
- Success status: `200 OK`

Request:

```json
{
  "identifier": "campus.user@example.com",
  "password": "password123"
}
```

`identifier` accepts either the normalized email or username. Login does not
embed profile data; that data is retrieved from the separate current-user API.

Response data:

```json
{
  "accessToken": "jwt",
  "refreshToken": "opaque-refresh-token",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "refreshTokenExpiresAt": "2026-09-27T12:00:00Z"
}
```

Each successful login creates an independent login session. Multiple login
sessions for the same account remain valid independently. Redis stores the
current JWT ID (`jti`) for each session; it never stores the raw JWT.

Errors:

- `400 COMMON_1001`: request validation failed.
- `401 AUTH_1004`: identifier or password is incorrect.
- `403 AUTH_1008`: credentials are valid, but email verification is required.
- `403 AUTH_1014`: the account is disabled.
- `503 AUTH_1016`: the online authentication-session registry is unavailable;
  no token pair is issued.

### Refresh Token

- Method: `POST`
- Path: `/api/auth/token/refresh`
- Authentication: none
- Success status: `200 OK`
- Idempotency: not idempotent; each refresh token can be used once.

Request:

```json
{
  "refreshToken": "opaque-refresh-token"
}
```

Response data:

```json
{
  "accessToken": "new-jwt",
  "refreshToken": "new-opaque-refresh-token",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "refreshTokenExpiresAt": "2026-09-27T12:00:00Z"
}
```

A successful refresh atomically replaces the login session's current JWT ID in
Redis. The previous access token for that same session is no longer current.
It also marks the supplied refresh token as `USED` and returns a new active
refresh token. The returned refresh token can be used for the next rotation, so
the chain can continue until the original login session expires. Refresh does
not extend that absolute session expiration time.

Different login sessions for the same account are independent; refreshing one
session does not invalidate access tokens issued to another session.

Errors:

- `400 COMMON_1001`: request validation failed.
- `401 AUTH_1015`: the refresh token is unknown, expired, already used, its
  session is inactive, or its online Redis session is missing.
- `403 AUTH_1014`: the account is disabled.
- `503 AUTH_1016`: the online authentication-session registry is unavailable.

### Confirm Email Verification

- Method: `POST`
- Path: `/api/auth/email-verification/confirm`
- Authentication: none
- Success status: `200 OK`

Request:

```json
{
  "token": "email-verification-token"
}
```

The success envelope contains `null` data. Confirmation enables the Auth
account; profile data is retrieved through the separate current-user/profile
API. Reusing the same current token is idempotent and returns `200 OK` while the
token is retained. An expired token or a token superseded by resend is invalid.

Errors:

- `400 COMMON_1001`: request validation failed.
- `400 AUTH_1006`: token is invalid, expired, or superseded.
- `503 AUTH_1007`: the verification token store is temporarily unavailable.

### Resend Email Verification

- Method: `POST`
- Path: `/api/auth/email-verification/resend`
- Authentication: none
- Success status: `202 Accepted`

Request:

```json
{
  "email": "campus.user@example.com"
}
```

The success envelope contains `null` data. To prevent account enumeration, the
endpoint also returns `202 Accepted` when the email is unknown, the account is
already verified, or the request is suppressed by the resend cooldown. A new
message supersedes the account's previous verification token.

Errors:

- `400 COMMON_1001`: request validation failed.
- `503 AUTH_1007`: the verification token store is temporarily unavailable.

### Current User

- Method: `GET`
- Path: `/api/auth/me`
- Authentication: `Authorization: Bearer <access-token>`
- Success status: `200 OK`

Response data:

```json
{
  "accountId": "uuid",
  "username": "campus.user",
  "email": "campus.user@example.com",
  "phoneNumber": null,
  "emailVerified": true,
  "phoneVerified": false
}
```

Errors:

- `401 AUTH_1005`: the JWT is missing, malformed, expired, has an invalid
  signature, or is no longer current for its login session.
- `403 AUTH_1014`: the account is disabled.
- `503 AUTH_1016`: the online authentication-session registry is unavailable.

### Logout

- Method: `POST`
- Path: `/api/auth/logout`
- Authentication: `Authorization: Bearer <access-token>`
- Success status: `200 OK`

Logout revokes the persisted login session and its active refresh token. The
operation uses the same session-first lock order as refresh. Redis removal of
the current access-token registration is not yet wired into this slice, so
immediate access-token invalidation on logout remains follow-up work.

Errors:

- `401 AUTH_1005`: the token is missing, invalid, expired, or is not current for
  its login session.
- `503 AUTH_1016`: the online authentication-session registry is unavailable.

## JWT Contract

- Signing: HS256 using a Base64-encoded secret of at least 256 bits.
- Default access-token lifetime: 15 minutes (`JWT_ACCESS_TOKEN_TTL`).
- Required issuer: `campushub` by default (`JWT_ISSUER`).
- `sub`: Auth account UUID.
- `sid`: login-session UUID.
- `jti`: unique access-token UUID.
- `roles`: account roles; permissions, profile data, and credentials are not
  embedded.
- Redis stores one current `jti` for each online login session.
- A correctly signed JWT is accepted only when its `jti` matches the current
  Redis value for its session.
- Refresh rotates both the database refresh token and the Redis `jti`.
- OAuth remains deferred.

## Marketplace

All Marketplace endpoints require a current Bearer access token. An API request
without a current token receives the normal JSON `401 Unauthorized` response;
the API never redirects to an HTML login page. The frontend redirects to login
and supplies a same-origin return location so the user can resume the original
Marketplace page after authentication.

Marketplace V1 uses fixed USD asking prices. It does not include bidding,
offers, carts, orders, payments, inventory reservation, messaging, or media
upload.

Listing conditions:

- `NEW`
- `LIKE_NEW`
- `GOOD`
- `FAIR`
- `FOR_PARTS`

Listing statuses:

- `ACTIVE`: visible in search results.
- `SOLD`: hidden from search results.
- `WITHDRAWN`: hidden from search results.

### List Labels

- Method: `GET`
- Path: `/api/marketplace/labels`
- Authentication: `Authorization: Bearer <access-token>`
- Success status: `200 OK`
- Idempotency: safe and idempotent.

Response data is a flat, display-ordered collection. `parentSlug` allows the
frontend to present a two-level label picker without coupling the API to a
particular tree component.

```json
[
  {
    "slug": "electronics",
    "displayName": "Electronics",
    "parentSlug": null
  },
  {
    "slug": "graphics-cards",
    "displayName": "Graphics Cards",
    "parentSlug": "electronics"
  }
]
```

Only active labels are returned. Selecting a child label causes its active
ancestor labels to be attached to the listing as well. This makes a listing
tagged `graphics-cards` discoverable through the broader `electronics` label.

Errors:

- `401 AUTH_1005`: the access token is missing, invalid, expired, or is not
  current for its login session.
- `503 AUTH_1016`: the online authentication-session registry is unavailable.

### Search Listings

- Method: `GET`
- Path: `/api/marketplace/listings`
- Authentication: `Authorization: Bearer <access-token>`
- Success status: `200 OK`
- Idempotency: safe and idempotent.

Query parameters:

- `q`: optional trimmed keyword, 1-100 characters when present.
- `labels`: optional comma-separated label slugs; a listing must contain every
  requested label.
- `minPrice`: optional inclusive USD lower bound.
- `maxPrice`: optional inclusive USD upper bound.
- `condition`: optional listing condition.
- `sort`: `RELEVANCE`, `NEWEST`, `PRICE_ASC`, or `PRICE_DESC`. The default is
  `RELEVANCE` when `q` is present and `NEWEST` otherwise.
- `page`: zero-based page number; defaults to `0`.
- `size`: page size; defaults to `20` and cannot exceed `50`.

Example:

```http
GET /api/marketplace/listings?q=5090&labels=electronics,graphics-cards&sort=RELEVANCE&page=0&size=20
```

Search returns only `ACTIVE` listings. Keyword relevance ranks an exact title
match before a title containing the keyword, then a description containing the
keyword. Every sort uses `createdAt` and `id` as deterministic tie-breakers.

Response data:

```json
{
  "items": [
    {
      "listingId": "uuid",
      "title": "NVIDIA RTX 5090",
      "manufactureYear": 2025,
      "condition": "LIKE_NEW",
      "askingPrice": 1999.00,
      "currency": "USD",
      "location": "Main Campus",
      "coverImageUrl": "https://example.com/5090.jpg",
      "labels": ["electronics", "graphics-cards"],
      "createdAt": "2026-09-02T18:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "hasNext": false
}
```

The response deliberately uses `hasNext` rather than a total result count so
the common search path does not require an additional count query.

Errors:

- `400 COMMON_1001`: query parameter validation failed, the price range is
  invalid, or the requested page is outside the supported range.
- `400 MARKETPLACE_1001`: one or more requested labels do not exist or are
  inactive.
- `401 AUTH_1005`: the access token is missing, invalid, expired, or is not
  current for its login session.
- `503 AUTH_1016`: the online authentication-session registry is unavailable.

### Get Listing

- Method: `GET`
- Path: `/api/marketplace/listings/{listingId}`
- Authentication: `Authorization: Bearer <access-token>`
- Success status: `200 OK`
- Idempotency: safe and idempotent.

An `ACTIVE` listing is visible to every authenticated account. A `SOLD` or
`WITHDRAWN` listing is visible only to its seller; other accounts receive the
same not-found response as they would for an unknown ID.

Response data:

```json
{
  "listingId": "uuid",
  "title": "NVIDIA RTX 5090",
  "description": "Used for six months; original box included.",
  "manufactureYear": 2025,
  "condition": "LIKE_NEW",
  "askingPrice": 1999.00,
  "currency": "USD",
  "location": "Main Campus",
  "coverImageUrl": "https://example.com/5090.jpg",
  "labels": ["electronics", "graphics-cards"],
  "status": "ACTIVE",
  "version": 0,
  "createdAt": "2026-09-02T18:00:00Z",
  "updatedAt": "2026-09-02T18:00:00Z"
}
```

Errors:

- `401 AUTH_1005`: the access token is missing, invalid, expired, or is not
  current for its login session.
- `404 MARKETPLACE_1002`: the listing does not exist or is not visible to the
  current account.
- `503 AUTH_1016`: the online authentication-session registry is unavailable.

### Create Listing

- Method: `POST`
- Path: `/api/marketplace/listings`
- Authentication: `Authorization: Bearer <access-token>`
- Authorization: any authenticated account; the authenticated account becomes
  the seller.
- Success status: `201 Created`
- Idempotency: not idempotent; a successful retry creates another listing.

Request:

```json
{
  "title": "NVIDIA RTX 5090",
  "description": "Used for six months; original box included.",
  "manufactureYear": 2025,
  "condition": "LIKE_NEW",
  "askingPrice": 1999.00,
  "location": "Main Campus",
  "coverImageUrl": "https://example.com/5090.jpg",
  "labels": ["electronics", "graphics-cards"]
}
```

Rules:

- `title`: required, trimmed, 3-120 characters.
- `description`: required, trimmed, 1-4000 characters; treated as plain text.
- `manufactureYear`: optional; from 1900 through the next calendar year.
- `condition`: required and must be one of the documented conditions.
- `askingPrice`: required, greater than zero, and at most `9999999999.99`.
- `location`: required, trimmed, 1-120 characters.
- `coverImageUrl`: optional HTTPS URL, at most 2048 characters. Marketplace V1
  stores the URL but does not fetch or proxy the remote content.
- `labels`: 1-5 unique, active label slugs. Child-label ancestors added by the
  service do not count against the submitted limit.
- Currency is assigned by the server as `USD`.
- `sellerAccountId`, status, timestamps, and version cannot be supplied by the
  client. A new listing is immediately `ACTIVE`.

Response data uses the Get Listing representation.

Errors:

- `400 COMMON_1001`: request validation failed.
- `400 MARKETPLACE_1001`: one or more labels do not exist or are inactive.
- `401 AUTH_1005`: the access token is missing, invalid, expired, or is not
  current for its login session.
- `503 AUTH_1016`: the online authentication-session registry is unavailable.

### List My Listings

- Method: `GET`
- Path: `/api/marketplace/listings/mine`
- Authentication: `Authorization: Bearer <access-token>`
- Authorization: only listings owned by the authenticated account are returned.
- Success status: `200 OK`
- Idempotency: safe and idempotent.

Query parameters:

- `status`: optional `ACTIVE`, `SOLD`, or `WITHDRAWN` filter.
- `page`: zero-based page number; defaults to `0`.
- `size`: page size; defaults to `20` and cannot exceed `50`.

Response data uses the Search Listings page representation, with `status`
included in every item. Results default to newest first and include all statuses.

Errors:

- `400 COMMON_1001`: query parameter validation failed.
- `401 AUTH_1005`: the access token is missing, invalid, expired, or is not
  current for its login session.
- `503 AUTH_1016`: the online authentication-session registry is unavailable.

### Update Listing

- Method: `PUT`
- Path: `/api/marketplace/listings/{listingId}`
- Authentication: `Authorization: Bearer <access-token>`
- Authorization: listing seller only.
- Success status: `200 OK`
- Idempotency: conditional on `expectedVersion`; an exact retry after a
  successful update is rejected as stale and cannot mutate the listing again.

Request uses the Create Listing fields and adds the version returned by the
most recent listing read:

```json
{
  "title": "NVIDIA RTX 5090",
  "description": "Original box and receipt included.",
  "manufactureYear": 2025,
  "condition": "LIKE_NEW",
  "askingPrice": 1899.00,
  "location": "Main Campus",
  "coverImageUrl": "https://example.com/5090.jpg",
  "labels": ["electronics", "graphics-cards"],
  "expectedVersion": 0
}
```

Updating listing content does not change its status. Response data uses the Get
Listing representation with the new version.

Errors:

- `400 COMMON_1001`: request validation failed.
- `400 MARKETPLACE_1001`: one or more labels do not exist or are inactive.
- `401 AUTH_1005`: the access token is missing, invalid, expired, or is not
  current for its login session.
- `403 MARKETPLACE_1003`: the current account does not own the listing.
- `404 MARKETPLACE_1002`: the listing does not exist.
- `409 MARKETPLACE_1005`: the listing changed after `expectedVersion` was read.
- `503 AUTH_1016`: the online authentication-session registry is unavailable.

### Change Listing Status

- Method: `PATCH`
- Path: `/api/marketplace/listings/{listingId}/status`
- Authentication: `Authorization: Bearer <access-token>`
- Authorization: listing seller only.
- Success status: `200 OK`
- Idempotency: repeating an already-applied target status returns the current
  representation without another state change.

Request:

```json
{
  "status": "SOLD",
  "expectedVersion": 0
}
```

The seller may change `ACTIVE` to `SOLD` or `WITHDRAWN`, and may reactivate a
`SOLD` or `WITHDRAWN` listing. Response data uses the Get Listing
representation.

Errors:

- `400 COMMON_1001`: request validation failed.
- `401 AUTH_1005`: the access token is missing, invalid, expired, or is not
  current for its login session.
- `403 MARKETPLACE_1003`: the current account does not own the listing.
- `404 MARKETPLACE_1002`: the listing does not exist.
- `409 MARKETPLACE_1004`: the requested status transition is not allowed.
- `409 MARKETPLACE_1005`: the listing changed after `expectedVersion` was read.
- `503 AUTH_1016`: the online authentication-session registry is unavailable.
