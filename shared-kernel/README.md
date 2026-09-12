# Shared Kernel

Shared Kernel is a real Maven module in the modular monolith.

It owns framework templates and common primitives that are genuinely shared by
multiple modules, such as the unified API response, base error contract, base
exception, and request tracing helper.

It does not own feature-specific DTOs, domain models, business rules, or module
error codes.
