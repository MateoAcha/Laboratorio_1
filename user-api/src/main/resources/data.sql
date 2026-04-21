INSERT INTO item (item_id, item_name, item_type, rarity, description) VALUES
    (1001, 'Starter Spear', 'Weapon', 'Common', 'A reliable spear with reach that better matches the player attack style.'),
    (1002, 'Training Vest', 'Armor', 'Common', 'Basic armor with enough padding for early matches.'),
    (1003, 'Health Potion', 'Consumable', 'Uncommon', 'Restores vitality during long sessions.'),
    (1004, 'Gold Coins', 'Currency', 'Common', 'Standard soft currency used in the shop.'),
    (1005, 'Iron Ore', 'Material', 'Common', 'A crafting material used for simple upgrades.')
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO weapon (item_id, damage, accuracy, range, fire_rate, ammo_type) VALUES
    (1001, 12, 0.92, 1.8, NULL, NULL)
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO armor (item_id, defense, durability, weight) VALUES
    (1002, 8, 45, 3.5)
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO consumable (item_id, effect_description, duration_seconds, cooldown_seconds) VALUES
    (1003, 'Restores 35 HP instantly.', NULL, 12)
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO currency (item_id, currency_code, is_tradeable) VALUES
    (1004, 'GOLD', TRUE)
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO material (item_id, material_grade) VALUES
    (1005, 'Refined')
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO item (item_id, item_name, item_type, rarity, description) VALUES
    (1006, 'Iron Spear',    'Weapon',     'Uncommon', 'A heavier spear forged from iron. More reach and power than the starter.'),
    (1007, 'Shadow Lance',  'Weapon',     'Rare',     'A lance imbued with dark energy. Exceptional damage and range.'),
    (1008, 'Chain Mail',    'Armor',      'Uncommon', 'Interlocked iron rings offering solid protection without much weight.'),
    (1009, 'Dragon Scale',  'Armor',      'Rare',     'Scales harvested from a dragon. The finest armor available in the shop.'),
    (1010, 'Max Potion',    'Consumable', 'Rare',     'A powerful elixir that restores a large amount of health instantly.')
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO weapon (item_id, damage, accuracy, range, fire_rate, ammo_type) VALUES
    (1006, 20, 0.90, 2.2, NULL, NULL),
    (1007, 30, 0.95, 2.8, NULL, NULL)
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO armor (item_id, defense, durability, weight) VALUES
    (1008, 15, 70,  5.0),
    (1009, 25, 100, 8.5)
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO consumable (item_id, effect_description, duration_seconds, cooldown_seconds) VALUES
    (1010, 'Restores 80 HP instantly.', NULL, 20)
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO shop_item (shop_item_id, item_id, gold_price, purchase_quantity, is_available) VALUES
    (1, 1006, 150, 1, TRUE),
    (2, 1007, 400, 1, TRUE),
    (3, 1008, 200, 1, TRUE),
    (4, 1009, 500, 1, TRUE),
    (5, 1010,  50, 3, TRUE)
ON CONFLICT (shop_item_id) DO NOTHING;

INSERT INTO item (item_id, item_name, item_type, rarity, description) VALUES
    (1011, 'Long Spear',        'Weapon',     'Common',   'A spear with extended reach, trading striking power for range.'),
    (1012, 'Reinforced Armor',  'Armor',      'Uncommon', 'Heavy plating that significantly reduces incoming damage.'),
    (1013, 'Swift Elixir',      'Consumable', 'Uncommon', 'A potion that grants a burst of speed for 3 seconds when used.')
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO weapon (item_id, damage, accuracy, range, fire_rate, ammo_type) VALUES
    (1011, 6, 0.80, 3.5, NULL, NULL)
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO armor (item_id, defense, durability, weight) VALUES
    (1012, 18, 75, 4.0)
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO consumable (item_id, effect_description, duration_seconds, cooldown_seconds) VALUES
    (1013, 'Speed Boost', 3, 15)
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO shop_item (shop_item_id, item_id, gold_price, purchase_quantity, is_available) VALUES
    (6, 1011, 100, 1, TRUE),
    (7, 1012, 180, 1, TRUE),
    (8, 1013,  75, 3, TRUE)
ON CONFLICT (shop_item_id) DO NOTHING;

INSERT INTO skin (skin_id, skin_name, rarity, created_at) VALUES
    (2001, 'Crimson Edge', 'Rare', NOW()),
    (2002, 'Field Green', 'Common', NOW())
ON CONFLICT (skin_id) DO NOTHING;

INSERT INTO item_skin (item_id, skin_id) VALUES
    (1001, 2001),
    (1002, 2002)
ON CONFLICT (item_id, skin_id) DO NOTHING;
