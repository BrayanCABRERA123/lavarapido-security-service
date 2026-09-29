# security-service

Identity and access for LavaRapido: accounts, login with JWT (ADR-006), password recovery,
profile, preferences and account administration. Owns the `security` and `audit` schemas
(ADR-009). Port **3001**.

## Run locally

Needs the shared database from the `lavarapido-infra` repository, cloned next to this one:

```bash
cd ../lavarapido-infra && docker compose up -d     # first time: cp .env.example .env
cd ../security-service
./mvnw spring-boot:run                              # Windows: .\mvnw.cmd spring-boot:run
./mvnw verify                                       # tests (no database needed)
```

Secrets are read from `../lavarapido-infra/.env`; a `.env` in this folder overrides it.

## API (`/api/v1`)

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/auth/register` | public | Customer sign-up → 201 user |
| POST | `/auth/login` | public | Email + password → `{accessToken, tokenType, expiresIn, expiresAt, user}` |
| POST | `/auth/logout` | token | Closes the session (`user_session.revoked_at`) → 204 |
| POST | `/auth/password/forgot` | public | Sends a 6-digit code → always 202 (no account enumeration) |
| POST | `/auth/password/verify` | public | Checks the code without consuming it → 204 |
| POST | `/auth/password/reset` | public | Code + new password → 204 |
| GET / PATCH | `/users/me` | token | Own profile (names, phone) |
| PUT | `/users/me/password` | token | Change password (requires the current one) → 204 |
| GET / PUT | `/users/me/preferences` | token | Theme, language, notifications |
| GET / POST | `/admin/users` | ADMIN | List (filter `role`, `page`, `size`) / create account with roles |
| PATCH | `/admin/users/{id}/status` | ADMIN | Enable / disable an account |

Errors: `application/problem+json` with `code` (`INVALID_CREDENTIALS`, `EMAIL_ALREADY_REGISTERED`,
`DOCUMENT_ALREADY_REGISTERED`, `WEAK_PASSWORD` + `violations`, `INVALID_RESET_CODE`,
`VALIDATION_ERROR` + `errors`, `UNAUTHORIZED`, `FORBIDDEN`, ...).

## JWT

HS256, 1 hour, secret from `JWT_SECRET` (≥ 32 bytes, startup fails otherwise). Claims: `sub`
(user id), `roles`, `sid` (session id), `iss`, `aud`, `iat`, `nbf`, `exp`, `jti` — no personal
data. Verification checks signature, algorithm, expiry, issuer and audience. Every other service
must verify with the same secret, issuer and audience.

## Domain rules

- One account per person (`document_number`); the email is the login and never changes.
- A walk-in customer (person without account) can later sign up: the account is linked to the
  existing person, counter data is kept, only missing contact info is filled.
- Password policy = frontend checklist: 8–72 chars, upper-case, digit, special character, no spaces.
- Recovery codes: 6 digits, stored only as a BCrypt hash, 15 min, single use, a new request burns
  the previous code, 5 wrong guesses burn it.
- Login never reveals whether the email exists (same error, same BCrypt timing); a disabled
  account is reported only after the correct password.
- Events `security.user.registered` and `security.user.password-changed` are raised (logged
  until RabbitMQ is wired; they will need an outbox).

## Structure

```
domain/          model (aggregates, value objects), service (policies), event, exception, port/in, port/out
application/     use cases (@Service + @Transactional)
infrastructure/  adapter/in/web, adapter/out/{persistence,security,notification,messaging}, config
```

## Known limitations

- Recovery codes are only printed to the log (dev); an email/SMS channel is pending.
- The failed-guess counter is in memory: fine for one instance, needs Redis with replicas.
- Tokens cannot be revoked before expiry (ADR-006 trade-off); logout only records the session end.
- No login rate limiting here: it belongs to the API Gateway (ADR-005).
- `audit` triggers and the per-service DB login (`svc_security`) are not created yet; dev uses `sa`.
