CREATE TABLE IF NOT EXISTS users (
    user_id INT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    is_premium BOOLEAN NOT NULL DEFAULT FALSE,
    premium_since TIMESTAMP NULL,
    premium_until TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_daily_coins_claimed_at TIMESTAMP NULL,
    password VARCHAR(255) NOT NULL
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS last_daily_coins_claimed_at TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS session_token VARCHAR(255) NULL;

ALTER TABLE IF EXISTS player_stats ADD COLUMN IF NOT EXISTS giant_enemies_killed INT NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS player_stats ADD COLUMN IF NOT EXISTS total_xp BIGINT NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS player_stats ADD COLUMN IF NOT EXISTS level INT NOT NULL DEFAULT 1;
ALTER TABLE IF EXISTS player_stats ADD COLUMN IF NOT EXISTS unspent_skill_points INT NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS player_stats ADD COLUMN IF NOT EXISTS spent_skill_points INT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS item (
    item_id INT PRIMARY KEY,
    item_name VARCHAR(255) NOT NULL,
    item_type VARCHAR(64) NOT NULL,
    rarity VARCHAR(64) NOT NULL,
    description TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS weapon (
    item_id INT PRIMARY KEY REFERENCES item(item_id) ON DELETE CASCADE,
    damage REAL NOT NULL,
    accuracy REAL NOT NULL,
    range REAL NOT NULL,
    fire_rate REAL NULL,
    ammo_type VARCHAR(128) NULL,
    weapon_type VARCHAR(32) NULL,
    weapon_color VARCHAR(9) NULL,
    CONSTRAINT ck_weapon_weapon_type CHECK (weapon_type IS NULL OR weapon_type IN ('Spear', 'Sword', 'Ranged')),
    CONSTRAINT ck_weapon_weapon_color CHECK (weapon_color IS NULL OR weapon_color ~ '^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$')
);

ALTER TABLE weapon ADD COLUMN IF NOT EXISTS weapon_type VARCHAR(32) NULL CHECK (weapon_type IS NULL OR weapon_type IN ('Spear', 'Sword', 'Ranged'));
ALTER TABLE weapon ADD COLUMN IF NOT EXISTS weapon_color VARCHAR(9) NULL CHECK (weapon_color IS NULL OR weapon_color ~ '^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$');

CREATE TABLE IF NOT EXISTS armor (
    item_id INT PRIMARY KEY REFERENCES item(item_id) ON DELETE CASCADE,
    defense REAL NOT NULL,
    durability REAL NOT NULL,
    weight REAL NULL
);

CREATE TABLE IF NOT EXISTS consumable (
    item_id INT PRIMARY KEY REFERENCES item(item_id) ON DELETE CASCADE,
    effect_description TEXT NOT NULL,
    duration_seconds INT NULL,
    cooldown_seconds INT NULL
);

CREATE TABLE IF NOT EXISTS currency (
    item_id INT PRIMARY KEY REFERENCES item(item_id) ON DELETE CASCADE,
    currency_code VARCHAR(64) NOT NULL,
    is_tradeable BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS material (
    item_id INT PRIMARY KEY REFERENCES item(item_id) ON DELETE CASCADE,
    material_key VARCHAR(128) NULL,
    material_grade VARCHAR(64) NULL
);

ALTER TABLE material ADD COLUMN IF NOT EXISTS material_key VARCHAR(128) NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_material_material_key ON material (material_key) WHERE material_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS skin (
    skin_id INT PRIMARY KEY,
    skin_name VARCHAR(255) NOT NULL,
    rarity VARCHAR(64) NOT NULL,
    skin_color VARCHAR(9) NULL CHECK (skin_color IS NULL OR skin_color ~ '^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$'),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE skin ADD COLUMN IF NOT EXISTS skin_color VARCHAR(9) NULL CHECK (skin_color IS NULL OR skin_color ~ '^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$');

CREATE TABLE IF NOT EXISTS user_skin (
    user_skin_id INT PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    skin_id INT NOT NULL REFERENCES skin(skin_id) ON DELETE CASCADE,
    unlocked_at TIMESTAMP NOT NULL DEFAULT NOW(),
    unlock_source VARCHAR(255) NULL,
    UNIQUE (user_id, skin_id)
);

CREATE TABLE IF NOT EXISTS item_skin (
    item_id INT NOT NULL REFERENCES item(item_id) ON DELETE CASCADE,
    skin_id INT NOT NULL REFERENCES skin(skin_id) ON DELETE CASCADE,
    PRIMARY KEY (item_id, skin_id)
);

CREATE SEQUENCE IF NOT EXISTS user_inventory_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS user_inventory (
    user_inventory_id INT PRIMARY KEY DEFAULT nextval('user_inventory_seq'),
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    item_id INT NOT NULL REFERENCES item(item_id) ON DELETE CASCADE,
    quantity INT NOT NULL CHECK (quantity >= 0),
    acquired_at TIMESTAMP NULL,
    is_equipped BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (user_id, item_id)
);

ALTER TABLE user_inventory ADD COLUMN IF NOT EXISTS is_equipped BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS user_skill (
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    skill_id VARCHAR(128) NOT NULL,
    skill_level INT NOT NULL DEFAULT 0 CHECK (skill_level >= 0),
    unlocked_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, skill_id)
);

CREATE TABLE IF NOT EXISTS user_skill_loadout (
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    branch VARCHAR(32) NOT NULL,
    slot_kind VARCHAR(32) NOT NULL,
    skill_id VARCHAR(128) NOT NULL,
    equipped_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, branch, slot_kind),
    FOREIGN KEY (user_id, skill_id) REFERENCES user_skill(user_id, skill_id) ON DELETE CASCADE
);

INSERT INTO item (item_id, item_name, item_type, rarity, description) VALUES
    (1001, 'Windrunner Spear', 'Weapon', 'Common', 'A storm-blue spear inspired by Windrunner bridge crews and quick aerial strikes.'),
    (1002, 'Training Vest', 'Armor', 'Common', 'Basic armor with enough padding for early matches.'),
    (1003, 'Health Potion', 'Consumable', 'Uncommon', 'Restores vitality during long sessions.'),
    (1004, 'Gold Coins', 'Currency', 'Common', 'Standard soft currency used in the shop.'),
    (1005, 'Iron Ore', 'Material', 'Common', 'A crafting material used for simple upgrades.')
ON CONFLICT (item_id) DO NOTHING;

UPDATE item SET item_name = 'Windrunner Spear', description = 'A storm-blue spear inspired by Windrunner bridge crews and quick aerial strikes.' WHERE item_id = 1001;

INSERT INTO weapon (item_id, damage, accuracy, range, fire_rate, ammo_type, weapon_type, weapon_color) VALUES
    (1001, 12, 0.92, 1.8, NULL, NULL, 'Spear', '#4CC9F0')
ON CONFLICT (item_id) DO NOTHING;

UPDATE weapon SET weapon_type = 'Spear', weapon_color = '#4CC9F0' WHERE item_id = 1001;

INSERT INTO armor (item_id, defense, durability, weight) VALUES
    (1002, 8, 45, 3.5)
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO consumable (item_id, effect_description, duration_seconds, cooldown_seconds) VALUES
    (1003, 'Restores 35 HP instantly.', NULL, 12)
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO currency (item_id, currency_code, is_tradeable) VALUES
    (1004, 'GOLD', TRUE)
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO material (item_id, material_key, material_grade) VALUES
    (1005, 'iron_ore', 'Refined')
ON CONFLICT (item_id) DO NOTHING;

UPDATE material SET material_key = 'iron_ore' WHERE item_id = 1005 AND material_key IS NULL;

INSERT INTO skin (skin_id, skin_name, rarity, skin_color, created_at) VALUES
    (2001, 'Crimson Edge', 'Rare', '#D90429', NOW()),
    (2002, 'Field Green', 'Common', '#2A9D8F', NOW())
ON CONFLICT (skin_id) DO NOTHING;

UPDATE skin SET skin_color = '#D90429' WHERE skin_id = 2001;
UPDATE skin SET skin_color = '#2A9D8F' WHERE skin_id = 2002;

INSERT INTO item_skin (item_id, skin_id) VALUES
    (1001, 2001),
    (1002, 2002)
ON CONFLICT (item_id, skin_id) DO NOTHING;
