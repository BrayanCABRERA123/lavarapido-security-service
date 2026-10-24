# security-service

Identity and access for LavaRapido: accounts, login with JWT (ADR-006), password recovery,
profile, preferences and account administration. Owns the `security` and `audit` schemas
(ADR-009). Port **3001**.

## Run locally

Needs the shared database from the `lavarapido-infra` repository, cloned next to this one:

```bash
cd ../lavarapido-infra && docker compose up -d     # first time: cp .env.example .env
cd ../security-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # Windows: .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
./mvnw verify                                       # 200 tests with the 2026-10-23 fixes, 181 on develop (no database needed)
```

Secrets are read from `../lavarapido-infra/.env`; a `.env` in this folder overrides it.

There is no default profile (E-05): turn on `dev` explicitly as above (`docker-compose.yml` already sets
`SPRING_PROFILES_ACTIVE=dev`). With `dev`, one demo account per role (`admin@gmail.com`,
`operador@gmail.com`, `cliente@gmail.com`) is created only if `DEMO_ADMIN_PASSWORD`,
`DEMO_OPERATOR_PASSWORD` and `DEMO_CLIENT_PASSWORD` are set in `.env`; passwords are never in the code.

**Swagger UI:** http://localhost:3001/swagger-ui.html (dev profile only; disabled elsewhere).
Log in with `POST /api/v1/auth/login`, copy the `accessToken`, click **Authorize** and paste it.

## Configuration

| Variable | Default | Use |
|---|---|---|
| `JWT_SECRET` | — (required, ≥ 32 bytes) | HS256 key shared with every service |
| `JWT_ISSUER` / `JWT_AUDIENCE` / `JWT_ACCESS_TOKEN_TTL` | `lavarapido-security-service` / `lavarapido-api` / `1h` | Token claims and life |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | local SQL Server, `sa` / — | Shared `LavaRapido` database |
| `ALLOWED_EMAIL_DOMAINS` | `gmail.com` | Domains accepted for new emails |
| `MAIL_ENABLED` + `MAIL_*` | `false` | Send recovery codes by SMTP |
| `MESSAGING_ENABLED` + `RABBITMQ_*` | `false` | Publish events to RabbitMQ |
| `INTERNAL_API_KEY` | empty (= `/internal/**` closed) | `X-Internal-Key` between services |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200,http://localhost:8081` | Web and Expo web |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_*` | empty | First administrator of a new installation |
| `DEMO_ADMIN_PASSWORD`, `DEMO_OPERATOR_PASSWORD`, `DEMO_CLIENT_PASSWORD` | empty | Demo accounts, `dev` profile only |
| `FORWARD_HEADERS_STRATEGY` | `native` | Client IP from the gateway's `X-Forwarded-For` |

## API (`/api/v1`)

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/auth/register` | public | Customer sign-up; `acceptsTerms` and `acceptsDataPolicy` must be `true` → 201 user |
| POST | `/auth/login` | public | Email + password → `{accessToken, tokenType, expiresIn, expiresAt, user}` |
| POST | `/auth/logout` | token | Closes the session (`user_session.revoked_at`) → 204 |
| GET | `/auth/session` | token | 204 while the token's session is open, else 401 (the gateway asks this) |
| POST | `/auth/password/forgot` | public | Sends a 6-digit code → always 202 (no account enumeration) |
| POST | `/auth/password/verify` | public | Checks the code without consuming it → 204 |
| POST | `/auth/password/reset` | public | Code + new password → 204 |
| GET / PATCH | `/users/me` | token | Own profile (names, phone) |
| PUT | `/users/me/password` | token | Change password (requires the current one) → 204 |
| PUT | `/users/me/email` | token | Change the login email (requires the current password) → user |
| POST | `/users/me/deactivate` | token | Close the own account (requires the current password; not for ADMIN) → 204 |
| GET / PUT | `/users/me/address` | token | Own address |
| GET / PUT | `/users/me/preferences` | token | Theme, language, notification channels |
| GET / POST | `/admin/users` | ADMIN | List (filter `role`, `page`, `size`) / create account with roles |
| PATCH | `/admin/users/{id}/status` | ADMIN | Enable / disable an account (closes its sessions) |
| GET / PUT | `/admin/roles-permissions`, `/admin/roles-permissions/{role}` | ADMIN | Permission matrix per role |
| GET | `/internal/v1/users/{id}/contact`, `/internal/v1/users/admins` | `X-Internal-Key` | Contact data and active admin ids for other services (not routed by the gateway) |

Errors: `application/problem+json` with `code`: `INVALID_CREDENTIALS` (401), `ACCOUNT_DISABLED` (403),
`ACCOUNT_LOCKED` (423), `TOO_MANY_ATTEMPTS` (423), `CONSENT_REQUIRED`, `EMAIL_DOMAIN_NOT_ALLOWED`,
`EMAIL_ALREADY_REGISTERED` (409), `DOCUMENT_ALREADY_REGISTERED` (409), `WEAK_PASSWORD` + `violations`,
`SAME_PASSWORD`, `INCORRECT_CURRENT_PASSWORD`, `INVALID_RESET_CODE`, `CANNOT_DISABLE_OWN_ACCOUNT` (403),
`ADMIN_CANNOT_BE_DISABLED` (403), `USER_NOT_FOUND` (404), `VALIDATION_ERROR` + `errors`, `UNAUTHORIZED`,
`FORBIDDEN`, `INVALID_*` for malformed values (400 unless noted).

## JWT

HS256, 1 hour, secret from `JWT_SECRET` (≥ 32 bytes, startup fails otherwise). Claims: `sub`
(user id), `roles`, `sid` (session id), `iss`, `aud`, `iat`, `nbf`, `exp`, `jti` — no personal
data. Verification checks signature, algorithm, expiry, issuer and audience. Every other service
must verify with the same secret, issuer and audience.

## Domain rules

- One account per person (`document_number`). The email is the login; it can be changed from the
  profile (`PUT /users/me/email`, current password required) and from then on only the new one logs in.
- Sign-up requires both legal consents (Law 1581, RF-001, SEC-07): without `acceptsTerms` and
  `acceptsDataPolicy` set to `true` the answer is 400 `CONSENT_REQUIRED` and nothing is stored.
- A walk-in customer (person without account) can later sign up: the account is linked to the
  existing person, counter data is kept, only missing contact info is filled.
- Email domains (A-01, RF-001): registering, creating a user and changing the email only accept the
  domains in `ALLOWED_EMAIL_DOMAINS` (comma separated, case-insensitive, default `gmail.com`);
  otherwise 400 `EMAIL_DOMAIN_NOT_ALLOWED`. Login and stored accounts are not checked, and the profile
  accepts the current email unchanged even if it is from another domain, so older accounts still work.
- Password policy = frontend checklist: 8–72 characters and at most 72 UTF-8 bytes (BCrypt's limit; an
  accented letter or ñ takes 2 bytes, SEC-11), upper-case, digit, special character, no spaces.
- Login lockout (RF-004b): 5 wrong passwords within 15 minutes lock the account for 15 minutes
  (423 `ACCOUNT_LOCKED`, even with the right password), counted under a SQL Server `sp_getapplock`
  per account. The current password asked by change password, change email and close account counts
  in the same lockout (SEC-12); while it lasts those three answer 423 `TOO_MANY_ATTEMPTS`.
- Recovery codes: 6 digits, stored only as a BCrypt hash, 15 min, single use, a new request burns
  the previous code. Guesses for one user are checked one after another under an `sp_getapplock`
  (SEC-05); the 5th wrong guess burns the code and answers 423 `TOO_MANY_ATTEMPTS`, and so do later
  guesses until a new code is requested or the burned one expires.
- Enumeration: login gives the same error with the same BCrypt time for unknown emails, and
  `/password/forgot` spends the same hashing time either way (SEC-04); a disabled account is reported
  only after the correct password. Still, only real accounts get locked (423) and `/register` answers
  409 for a taken email or document; the gateway's per-IP limit on `/api/v1/auth/**` slows that down.
- Client IP: sessions and recovery codes store the caller's IP. With `server.forward-headers-strategy=native`
  (`FORWARD_HEADERS_STRATEGY`) Tomcat takes it from `X-Forwarded-For` only when the connection comes from
  a private or local address (the gateway, which writes that header; SEC-11). Override the trusted
  networks with `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES` (a regex).
- Events `security.user_registered`, `security.user.password-changed`, `security.user.email-changed`
  and `security.user.deactivated` go to the `carwash.events` exchange after the commit when
  `MESSAGING_ENABLED=true` (otherwise they are only logged). There is no outbox: an event is lost if
  RabbitMQ fails right after the commit.

## Structure

```
domain/          model (aggregates, value objects), service (policies), event, exception, port/in, port/out
application/     use cases (@Service + @Transactional)
infrastructure/  adapter/in/web, adapter/out/{persistence,security,notification,messaging}, config
```

## Known limitations

- Recovery codes go by email over SMTP when `MAIL_ENABLED=true` (Gmail, Brevo, or Mailpit in dev);
  otherwise they are printed to the log (dev profile only). Other system emails are sent by
  `notification-service`.
- The failed-guess counter of recovery codes is in memory: fine for one instance (a restart resets it,
  the code still expires in 15 minutes), needs Redis or a column with replicas.
- Each token carries its session (`sid`): logout, a disabled or closed account, a password change (the
  other sessions) or recovery (all of them), or a role removed in `security.user_role` (E-02: the token's roles are compared with the current
  ones and all the person's sessions are revoked) close it. This service checks it on every call.
- Accepted risk (D-02): the gateway keeps each answer of `/auth/session` for 30 seconds and lets the
  call through if this service does not answer, so other services may accept a closed session for up
  to 30 seconds (or until the token expires, 1 hour, while this service is down).
- Accepted (E-21): `user_session.refresh_token_hash` exists in the schema but is never written; there
  are no refresh tokens, the access token lasts 1 hour and then the user logs in again.
- Known (SEC-15): signing up with the document of a walk-in person links that person to the new
  account without proving it is the same person (there is no identity check at the counter yet).
- Login rate limiting is not done here: the API Gateway limits `/api/v1/auth/**` per IP (ADR-005, SEC-04).
- `audit` triggers and the per-service DB login (`svc_security`) are not created yet; dev uses `sa`.

## Proyecto LavaRápido

Este repositorio es parte de LavaRápido (Vehicle Washing), proyecto formativo del Tecnólogo en
Análisis y Desarrollo de Software (ADSO), ficha 3145556, SENA Regional Huila.

| Recurso | Enlace |
|---|---|
| Documentación y manuales (ES/EN, MD · PDF · Word) | [lavarapido-docs](https://github.com/BrayanCABRERA123/lavarapido-docs#readme) |
| Manual técnico | [09_MANUALES/ES/MD/manual-tecnico.md](https://github.com/BrayanCABRERA123/lavarapido-docs/blob/main/09_MANUALES/ES/MD/manual-tecnico.md) |
| Manual de instalación | [09_MANUALES/ES/MD/manual-de-instalacion.md](https://github.com/BrayanCABRERA123/lavarapido-docs/blob/main/09_MANUALES/ES/MD/manual-de-instalacion.md) |
| Levantar todo el sistema | [lavarapido-infra](https://github.com/BrayanCABRERA123/lavarapido-infra) (`docker compose --profile app up -d`) |
| Aplicación desplegada | https://lavarapidovehicular.northcentralus.cloudapp.azure.com |

**Ramas y commits:** se trabaja en `develop` con ramas `feature/`, `fix/` y `docs/` que se
fusionan con `git merge --no-ff`; los commits siguen Conventional Commits
(`feat(scope): ...`, `fix(scope): ...`). Detalle en la
[guía de git del equipo](https://github.com/BrayanCABRERA123/lavarapido-docs#guía-de-git-del-equipo).

**Equipo:** Brayan Estiven Patiño Cabrera, Juan Camilo Guilombo Osorio y Merari Urbano Quintero.
