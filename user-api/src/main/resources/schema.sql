CREATE TABLE IF NOT EXISTS users (
    user_id INT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    is_premium BOOLEAN NOT NULL DEFAULT FALSE,
    premium_since TIMESTAMP NULL,
    premium_until TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    password VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_username ON users (username);
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email ON users (email);

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
    ammo_type VARCHAR(128) NULL
);

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
    material_grade VARCHAR(64) NULL
);

CREATE TABLE IF NOT EXISTS skin (
    skin_id INT PRIMARY KEY,
    skin_name VARCHAR(255) NOT NULL,
    rarity VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

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

CREATE SEQUENCE IF NOT EXISTS user_inventory_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS user_inventory (
    user_inventory_id INT PRIMARY KEY DEFAULT nextval('user_inventory_seq'),
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    item_id INT NOT NULL REFERENCES item(item_id) ON DELETE CASCADE,
    quantity INT NOT NULL CHECK (quantity >= 0),
    acquired_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_inventory_user_item ON user_inventory (user_id, item_id);

CREATE TABLE IF NOT EXISTS shop_item (
    shop_item_id INT PRIMARY KEY,
    item_id INT NOT NULL REFERENCES item(item_id) ON DELETE CASCADE,
    gold_price INT NOT NULL CHECK (gold_price >= 0),
    purchase_quantity INT NOT NULL DEFAULT 1 CHECK (purchase_quantity > 0),
    is_available BOOLEAN NOT NULL DEFAULT TRUE
);
