# Database Structure

## Purpose
This document is the source of truth for the database design used by:
- `user-api` (Spring Boot backend)
- Unity client (through API calls, not direct DB access)

Keep this file updated whenever schema changes are introduced.

## Current Snapshot
Date: 2026-03-27
Status: Draft (updated with Lucid user table now mapped in Java)

## Tables

### `app_users`
Source:
- Historical starter schema (first API iteration)

Columns:
- `id` `BIGINT` `PRIMARY KEY` `NOT NULL`
- `name` `VARCHAR` `NOT NULL`

Notes:
- Legacy starter table from first API iteration.
- Current Java model uses `users` table instead.

---

### `users`
Source:
- `db-diagram.pdf` (Lucid export)
- JPA entity in `user-api/src/main/java/user_api/User.java`

Columns:
- `user_id` `INT` `PRIMARY KEY` `NOT NULL`
- `username` `STRING` `NOT NULL` `UNIQUE`
- `email` `STRING` `NOT NULL` `UNIQUE`
- `is_premium` `BOOLEAN` `NOT NULL`
- `premium_since` `DATETIME` `NULL`
- `premium_until` `DATETIME` `NULL`
- `created_at` `DATETIME` `NOT NULL`
- `password` `STRING` `NOT NULL`

Notes:
- Implemented in current Spring/JPA code as table name `users`.
- API endpoints using this table:
  - `POST /users`
  - `GET /users/{id}`

---

### `students`
Source:
- SQL init script in `db/init.sql`

Columns:
- `id` `SERIAL` `PRIMARY KEY` `NOT NULL`
- `name` `VARCHAR(100)` `NOT NULL`
- `course` `VARCHAR(100)` `NOT NULL`
- `grade` `NUMERIC(4,2)` `NULL`
- `created_at` `TIMESTAMP` `DEFAULT NOW()`

Notes:
- Seed/sample table from lab setup.
- May be removed later if not needed by game features.

## Relationships
Currently documented relationships:
- None

## Constraints And Rules
- Primary keys:
  - `app_users.id`
  - `users.user_id`
  - `students.id`
- Unique constraints:
  - `users.username`
  - `users.email`
- No foreign keys documented yet in this file.

## Naming Conventions (proposed)
- Tables: `snake_case`, plural when practical
- Primary key: `id`
- Foreign keys: `<referenced_table_singular>_id`
- Timestamps: `created_at`, `updated_at`

## How To Add New Tables From Lucid
Copy each table using this mini-template:

```md
### `<table_name>`
Source:
- Lucid diagram v<version/date>

Columns:
- `<column>` `<type>` `<NULL/NOT NULL>` `<extra: PK/FK/UNIQUE/DEFAULT ...>`

Relationships:
- `<table.column>` -> `<other_table.other_column>`

Notes:
- business rule 1
- business rule 2
```

## Change Process
When schema changes:
1. Update this file first.
2. Add SQL migration (or update JPA/migration plan).
3. Update Java entities/repositories/services.
4. Update API contract if request/response changes.
