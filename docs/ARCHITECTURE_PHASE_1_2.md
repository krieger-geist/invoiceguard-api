# InvoiceGuard API — Phase 1 & Phase 2 Deep Dive

This document explains, in depth, everything built so far, why it was built that way, and what comes next. It's meant to be read alongside the source code — file paths are given throughout so you can jump straight to the real implementation.

---

## 1. The big picture: what kind of project is this?

InvoiceGuard is being built as a **modular monolith** using **package-by-feature** organization. Two architectural terms worth unpacking, since they drive almost every structural decision you'll see:

- **Modular monolith** — one deployable Spring Boot application (one JAR, one process), but internally split into self-contained modules (`auth`, `organization`, `vendor`, `invoice`, `risk`, ...). This is different from a classic "layered" monolith (one giant `controller/`, `service/`, `repository/` folder for the whole app) and different from microservices (many deployables, network calls between them). You get most of microservices' organizational clarity without the operational cost of distributed systems — no network latency between modules, no distributed transactions, one thing to deploy and monitor. The trade-off is discipline: nothing stops one module from reaching into another's internals except convention and code review, so the package boundaries have to be treated as real boundaries.

- **Package-by-feature** — instead of `controller/AuthController.java`, `controller/VendorController.java`, `service/AuthService.java`, `service/VendorService.java` all mixed together, each feature gets its own package containing its own `controller/`, `service/`, `entity/`, etc. Open `com.invoiceguard.auth` and you see the *entire* auth feature — every layer, in one place. This is the opposite of "package-by-layer," which scales badly: once you have 15 controllers and 40 services in two folders, finding anything requires search, not browsing.

Every phase from here adds one or more feature packages to this structure without changing the pattern.

---

## 2. Phase 1 — the skeleton every other phase builds on

Phase 1 didn't implement any business feature. It built the *infrastructure every feature needs*, so Phase 2 onward could focus purely on domain logic instead of re-solving "how do errors look," "how do I know which organisation this entity belongs to," and so on, every time.

### 2.1 `pom.xml` — the technology stack

Java 21, Spring Boot 3.3.4, and every dependency your original spec asked for: Spring Web, Data JPA, Security, Validation, Redis, Actuator, Flyway, PostgreSQL driver, JJWT (JWT library), Lombok, MapStruct, springdoc-openapi (Swagger), and Testcontainers for integration tests. One deliberate choice: **MapStruct + Lombok together** need an extra annotation-processor binding (`lombok-mapstruct-binding`) in the compiler plugin config, otherwise MapStruct can't see Lombok-generated getters/setters at compile time — a classic gotcha that's already handled for you.

### 2.2 `BaseEntity` and `OrganizationScopedEntity` — the two classes everything extends

**File:** `common/entity/BaseEntity.java`

Every table in the system needs: a UUID primary key, `createdAt`/`updatedAt` timestamps, who created/last touched the row, an optimistic-locking version number, and a soft-delete flag. Rather than repeating those seven fields (and their JPA annotations) in every entity, `BaseEntity` is a `@MappedSuperclass` that all entities extend. Concretely:

```java
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version                    // <- optimistic locking, explained below
    private Long version;

    @CreatedDate  private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
    @CreatedBy    private String createdBy;
    @LastModifiedBy private String updatedBy;

    private boolean deleted = false;   // soft delete
}
```

**Why `@Version` matters:** this is JPA's built-in optimistic locking. Every `UPDATE` statement Hibernate generates automatically includes `WHERE version = ?` and bumps the version. If two requests read the same invoice, both try to update it, the second one's `UPDATE` matches zero rows (because the first request already changed the version), and Hibernate throws `ObjectOptimisticLockingFailureException`. Your `GlobalExceptionHandler` (below) catches exactly that exception and returns a clean 409 Conflict — "someone else already changed this, reload and try again" — instead of silently letting the second write clobber the first. This is what your spec's "prevent two people from approving the same invoice" and "@Version everywhere" requirements are actually implemented by.

**Why `@CreatedBy`/`@LastModifiedBy` "just work" later:** these are populated by an `AuditorAware<String>` bean (`config/AuditingConfig.java`) that reads the current Spring Security authentication. In Phase 1, before any auth existed, it always returned `"system"`. As soon as Phase 2 wired real JWT authentication, this bean started returning the real logged-in user's email automatically — **no code in `BaseEntity` or `AuditingConfig` had to change.** That's a small but real example of designing a seam before you need it.

**File:** `common/entity/OrganizationScopedEntity.java`

```java
public abstract class OrganizationScopedEntity extends BaseEntity {
    private UUID organizationId;
}
```

This is the multi-tenancy backbone. Every table that belongs to one organisation (vendors, invoices, alerts, `OrganizationMember`, everything in Phases 3 onward) extends this instead of `BaseEntity` directly, which adds an indexed `organization_id` column. Two deliberate choices here:

1. It's a **plain column, not a JPA `@ManyToOne` relationship** to `Organization`. If it were a relationship, every query touching a vendor would risk an extra join or lazy-loading trip just to read an ID you already have. As a plain UUID column it's cheap to index and cheap to filter on in `Specification` predicates (used heavily from Phase 4 onward for search/filtering).
2. Nothing here *enforces* tenant isolation automatically — that's a common misconception about "multi-tenant base classes." The column exists so that isolation *can* be enforced; the actual enforcement is `TenantContext` (Phase 2, explained below) plus disciplined repository/service code. This is why the class-level Javadoc explicitly points future developers at `TenantContext`.

### 2.3 The standard response envelope

**Files:** `common/response/ApiResponse.java`, `ErrorResponse.java`, `PageResponse.java`

Every successful response returns the same shape:
```json
{ "success": true, "message": "...", "data": {...}, "timestamp": "...", "correlationId": "..." }
```
Every error returns:
```json
{ "success": false, "code": "INVOICE_NOT_FOUND", "message": "...", "fieldErrors": [], "timestamp": "...", "path": "...", "correlationId": "..." }
```
This consistency is what lets a frontend (or Postman collection, or another service) write one generic response-handling function instead of guessing the shape per-endpoint. `PageResponse<T>` does the same job for paginated list endpoints (Phase 4 onward) — `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`.

### 2.4 Exception handling — one funnel for every error in the system

**Files:** `exception/ErrorCode.java`, `ApplicationException.java` + 7 subclasses, `GlobalExceptionHandler.java`

The pattern:
1. `ErrorCode` is an enum where **every value already knows its own HTTP status** (`INVOICE_NOT_FOUND → 404`, `ACCOUNT_LOCKED → 423`, `OPTIMISTIC_LOCK_CONFLICT → 409`, etc.).
2. `ApplicationException` is the one base class every business exception extends; it just carries an `ErrorCode`.
3. Seven concrete subclasses exist for common situations (`ResourceNotFoundException`, `DuplicateResourceException`, `InvalidStateTransitionException`, `BusinessRuleViolationException`, `RateLimitExceededException`, `TenantAccessDeniedException`, `IdempotencyConflictException`) so service code reads naturally: `throw ResourceNotFoundException.of("Invoice", id);`
4. `GlobalExceptionHandler` (`@RestControllerAdvice`) is the *only* place that turns exceptions into HTTP responses. It handles: any `ApplicationException` (via its `ErrorCode`), Bean Validation failures (`MethodArgumentNotValidException` → per-field errors), malformed JSON, Spring Security exceptions, JPA optimistic-lock and data-integrity violations, unsupported HTTP methods, and a catch-all `Exception` handler that logs the full stack trace server-side but returns a generic "An unexpected error occurred" message — **stack traces never reach the client**, satisfying your spec's explicit requirement.

No controller anywhere in the codebase has (or should have) a `try/catch` for business exceptions. They propagate up and get translated once, centrally, so the response shape is guaranteed consistent no matter which module threw the error.

### 2.5 Correlation IDs — tracing one request across logs

**File:** `config/CorrelationIdFilter.java`

Every request either brings its own `X-Correlation-Id` header (useful if a frontend or another service wants to trace a call end-to-end) or gets a fresh UUID generated for it. That ID is:
- put into the SLF4J **MDC** (Mapped Diagnostic Context), so every log line written during that request can include it in structured logging (wired up in Phase 8),
- stashed as a request attribute so `GlobalExceptionHandler` and `RestAuthenticationEntryPoint`/`RestAccessDeniedHandler` can echo it back in the `correlationId` field of every response,
- echoed back as a response header.

This is what lets you take a `correlationId` a user reports and grep every log line across the whole request lifecycle, even once there are dozens of modules involved.

### 2.6 Configuration profiles

Four YAML files: `application.yml` (shared defaults, all read from environment variables with sane fallbacks — **no secret is ever hardcoded**), `application-dev.yml` (verbose SQL logging, seed data enabled), `application-test.yml` (used by Testcontainers-backed integration tests), `application-prod.yml` (Swagger disabled by default, stack traces hidden, `ddl-auto: validate` — Hibernate is *never* allowed to auto-generate schema; Flyway migrations are the only source of truth for the database shape, exactly as your spec required).

---

## 3. Phase 2 — Organizations, Users, Security, and Authentication

This is where the system became runnable end-to-end for the first time: you can now register, log in, and get a token that proves who you are and what you're allowed to do.

### 3.1 The identity model: three entities, one relationship

```
User  ──────< OrganizationMember >──────  Organization
(global               (the join:              (the tenant)
 identity)          role + status)
```

- **`Organization`** (`organization/entity/Organization.java`) — the tenant. Has a human name and a URL-safe, globally-unique `slug` auto-derived from the name at creation time (`OrganizationService.generateUniqueSlug`: "Acme Corp" → `acme-corp`, or `acme-corp-2` if taken). Notably, `Organization` does **not** extend `OrganizationScopedEntity` — it *is* the boundary, not a member inside one.

- **`User`** (`user/entity/User.java`) — a person's login identity: email, BCrypt password hash, name, and **account-security state that belongs here rather than in a separate table**: `status` (ACTIVE/LOCKED/DISABLED), `emailVerified`, `failedLoginAttempts`, `lockedUntil`, `lastLoginAt`. This is deliberately global, not organisation-scoped — the same person could in principle belong to more than one organisation.

- **`OrganizationMember`** (`organization/entity/OrganizationMember.java`) — the join entity that actually says "this user has this role in this organisation." It's the one place that *does* extend `OrganizationScopedEntity` (its `organizationId` is the tenant it's scoped to), with a unique constraint on `(organization_id, user_id)` so a user can't be added to the same org twice.

**Why this shape and not just an `organizationId` column directly on `User`?** A direct column is simpler but locks every user into exactly one organisation forever. The join-entity model costs one extra table and one extra query at login, but means "invite an existing user into a second organisation" or "a consultant working across three client orgs" are just additional `OrganizationMember` rows — no schema change, no data migration, ever.

### 3.2 Roles and Permissions — two layers of authorization, implemented as one static map

**Files:** `security/Role.java`, `security/Permission.java`, `security/RolePermissions.java`

Your spec asked for both role-based *and* permission-based authorization. Rather than modeling `Role` and `Permission` as database tables (which your original spec's entity list suggested), Phase 2 implements them as **enums** plus one static `Map<Role, Set<Permission>>`:

```java
MAP.put(Role.FINANCE_MANAGER, EnumSet.of(
    Permission.INVOICE_READ, Permission.INVOICE_WRITE,
    Permission.INVOICE_APPROVE, Permission.INVOICE_APPROVE_HIGH_RISK,
    Permission.INVOICE_APPROVE_CRITICAL_RISK, ...));
```

**Why not a DB table?** The seven roles in your spec (`SUPER_ADMIN`, `ORGANIZATION_ADMIN`, `FINANCE_MANAGER`, `ANALYST`, `REVIEWER`, `AUDITOR`, `API_CLIENT`) are *system-defined* — no organisation invents its own role names. A DB table for genuinely static reference data buys you nothing except a JOIN on every single authorization check. The enum approach means a user's full permission set is resolved once, synchronously, in-memory, at the moment their JWT is issued — no database round-trip needed to check "can this user approve invoices" on every request. If you ever need organisation-customizable roles, `RolePermissions.permissionsFor(Role)` is the one seam that would change to a DB lookup — nothing calling it would need to change.

Permissions themselves are colon-namespaced strings like `invoice:write`, `vendor:bank-change-approve`, `risk:configure` — these become Spring Security "authorities," which is what makes controller code like this work later:

```java
@PreAuthorize("hasAuthority('vendor:bank-change-approve')")
```

### 3.3 How a request proves who it is: the JWT lifecycle

This is the part worth understanding in full, since it's the backbone every future protected endpoint relies on.

**Step 1 — Login.** `AuthService.login()` (not Spring's built-in `AuthenticationManager` — see note below) does, in order:
1. Look up the user by email. If absent, fail generically ("Invalid email or password") — **never** "no such user," which would let an attacker enumerate valid emails.
2. Check `user.isCurrentlyLocked()` — if `lockedUntil` is in the future, reject with `423 Locked` immediately, no password check performed.
3. Check `BCryptPasswordEncoder.matches(rawPassword, user.getPasswordHash())`. If it fails, `registerFailedAttempt()` increments `failedLoginAttempts`; once it hits `invoiceguard.security.max-failed-login-attempts` (default 5), the account is locked for `account-lock-minutes` (default 15) and the counter resets to zero.
4. On success, reset the failure counter, stamp `lastLoginAt`, and resolve **which organisation this session is for** — via `resolveMembership()`: if the user has exactly one active `OrganizationMember`, use it automatically; if they have several, the request must include `organizationId` or the API returns a clear `ORGANIZATION_CONTEXT_REQUIRED` error rather than silently guessing.
5. Build an `AuthenticatedPrincipal` (userId, email, organizationId, role, resolved permission set) and call `issueTokenPair()`.

*Why not use Spring's `AuthenticationManager`/`UserDetailsService`?* That's the idiomatic Spring Security path, and an earlier draft of this code included it — but it was removed. The reason: custom failed-attempt counting and account-locking logic needs direct access to the `User` entity around the credential check, and `AuthenticationManager`'s exception-based flow (`BadCredentialsException`, `LockedException`) doesn't hand you that entity back cleanly. Building both the idiomatic path *and* the manual one would have meant two ways to do the same thing, with only one actually used — that's exactly the kind of unused indirection the project's code-quality rules say to avoid, so it was deleted once the manual approach proved necessary anyway.

**Step 2 — Two tokens are issued** (`AuthService.issueTokenPair`):

- **Access token** — a real signed **JWT** (`security/JwtTokenProvider.java`), HMAC-SHA-signed with `JWT_SECRET`, short-lived (default 15 minutes, `invoiceguard.jwt.access-token-ttl-minutes`). Its claims carry everything a request needs to be authorized *without hitting the database*: `sub` (userId), `email`, `orgId`, `role`, and `perms` (the resolved permission-authority strings). The app **refuses to start** if `JWT_SECRET` isn't set — a deliberate fail-fast rather than silently signing tokens with an empty key.

- **Refresh token** — deliberately **not** a JWT. It's a cryptographically random 48-byte string (`security/OpaqueTokenService.generateToken()`), and only its **SHA-256 hash** is ever written to the `refresh_tokens` table — the raw value is shown to the client exactly once, at issuance, exactly like a password. This matters because a JWT refresh token, if it leaked from a database backup or log, would be immediately usable by an attacker; an opaque token's *hash* leaking is useless without also compromising your hashing (which is one-way).

**Step 3 — Every subsequent request.** `security/JwtAuthenticationFilter.java` runs once per request (before Spring Security's own auth filter), pulls the `Authorization: Bearer <jwt>` header, and calls `JwtTokenProvider.parseAndValidate()`. If the signature and expiry check out, it rebuilds the `AuthenticatedPrincipal` straight from the claims — **zero database queries per request** — and sets it as the Spring Security `Authentication`, with `ROLE_<role>` plus every permission authority attached. This is what makes `@PreAuthorize("hasAuthority('invoice:write')")` and `@PreAuthorize("hasRole('FINANCE_MANAGER')")` both work, and it's why the API can scale request throughput without the auth check itself becoming a bottleneck.

**Step 4 — Refresh, with theft detection.** When the access token expires, the client calls `/auth/refresh` with the raw refresh token. `AuthService.refresh()`:
1. Hashes the presented token and looks it up.
2. **If the stored row is already marked `revoked`**, that's treated as a signal the token was stolen and already used by someone else — every refresh token for that user is immediately revoked (`refreshTokenRepository.revokeAllForUser`), forcing a full re-login everywhere. This "rotation + reuse detection" pattern is the industry-standard defense against a leaked refresh token being replayed silently in the background.
3. Otherwise, issues a brand-new access+refresh pair and revokes the old refresh token, linking `replacedByTokenId` for traceability.

**Step 5 — Logout, password reset, email verification** all reuse the same building blocks. Logout just revokes the presented refresh token. Password reset (`/password-reset/request` → `/confirm`) issues a short-lived (30 min) opaque token exactly like the refresh-token pattern, and — importantly — **revokes every existing refresh token for the user once the password changes**, so a stolen session doesn't survive a password reset. Both the password-reset-request and resend-verification endpoints **always return the same generic success message whether or not the email exists**, to prevent account enumeration.

### 3.4 Tenant isolation in practice: `TenantContext`

**File:** `security/TenantContext.java`

This is the single choke point every future service will call instead of trusting a client-supplied `organizationId`:

```java
UUID orgId = tenantContext.requireOrganizationId(); // reads it out of the JWT-derived principal
```

Because the organisation comes from the *signed JWT*, not from a request body or query parameter, a malicious client editing a JSON payload can never widen their own access — there's no `organizationId` field on any request DTO for them to tamper with in the first place. `assertOwnedByCurrentOrganization()` is the helper future modules (vendor, invoice, ...) will call after loading an entity by ID, to make sure that entity actually belongs to the caller's tenant before returning or mutating it — throwing `TenantAccessDeniedException` (mapped to `403`) if not.

### 3.5 Security wiring: `SecurityConfig`

**File:** `config/SecurityConfig.java`

Three choices worth calling out:
- **`SessionCreationPolicy.STATELESS`** — no server-side HTTP session exists at all; every request must carry its own valid bearer token. This is what makes horizontal scaling trivial later (any instance can handle any request; there's no session affinity to worry about).
- **CSRF disabled** — this is *correct*, not an oversight, for a stateless bearer-token API. CSRF attacks exploit browsers automatically attaching cookies to cross-site requests; there's no cookie-based ambient auth here for a forged request to ride on.
- **`RestAuthenticationEntryPoint`/`RestAccessDeniedHandler`** replace Spring Security's default HTML login-page/403-page behavior with your standard JSON `ErrorResponse` shape, so a 401 or 403 from the security layer looks identical to a 401/403 thrown anywhere else in the app.

Only eight URL patterns are public (`/auth/register`, `/login`, `/refresh`, `/password-reset/**`, `/email-verification/**`, Swagger, `/actuator/health`); everything else requires a valid token by default, with per-endpoint permission checks layered on via `@PreAuthorize` right next to the endpoint they protect (kept local rather than centralized, since "who can call this" is a property of the specific endpoint).

### 3.6 Database migration

**File:** `db/migration/V1__init_identity_and_auth.sql`

Six tables: `organizations`, `users`, `organization_members`, `refresh_tokens`, `password_reset_tokens`, `email_verification_tokens`. Every table carries the full `BaseEntity` column set (`version`, `created_at`, `updated_at`, `created_by`, `updated_by`, `deleted`). Unique constraints exist on `organizations.slug`, `users.email`, `(organization_id, user_id)` on membership, and the token-hash columns — these are **database-enforced**, not just Java-validated, so a race condition or a bug elsewhere in the code still can't produce a duplicate.

---

## 4. Project structure as it stands today

```
com.invoiceguard
├── InvoiceGuardApplication.java        entry point
├── common/
│   ├── entity/     BaseEntity, OrganizationScopedEntity
│   └── response/   ApiResponse, ErrorResponse, PageResponse
├── config/         SecurityConfig, CorrelationIdFilter, AuditingConfig, OpenApiConfig
├── exception/      ErrorCode, ApplicationException + subclasses, GlobalExceptionHandler
├── security/       Role, Permission, RolePermissions, JwtTokenProvider,
│                   JwtAuthenticationFilter, TenantContext, AuthenticatedPrincipal, ...
├── organization/   entity / repository / service / controller / dto / mapper
├── user/           entity / repository / service / controller / dto / mapper
├── auth/           entity (tokens) / repository / service / controller / dto / event
├── notification/   EmailService (stub) — full module comes later
└── (empty, ready for Phase 3+): vendor, invoice, risk, approval, alert,
    analytics, audit, integration
```

Every one of those empty packages already exists on disk so future phases only *add* files — the structure itself never needs reshaping.

---

## 5. What's next: Phase 3 and beyond

| Phase | What it adds |
|---|---|
| **3 (next)** | Vendor management: `Vendor` entity, vendor bank-account history with the "new bank account requires approval, doesn't replace the verified one" rule, verification workflow (pending/verified/rejected/suspended) |
| **4** | Invoice management: `Invoice`/`InvoiceItem`/`InvoiceDocument`, search/filter/pagination via JPA Specifications, idempotency-key handling |
| **5** | Duplicate detection engine, statistical anomaly detection, the explainable rule-based risk-scoring engine — this is also where **Gemini** gets wired in behind `RiskExplanationService`, config-flagged, with the app fully functional if no Gemini key is set |
| **6** | Approval workflow, alerts, audit logging, notifications (event-driven, via Spring Application Events already scaffolded by `UserRegisteredEvent`/`LoginSucceededEvent`/`LoginFailedEvent` in this phase) |
| **7** | Analytics, API keys (machine-to-machine access), webhooks, Redis (rate limiting, caching) |
| **8** | Tests, Docker, CI/CD, final README polish |

If anything above wasn't clear, or you want me to go deeper on one specific piece (e.g., walk through exactly what happens byte-by-byte during a login call, or explain why `OrganizationMember` uses `UUID invitedBy` instead of a relationship), just point at it and I'll expand that section before we move on to Phase 3.
