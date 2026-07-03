CREATE TABLE IF NOT EXISTS users (
    user_id INT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    is_premium BOOLEAN NOT NULL DEFAULT FALSE,
    premium_since TIMESTAMP NULL,
    premium_until TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_daily_coins_claimed_at TIMESTAMP NULL,
    account_type VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
    google_subject VARCHAR(255) NULL,
    password VARCHAR(255) NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_username ON users (username);
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email ON users (email);

ALTER TABLE users ADD COLUMN IF NOT EXISTS last_daily_coins_claimed_at TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS account_type VARCHAR(32) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_subject VARCHAR(255) NULL;
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_google_subject ON users (google_subject) WHERE google_subject IS NOT NULL;

ALTER TABLE users ADD COLUMN IF NOT EXISTS session_token VARCHAR(255) NULL;

ALTER TABLE IF EXISTS player_stats ADD COLUMN IF NOT EXISTS giant_enemies_killed INT NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS player_stats ADD COLUMN IF NOT EXISTS total_xp BIGINT NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS player_stats ADD COLUMN IF NOT EXISTS level INT NOT NULL DEFAULT 1;
ALTER TABLE IF EXISTS player_stats ADD COLUMN IF NOT EXISTS unspent_skill_points INT NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS player_stats ADD COLUMN IF NOT EXISTS spent_skill_points INT NOT NULL DEFAULT 0;

CREATE SEQUENCE IF NOT EXISTS friendship_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS friendship (
    friendship_id BIGINT PRIMARY KEY DEFAULT nextval('friendship_seq'),
    user_one_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    user_two_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_friendship_order CHECK (user_one_id < user_two_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_friendship_pair ON friendship (user_one_id, user_two_id);
CREATE INDEX IF NOT EXISTS ix_friendship_user_one ON friendship (user_one_id);
CREATE INDEX IF NOT EXISTS ix_friendship_user_two ON friendship (user_two_id);

CREATE SEQUENCE IF NOT EXISTS friend_request_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS friend_request (
    request_id BIGINT PRIMARY KEY DEFAULT nextval('friend_request_seq'),
    requester_user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    recipient_user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    responded_at TIMESTAMP NULL,
    canceled_at TIMESTAMP NULL,
    CONSTRAINT ck_friend_request_not_self CHECK (requester_user_id <> recipient_user_id),
    CONSTRAINT ck_friend_request_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_friend_request_pending_direction
    ON friend_request (requester_user_id, recipient_user_id)
    WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS ix_friend_request_recipient_status
    ON friend_request (recipient_user_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_friend_request_requester_status
    ON friend_request (requester_user_id, status, created_at DESC);

CREATE SEQUENCE IF NOT EXISTS game_invite_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS game_invite (
    invite_id BIGINT PRIMARY KEY DEFAULT nextval('game_invite_seq'),
    host_user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    recipient_user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    room_number INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    responded_at TIMESTAMP NULL,
    canceled_at TIMESTAMP NULL,
    CONSTRAINT ck_game_invite_not_self CHECK (host_user_id <> recipient_user_id),
    CONSTRAINT ck_game_invite_room CHECK (room_number > 0),
    CONSTRAINT ck_game_invite_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'CANCELED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_game_invite_pending_room
    ON game_invite (host_user_id, recipient_user_id, room_number)
    WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS ix_game_invite_recipient_status_expires
    ON game_invite (recipient_user_id, status, expires_at);
CREATE INDEX IF NOT EXISTS ix_game_invite_host_room_status
    ON game_invite (host_user_id, room_number, status);

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
    unlock_source VARCHAR(255) NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_skin_user_skin ON user_skin (user_id, skin_id);

CREATE TABLE IF NOT EXISTS item_skin (
    item_id INT NOT NULL REFERENCES item(item_id) ON DELETE CASCADE,
    skin_id INT NOT NULL REFERENCES skin(skin_id) ON DELETE CASCADE,
    PRIMARY KEY (item_id, skin_id)
);

CREATE SEQUENCE IF NOT EXISTS user_skin_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE user_skin ADD COLUMN IF NOT EXISTS is_equipped BOOLEAN NOT NULL DEFAULT FALSE;

CREATE SEQUENCE IF NOT EXISTS user_inventory_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS user_inventory (
    user_inventory_id INT PRIMARY KEY DEFAULT nextval('user_inventory_seq'),
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    item_id INT NOT NULL REFERENCES item(item_id) ON DELETE CASCADE,
    quantity INT NOT NULL CHECK (quantity >= 0),
    acquired_at TIMESTAMP NULL,
    is_equipped BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_inventory_user_item ON user_inventory (user_id, item_id);
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

CREATE TABLE IF NOT EXISTS user_challenge_claim (
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    challenge_id INT NOT NULL,
    challenge_key VARCHAR(128) NOT NULL,
    reward_coins INT NOT NULL DEFAULT 0 CHECK (reward_coins >= 0),
    claimed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, challenge_id)
);

CREATE TABLE IF NOT EXISTS shop_item (
    shop_item_id INT PRIMARY KEY,
    item_id INT NOT NULL REFERENCES item(item_id) ON DELETE CASCADE,
    gold_price INT NOT NULL CHECK (gold_price >= 0),
    purchase_quantity INT NOT NULL DEFAULT 1 CHECK (purchase_quantity > 0),
    is_available BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE shop_item ADD COLUMN IF NOT EXISTS skin_id INT REFERENCES skin(skin_id);
