INSERT INTO item (item_id, item_name, item_type, rarity, description) VALUES
    (1001, 'Starter Spear', 'Weapon', 'Common', 'A reliable spear with reach that better matches the player attack style.'),
    (1002, 'Training Vest', 'Armor', 'Common', 'Basic armor with enough padding for early matches.'),
    (1003, 'Health Potion', 'Consumable', 'Uncommon', 'Restores vitality during long sessions.'),
    (1004, 'Gold Coins', 'Currency', 'Common', 'Standard soft currency used in the shop.'),
    (1005, 'Iron Ore', 'Material', 'Common', 'A crafting material used for simple upgrades.')
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO weapon (item_id, damage, accuracy, range, fire_rate, ammo_type, weapon_type, weapon_color) VALUES
    (1001, 12, 0.92, 1.8, NULL, NULL, 'Spear', '#4CC9F0')
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

INSERT INTO weapon (item_id, damage, accuracy, range, fire_rate, ammo_type, weapon_type, weapon_color) VALUES
    (1006, 20, 0.90, 2.2, NULL, NULL, 'Spear', '#F4A261'),
    (1007, 30, 0.95, 2.8, NULL, NULL, 'Spear', '#9B5DE5')
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

INSERT INTO weapon (item_id, damage, accuracy, range, fire_rate, ammo_type, weapon_type, weapon_color) VALUES
    (1011, 6, 0.80, 3.5, NULL, NULL, 'Spear', '#2A9D8F')
ON CONFLICT (item_id) DO NOTHING;

UPDATE weapon SET weapon_type = 'Spear', weapon_color = '#4CC9F0' WHERE item_id = 1001;
UPDATE weapon SET weapon_type = 'Spear', weapon_color = '#F4A261' WHERE item_id = 1006;
UPDATE weapon SET weapon_type = 'Spear', weapon_color = '#9B5DE5' WHERE item_id = 1007;
UPDATE weapon SET weapon_type = 'Spear', weapon_color = '#2A9D8F' WHERE item_id = 1011;

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

INSERT INTO skin (skin_id, skin_name, rarity, skin_color, created_at) VALUES
    (2000, 'Default Blue', 'Common', '#4CC9F0', NOW()),
    (2001, 'Crimson Edge', 'Rare', '#D90429', NOW()),
    (2002, 'Field Green', 'Common', '#2A9D8F', NOW())
ON CONFLICT (skin_id) DO NOTHING;

UPDATE skin SET skin_color = '#4CC9F0' WHERE skin_id = 2000;
UPDATE skin SET skin_color = '#D90429' WHERE skin_id = 2001;
UPDATE skin SET skin_color = '#2A9D8F' WHERE skin_id = 2002;

INSERT INTO item_skin (item_id, skin_id) VALUES
    (1001, 2001),
    (1002, 2002)
ON CONFLICT (item_id, skin_id) DO NOTHING;

INSERT INTO item (item_id, item_name, item_type, rarity, description) VALUES
    (1016, 'Crimson Edge Skin', 'Skin', 'Rare',   'A battle-worn crimson skin that marks a fearless warrior.'),
    (1017, 'Field Green Skin',  'Skin', 'Common', 'A muted field-operative look for blending into the fray.')
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO shop_item (shop_item_id, item_id, gold_price, purchase_quantity, is_available, skin_id) VALUES
    (9,  1016, 300, 1, TRUE, 2001),
    (10, 1017, 100, 1, TRUE, 2002)
ON CONFLICT (shop_item_id) DO NOTHING;

INSERT INTO skin (skin_id, skin_name, rarity, skin_color, created_at) VALUES
    (2003, 'Skin 1', 'Common',   '#FFB703', NOW()),
    (2004, 'Skin 2', 'Uncommon', '#219EBC', NOW()),
    (2005, 'Skin 3', 'Rare',     '#8338EC', NOW()),
    (2006, 'Skin 4', 'Epic',     '#FB5607', NOW())
ON CONFLICT (skin_id) DO NOTHING;

UPDATE skin SET skin_color = '#FFB703' WHERE skin_id = 2003;
UPDATE skin SET skin_color = '#219EBC' WHERE skin_id = 2004;
UPDATE skin SET skin_color = '#8338EC' WHERE skin_id = 2005;
UPDATE skin SET skin_color = '#FB5607' WHERE skin_id = 2006;

INSERT INTO item (item_id, item_name, item_type, rarity, description) VALUES
    (1026, 'Skin 1', 'Skin', 'Common',   'A simple character skin with a distinct color.'),
    (1027, 'Skin 2', 'Skin', 'Uncommon', 'A simple character skin with a distinct color.'),
    (1028, 'Skin 3', 'Skin', 'Rare',     'A simple character skin with a distinct color.'),
    (1029, 'Skin 4', 'Skin', 'Epic',     'A simple character skin with a distinct color.')
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO shop_item (shop_item_id, item_id, gold_price, purchase_quantity, is_available, skin_id) VALUES
    (19, 1026, 100, 1, TRUE, 2003),
    (20, 1027, 200, 1, TRUE, 2004),
    (21, 1028, 350, 1, TRUE, 2005),
    (22, 1029, 500, 1, TRUE, 2006)
ON CONFLICT (shop_item_id) DO NOTHING;

INSERT INTO item (item_id, item_name, item_type, rarity, description) VALUES
    (1018, 'Sword 1',  'Weapon', 'Common',    'A basic sword with low damage and short reach.'),
    (1019, 'Sword 2',  'Weapon', 'Uncommon',  'A decent sword with balanced damage and accuracy.'),
    (1020, 'Sword 3',  'Weapon', 'Rare',      'A good sword with strong damage and reliable handling.'),
    (1021, 'Sword 4',  'Weapon', 'Epic',      'A really good sword with excellent close-range power.'),
    (1022, 'Ranged 1', 'Weapon', 'Common',    'A basic ranged weapon with low damage.'),
    (1023, 'Ranged 2', 'Weapon', 'Uncommon',  'A decent ranged weapon with improved accuracy.'),
    (1024, 'Ranged 3', 'Weapon', 'Rare',      'A good ranged weapon with strong reach and damage.'),
    (1025, 'Ranged 4', 'Weapon', 'Epic',      'A really good ranged weapon with a very high fire rate.')
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO weapon (item_id, damage, accuracy, range, fire_rate, ammo_type, weapon_type, weapon_color) VALUES
    (1018, 8,  0.78, 1.2, NULL, NULL,     'Sword',  '#8D99AE'),
    (1019, 14, 0.86, 1.35, NULL, NULL,    'Sword',  '#E76F51'),
    (1020, 22, 0.92, 1.5, NULL, NULL,     'Sword',  '#FFD166'),
    (1021, 34, 0.96, 1.65, NULL, NULL,    'Sword',  '#EF476F'),
    (1022, 5,  0.72, 4.0, 0.8, 'Bolt',    'Ranged', '#6C757D'),
    (1023, 10, 0.82, 5.0, 1.2, 'Bolt',    'Ranged', '#06D6A0'),
    (1024, 17, 0.90, 6.0, 1.8, 'Bolt',    'Ranged', '#118AB2'),
    (1025, 24, 0.94, 6.5, 6.0, 'Crystal', 'Ranged', '#F72585')
ON CONFLICT (item_id) DO NOTHING;

UPDATE weapon SET weapon_type = 'Sword', weapon_color = '#8D99AE' WHERE item_id = 1018;
UPDATE weapon SET weapon_type = 'Sword', weapon_color = '#E76F51' WHERE item_id = 1019;
UPDATE weapon SET weapon_type = 'Sword', weapon_color = '#FFD166' WHERE item_id = 1020;
UPDATE weapon SET weapon_type = 'Sword', weapon_color = '#EF476F' WHERE item_id = 1021;
UPDATE weapon SET weapon_type = 'Ranged', weapon_color = '#6C757D' WHERE item_id = 1022;
UPDATE weapon SET weapon_type = 'Ranged', weapon_color = '#06D6A0' WHERE item_id = 1023;
UPDATE weapon SET weapon_type = 'Ranged', weapon_color = '#118AB2' WHERE item_id = 1024;
UPDATE weapon SET weapon_type = 'Ranged', weapon_color = '#F72585' WHERE item_id = 1025;

INSERT INTO shop_item (shop_item_id, item_id, gold_price, purchase_quantity, is_available) VALUES
    (11, 1018,  60, 1, TRUE),
    (12, 1019, 140, 1, TRUE),
    (13, 1020, 280, 1, TRUE),
    (14, 1021, 550, 1, TRUE),
    (15, 1022,  70, 1, TRUE),
    (16, 1023, 160, 1, TRUE),
    (17, 1024, 320, 1, TRUE),
    (18, 1025, 650, 1, TRUE)
ON CONFLICT (shop_item_id) DO NOTHING;
