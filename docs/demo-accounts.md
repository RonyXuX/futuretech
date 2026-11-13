# Demo accounts

## Customers (created by `V5__demo_customers_and_orders.sql`)

All three use the password **`Demo@2026`**. These are throw-away demo accounts, not secrets;
only the BCrypt hash is stored in the database.

| Email | What the account shows |
|---|---|
| lina.haddad@example.com | 2 delivered orders: an old one (2024) with **expired** warranties and a recent one with active warranties |
| omar.khalil@example.com | A full custom PC bought as parts (8 parts, delivered) + a small order that is **shipped** |
| sara.mansour@example.com | A **cancelled** order (points taken back), a **placed** order, and one **saved ForgeAI build** |

Points rule: **1 point per whole dollar** of the order total. A cancelled order gets its points
removed with a negative `ADMIN_ADJUSTMENT` entry in `point_transactions`.

## Admin

The admin account is **not** in any SQL file. `AdminAccountInitializer` creates it the first time
the app starts with these environment variables set:

```powershell
$env:ADMIN_EMAIL = "admin@futuretech.local"
$env:ADMIN_PASSWORD = "choose-a-strong-password"
```

If the variables are not set, no admin is created. Once the account exists, changing the
variables does nothing; the password is changed through the app (or by deleting the row).
