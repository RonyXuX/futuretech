# FutureTech security design

Three independent layers protect logins. If one fails, the others still hold.

## Layer 1: passwords

- Stored only as **BCrypt** hashes. BCrypt is salted and slow on purpose, so a leaked hash is
  very expensive to crack.
- **Strong-password rule** on register: 8-72 characters, at least one letter and one digit.
  BCrypt ignores anything after 72 bytes, so longer passwords are refused.
- **No account discovery:**
  - "Wrong password" and "no such email" return the same message.
  - An unknown email still runs one BCrypt check, so both cases take the same time.
- Emails are compared in lowercase.

## Layer 2: tokens

| | Access token | Refresh token |
|---|---|---|
| What | Signed JWT (HS256) | 256-bit random string |
| Lifetime | 15 minutes | 7 days |
| Sent as | `Authorization: Bearer ...` header | `refresh_token` cookie: HttpOnly, SameSite=Strict, Path=/api/auth, Secure in production |
| Stored on the server | Nothing (stateless) | Only its **SHA-256 hash** (`refresh_tokens` table) |
| Revocable | No (it just expires quickly) | Yes: logout, rotation, theft detection |

- **Rotation:** every `/api/auth/refresh` uses up the old refresh token and issues a new one.
- **Theft detection:** if an already-used refresh token is presented, someone copied it.
  All sessions of that user are then ended.
- **JWT contents:**
  - Claims: `sub` = user id, `email`, `role`, `iat`, `exp`.
  - The signing key comes from the `JWT_SECRET` env var (at least 32 characters). The app
    refuses to start without it.
- **Why no CSRF tokens:**
  - API calls use the Bearer header, which browsers never add on their own.
  - The only cookie is SameSite=Strict and limited to `/api/auth`.

## Layer 3: brute-force protection

- **Per account:** 5 wrong passwords in a row lock the account for 15 minutes.
  - The response is `423 Locked` with a `Retry-After` header.
  - The correct password is also refused while locked.
- **Per IP address:** at most 10 login (and 10 register) attempts per minute.
  - The response is `429 Too Many Requests` with `Retry-After`.
  - This catches one machine trying many accounts.
  - It's kept in memory, which is fine for one server; several servers would need a shared store such as Redis.

## Authorization (who may do what)

| URLs | Access |
|---|---|
| `GET /api/products/**`, `/api/categories/**`, `/api/health`, `POST /api/auth/**` | Everyone |
| `/api/admin/**` | JWT with role `ADMIN` |
| Everything else | Any logged-in user |

The logged-in user's id always comes from the verified JWT, never from the URL, so nobody can read
someone else's data by changing an id.

## Configuration

| Env var | Purpose |
|---|---|
| `JWT_SECRET` | **Required.** JWT signing key, at least 32 characters, never committed |
| `COOKIE_SECURE` | `true` in production (HTTPS). Default `false` for local http |
| `CORS_ALLOWED_ORIGINS` | Frontend address(es). Default `http://localhost:4200` |

Generate a strong secret in PowerShell:

```powershell
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

## Tests

| Test | What it covers |
|---|---|
| `UserLockoutTest` | Lock rules |
| `LoginRateLimiterTest` | Per-IP limit |
| `RefreshTokenServiceTest` | Hashing, rotation, theft detection, expiry |
| `AuthServiceTest` | The login flow, using real BCrypt |
| `AuthControllerTest` | Cookie flags, status codes, validation messages |
| `SecurityRulesTest` | Public, logged-in and admin access; expired and tampered tokens, using the app's real JWT signing |
