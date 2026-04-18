# Database Structure

## Purpose
This document is the source of truth for the database design used by:
- `user-api` (Spring Boot backend)
- Unity client (through API calls, not direct DB access)

Keep this file updated whenever schema changes are introduced.

## Current Snapshot
Date: 2026-03-27
Status: Draft (updated with inventory schema and user inventory API)

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
  - `GET /users/{id}/inventory` (ownership lookup)

---

### `item`
Source:
- `db-diagram.pdf`
- Spring SQL bootstrap (`schema.sql`, `data.sql`)

Columns:
- `item_id` `INT` `PRIMARY KEY` `NOT NULL`
- `item_name` `STRING` `NOT NULL`
- `item_type` `STRING` `NOT NULL`
- `rarity` `STRING` `NOT NULL`
- `description` `TEXT` `NOT NULL`

Notes:
- Base table for all inventory items.
- Subtype tables use `item_id` as both `PK` and `FK`.

---

### `weapon`
Columns:
- `item_id` `INT` `PRIMARY KEY` `FK -> item.item_id`
- `damage` `FLOAT` `NOT NULL`
- `accuracy` `FLOAT` `NOT NULL`
- `range` `FLOAT` `NOT NULL`
- `fire_rate` `FLOAT` `NULL`
- `ammo_type` `STRING` `NULL`

### `armor`
Columns:
- `item_id` `INT` `PRIMARY KEY` `FK -> item.item_id`
- `defense` `FLOAT` `NOT NULL`
- `durability` `FLOAT` `NOT NULL`
- `weight` `FLOAT` `NULL`

### `consumable`
Columns:
- `item_id` `INT` `PRIMARY KEY` `FK -> item.item_id`
- `effect_description` `TEXT` `NOT NULL`
- `duration_seconds` `INT` `NULL`
- `cooldown_seconds` `INT` `NULL`

### `currency`
Columns:
- `item_id` `INT` `PRIMARY KEY` `FK -> item.item_id`
- `currency_code` `STRING` `NOT NULL`
- `is_tradeable` `BOOLEAN` `NOT NULL`

### `material`
Columns:
- `item_id` `INT` `PRIMARY KEY` `FK -> item.item_id`
- `material_grade` `STRING` `NULL`

### `skin`
Columns:
- `skin_id` `INT` `PRIMARY KEY` `NOT NULL`
- `skin_name` `STRING` `NOT NULL`
- `rarity` `STRING` `NOT NULL`
- `created_at` `DATETIME` `NOT NULL`

### `user_skin`
Columns:
- `user_skin_id` `INT` `PRIMARY KEY` `NOT NULL`
- `user_id` `INT` `FK -> users.user_id`
- `skin_id` `INT` `FK -> skin.skin_id`
- `unlocked_at` `DATETIME` `NOT NULL`
- `unlock_source` `STRING` `NULL`

Notes:
- Unique pair on `(user_id, skin_id)`.

### `item_skin`
Columns:
- `item_id` `INT` `FK -> item.item_id`
- `skin_id` `INT` `FK -> skin.skin_id`

Notes:
- Composite primary key `(item_id, skin_id)`.

### `user_inventory`
Columns:
- `user_inventory_id` `INT` `PRIMARY KEY` `NOT NULL`
- `user_id` `INT` `FK -> users.user_id`
- `item_id` `INT` `FK -> item.item_id`
- `quantity` `INT` `NOT NULL`
- `acquired_at` `DATETIME` `NULL`

Notes:
- Unique pair on `(user_id, item_id)`.
- Exposed through `GET /users/{id}/inventory`.
- New users receive starter inventory rows automatically on first session provisioning.

## Relationships
Currently documented relationships:
- `weapon.item_id` -> `item.item_id`
- `armor.item_id` -> `item.item_id`
- `consumable.item_id` -> `item.item_id`
- `currency.item_id` -> `item.item_id`
- `material.item_id` -> `item.item_id`
- `user_skin.user_id` -> `users.user_id`
- `user_skin.skin_id` -> `skin.skin_id`
- `item_skin.item_id` -> `item.item_id`
- `item_skin.skin_id` -> `skin.skin_id`
- `user_inventory.user_id` -> `users.user_id`
- `user_inventory.item_id` -> `item.item_id`

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
