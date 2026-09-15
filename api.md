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
- `password`: 8-72 characters and at most 72 UTF-8 bytes (BCrypt limit). It
  must contain at least one uppercase letter, one lowercase letter, one digit,
  and one of `!@#$%^&*._-`; no other characters are accepted.
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
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "sessionExpiresAt": "2026-10-15T12:00:00Z"
}
```

The raw refresh token is not included in the JSON response. A successful login
sets it in the `campushub_refresh_token` cookie with `HttpOnly`, the configured
`Secure` flag, `SameSite=Strict` by default, and `Path=/api/auth`. Browser
clients keep the access token in memory and send it through the
`Authorization` header.

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

Request body: none. The browser sends the `campushub_refresh_token` HttpOnly
cookie automatically.

Response data:

```json
{
  "accessToken": "new-jwt",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "sessionExpiresAt": "2026-10-15T12:00:00Z"
}
```

A successful refresh atomically replaces the login session's current JWT ID in
Redis. The previous access token for that same session is no longer current.
It also marks the cookie's refresh token as `USED`, creates a new active refresh
token, and replaces the HttpOnly cookie in the response. The rotated cookie can
be used for the next refresh, so the chain can continue until the original
login session expires. Refresh does not extend that absolute session expiration
time.

Different login sessions for the same account are independent; refreshing one
session does not invalidate access tokens issued to another session.

Errors:

- `401 AUTH_1015`: the refresh cookie is missing, malformed, unknown, expired,
  already used, its session is inactive, or its online Redis session is
  missing.
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
operation uses the same session-first lock order as refresh, revokes the online
Redis session, and clears the `campushub_refresh_token` cookie with `Max-Age=0`.

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
offers, carts, orders, payments, inventory reservation, or messaging.

Listing conditions:

- `NEW`
- `OPEN_BOX`
- `LIKE_NEW`
- `GOOD`
- `FAIR`
- `FOR_PARTS_OR_NOT_WORKING`

Listing statuses:

- `ACTIVE`: visible in search results.
- `SOLD`: hidden from search results.
- `WITHDRAWN`: hidden from search results.

### Upload Marketplace Images

- Method: `POST`
- Path: `/api/marketplace/images`
- Authentication: `Authorization: Bearer <access-token>`
- Content type: `multipart/form-data`
- Success status: `201 Created`
- Idempotency: not idempotent; retrying creates a new upload batch and new
  object keys.

The multipart field name is `files`. A request accepts 1-8 images. Each image
must be at most 5 MB and contain valid JPEG, PNG, or WebP bytes. Validation uses
the file signature rather than trusting the browser-supplied content type.

Objects use the following key structure inside the Marketplace bucket:

```text
marketplace/accounts/{accountId}/{uploadBatchId}/{imageId}.{extension}
```

Response data:

```json
{
  "uploadBatchId": "uuid",
  "imageUrls": [
    "https://marketplace-assets.example.com/marketplace/accounts/account-uuid/batch-uuid/image-uuid.webp"
  ]
}
```

The returned HTTPS URLs are supplied as `imageUrls` when creating or updating
a listing. URL order is preserved, and the first URL becomes the listing's
primary image. If a batch fails after some objects were stored, the service
attempts to remove those partial objects.

Errors:

- `400 COMMON_1001`: the servlet upload-size limit was exceeded.
- `400 MARKETPLACE_1013`: the image count, size, content, or supported format
  is invalid.
- `401 AUTH_1005`: the access token is missing, invalid, expired, or is not
  current for its login session.
- `503 AUTH_1016`: the online authentication-session registry is unavailable.
- `503 MARKETPLACE_1014`: OSS is disabled or temporarily unavailable. When OSS
  is explicitly enabled with incomplete configuration, application startup
  fails fast instead of exposing a partially configured upload endpoint.

### Get Marketplace Catalog

- Method: `GET`
- Path: `/api/marketplace/catalog`
- Authentication: `Authorization: Bearer <access-token>`
- Success status: `200 OK`
- Idempotency: safe and idempotent.

The catalog supplies the frontend's two-level category navigation and brand
filter. Categories and brands are returned in display order.

```json
{
  "categories": [
    {
      "slug": "electronics",
      "displayName": "Electronics",
      "children": [
        {
          "slug": "graphics-cards",
          "displayName": "Graphics Cards"
        }
      ]
    }
  ],
  "brands": [
    {
      "slug": "nvidia",
      "displayName": "NVIDIA"
    }
  ]
}
```

Only active catalog entries are returned. First-level categories are used for
broad browsing; their children are the categories accepted when a listing is
created or updated.

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

- `q`: optional keyword, at most 100 characters.
- `category`: optional first- or second-level category slug. A first-level slug
  includes listings from all of its child categories.
- `brand`: optional brand slug and may be used without a category.
- `condition`: optional listing condition.
- `minPrice`: optional inclusive USD lower bound.
- `maxPrice`: optional inclusive USD upper bound.
- `page`: zero-based page number; defaults to `0`.
- `size`: page size; defaults to `20` and cannot exceed `50`.

Example:

```http
GET /api/marketplace/listings?q=5090&category=graphics-cards&brand=nvidia&page=0&size=20
```

Search returns only `ACTIVE` listings. A title containing the keyword is ranked
before other matches, then results use `createdAt` and `id` as deterministic
newest-first tie-breakers.

Response data:

```json
{
  "items": [
    {
      "id": "uuid",
      "category": {
        "slug": "graphics-cards",
        "displayName": "Graphics Cards",
        "parentSlug": "electronics",
        "parentDisplayName": "Electronics"
      },
      "brand": {
        "slug": "nvidia",
        "displayName": "NVIDIA"
      },
      "title": "NVIDIA RTX 5090",
      "condition": "LIKE_NEW",
      "price": 1999.00,
      "currency": "USD",
      "location": "Main Campus",
      "deliveryMethod": "LOCAL_PICKUP",
      "primaryImageUrl": "https://example.com/5090.jpg",
      "createdAt": "2026-09-02T18:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
```

Errors:

- `400 COMMON_1001`: query parameter validation or format failed.
- `400 MARKETPLACE_1006`: a category or brand slug is invalid.
- `400 MARKETPLACE_1012`: the price range is invalid.
- `401 AUTH_1005`: the access token is missing, invalid, expired, or is not
  current for its login session.
- `503 AUTH_1016`: the online authentication-session registry is unavailable.

### Get Listing

- Method: `GET`
- Path: `/api/marketplace/listings/{listingId}`
- Authentication: `Authorization: Bearer <access-token>`
- Success status: `200 OK`
- Idempotency: safe and idempotent.

Only `ACTIVE` listings are returned by this endpoint. Sellers use the separate
seller-management endpoints to retrieve and manage their other listings.

Response data:

```json
{
  "id": "uuid",
  "sellerAccountId": "uuid",
  "category": {
    "slug": "graphics-cards",
    "displayName": "Graphics Cards",
    "parentSlug": "electronics",
    "parentDisplayName": "Electronics"
  },
  "brand": {
    "slug": "nvidia",
    "displayName": "NVIDIA"
  },
  "title": "NVIDIA RTX 5090",
  "description": "Used for six months; original box included.",
  "manufactureYear": 2025,
  "condition": "LIKE_NEW",
  "price": 1999.00,
  "currency": "USD",
  "location": "Main Campus",
  "deliveryMethod": "LOCAL_PICKUP",
  "imageUrls": [
    "https://example.com/5090-front.jpg",
    "https://example.com/5090-back.jpg"
  ],
  "status": "ACTIVE",
  "version": 0,
  "createdAt": "2026-09-02T18:00:00Z",
  "updatedAt": "2026-09-02T18:00:00Z"
}
```

Errors:

- `401 AUTH_1005`: the access token is missing, invalid, expired, or is not
  current for its login session.
- `404 MARKETPLACE_1002`: the listing does not exist or is not active.
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
  "categorySlug": "graphics-cards",
  "brandSlug": "NVIDIA",
  "title": "NVIDIA RTX 5090",
  "description": "Used for six months; original box included.",
  "manufactureYear": 2025,
  "condition": "LIKE_NEW",
  "price": 1999.00,
  "location": "Main Campus",
  "deliveryMethod": "LOCAL_PICKUP",
  "imageUrls": [
    "https://example.com/5090-front.jpg",
    "https://example.com/5090-back.jpg"
  ]
}
```

Rules:

- `categorySlug`: required active second-level category slug; input is normalized
  to lowercase.
- `brandSlug`: optional active brand slug; input is normalized to lowercase.
- `title`: required, trimmed, at most 160 characters.
- `description`: required, trimmed, at most 5000 characters; treated as plain
  text.
- `manufactureYear`: optional integer from 1800 through 2100.
- `condition`: required and must be one of the documented conditions.
- `price`: required, greater than zero, with at most 10 integer digits and 2
  decimal digits.
- `location`: required, trimmed, at most 160 characters.
- `deliveryMethod`: required `LOCAL_PICKUP`, `SHIPPING`, or
  `PICKUP_OR_SHIPPING`.
- `imageUrls`: 1-8 non-blank HTTPS URLs, each at most 2048 characters. The
  first URL is the primary image; Marketplace V1 stores URLs but does not fetch
  or proxy remote content.
- Currency is assigned by the server as `USD`.
- `sellerAccountId`, status, timestamps, and version cannot be supplied by the
  client. A new listing is immediately `ACTIVE`.

Response data uses the Get Listing representation.

Errors:

- `400 COMMON_1001`: request validation failed.
- `400 MARKETPLACE_1006`: a catalog slug is invalid.
- `404 MARKETPLACE_1007`: the category does not exist.
- `400 MARKETPLACE_1008`: the category cannot be used for a listing.
- `404 MARKETPLACE_1009`: the brand does not exist.
- `400 MARKETPLACE_1010`: the brand is inactive.
- `400 MARKETPLACE_1011`: listing details are invalid.
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

Response data:

```json
{
  "items": [
    {
      "id": "uuid",
      "title": "NVIDIA RTX 5090",
      "price": 1999.99,
      "currency": "USD",
      "primaryImageUrl": "https://example.com/5090.jpg",
      "status": "ACTIVE",
      "version": 0,
      "createdAt": "2026-09-14T10:00:00Z",
      "updatedAt": "2026-09-14T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
```

Results default to newest first and include every status when `status` is
omitted. The seller account ID is always taken from the authenticated access
token and cannot be supplied as a query parameter.

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
  "categorySlug": "graphics-cards",
  "brandSlug": "NVIDIA",
  "title": "NVIDIA RTX 5090",
  "description": "Original box and receipt included.",
  "manufactureYear": 2025,
  "condition": "LIKE_NEW",
  "price": 1899.00,
  "location": "Main Campus",
  "deliveryMethod": "LOCAL_PICKUP",
  "imageUrls": [
    "https://example.com/5090-front.jpg",
    "https://example.com/5090-back.jpg"
  ],
  "expectedVersion": 0
}
```

Updating listing content does not change its status. `categorySlug` and
`brandSlug` are normalized to lowercase. The category must be an active
second-level category; `brandSlug` is optional. Response data uses the Get
Listing representation with the new version.

Errors:

- `400 COMMON_1001`: request validation failed.
- `400 MARKETPLACE_1006`: a catalog slug is invalid.
- `404 MARKETPLACE_1007`: the category does not exist.
- `400 MARKETPLACE_1008`: the category cannot be used for a listing.
- `404 MARKETPLACE_1009`: the brand does not exist.
- `400 MARKETPLACE_1010`: the brand is inactive.
- `400 MARKETPLACE_1011`: listing details are invalid.
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

Allowed transitions are `ACTIVE` to `SOLD` or `WITHDRAWN`, and `SOLD` or
`WITHDRAWN` back to `ACTIVE`. Direct `SOLD` to `WITHDRAWN` and `WITHDRAWN` to
`SOLD` transitions are rejected. Repeating the current target status is
idempotent and returns the current representation even if the supplied version
is stale. Response data uses the Get Listing representation.

Errors:

- `400 COMMON_1001`: request validation failed.
- `401 AUTH_1005`: the access token is missing, invalid, expired, or is not
  current for its login session.
- `403 MARKETPLACE_1003`: the current account does not own the listing.
- `404 MARKETPLACE_1002`: the listing does not exist.
- `409 MARKETPLACE_1004`: the requested status transition is not allowed.
- `409 MARKETPLACE_1005`: the listing changed after `expectedVersion` was read.
- `503 AUTH_1016`: the online authentication-session registry is unavailable.
