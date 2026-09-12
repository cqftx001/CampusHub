# Marketplace Module

The Marketplace Module owns labels, fixed-price listings, listing search, and
seller-controlled listing lifecycle behavior. It intentionally contains no
business code yet.

## Overview

- Search and discovery are label-first. Free text is a secondary signal for
  product names and model numbers such as `RTX 5090`.
- Labels come from a controlled catalog. Sellers cannot create free-form labels.
- Child labels retain their parent labels, so `graphics-cards` listings also
  appear under `electronics`.
- The client is photo-led and keeps listing cards concise. V1 supports one
  optional HTTPS cover image without introducing upload or storage behavior.
- Every endpoint requires authentication. The API returns JSON `401` responses;
  the frontend owns login redirect and same-origin return navigation.

## V1 Endpoints

| Endpoint | Authorization | Purpose |
|---|---|---|
| `GET /api/marketplace/labels` | Authenticated | Get the active label catalog |
| `GET /api/marketplace/listings` | Authenticated | Search active listings |
| `GET /api/marketplace/listings/{id}` | Authenticated | Get a visible listing |
| `POST /api/marketplace/listings` | Authenticated | Publish an active listing |
| `GET /api/marketplace/listings/mine` | Authenticated seller | Manage owned listings |
| `PUT /api/marketplace/listings/{id}` | Listing seller | Replace listing content |
| `PATCH /api/marketplace/listings/{id}/status` | Listing seller | Sell, withdraw, or reactivate |

## Search Contract

Requested labels use AND semantics: a result contains every requested label but
may contain additional labels. Search exposes deterministic pagination and the
`RELEVANCE`, `NEWEST`, `PRICE_ASC`, and `PRICE_DESC` sorts. Label-only browsing
defaults to newest first.

The initial implementation uses PostgreSQL tables and indexes rather than a
separate search service. It should page matching listing IDs first and load the
corresponding labels in a second bounded query, avoiding collection-join
pagination and N+1 reads.

## Module Boundary

Marketplace stores the authenticated seller's account UUID but does not depend
on the Auth or User feature modules and does not create a cross-schema foreign
key. `AuthenticatedAccount` from `shared-kernel` is the identity contract.

Orders, payments, inventory reservation, bidding, offers, messaging, media
upload, recommendations, and label administration remain out of V1 scope.
Detailed HTTP contracts and error cases are documented in `/api.md`.
