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

UPDATE material SET material_key = 'iron_ore' WHERE item_id = 1005 AND material_key IS NULL;

INSERT INTO item (item_id, item_name, item_type, rarity, description) VALUES
    (1006, 'Alethi War Spear', 'Weapon',  'Uncommon', 'A disciplined spear built for brightlord formations and steady reach.'),
    (1007, 'Voidlight Lance',  'Weapon',  'Rare',     'A violet lance with ominous Voidlight styling and exceptional range.'),
    (1008, 'Chain Mail',    'Armor',      'Uncommon', 'Interlocked iron rings offering solid protection without much weight.'),
    (1009, 'Dragon Scale',  'Armor',      'Rare',     'Scales harvested from a dragon. The finest armor available in the shop.'),
    (1010, 'Max Potion',    'Consumable', 'Rare',     'A powerful elixir that restores a large amount of health instantly.')
ON CONFLICT (item_id) DO NOTHING;

UPDATE item SET item_name = 'Alethi War Spear', description = 'A disciplined spear built for brightlord formations and steady reach.' WHERE item_id = 1006;
UPDATE item SET item_name = 'Voidlight Lance', description = 'A violet lance with ominous Voidlight styling and exceptional range.' WHERE item_id = 1007;

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
    (1030, 'Green Fields Material', 'Material', 'Rare', 'A map material collected from giants in Green Fields.'),
    (1031, 'Ash Basin Material',    'Material', 'Rare', 'A map material collected from giants in Ash Basin.'),
    (1032, 'Moon Marsh Material',   'Material', 'Rare', 'A map material collected from giants in Moon Marsh.')
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO material (item_id, material_key, material_grade) VALUES
    (1030, 'green_fields_material', 'Rare'),
    (1031, 'ash_basin_material', 'Rare'),
    (1032, 'moon_marsh_material', 'Rare')
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO item (item_id, item_name, item_type, rarity, description) VALUES
    (3000, 'Emeralds', 'Currency', 'Rare', 'Premium currency purchased with real money.')
ON CONFLICT (item_id) DO NOTHING;

INSERT INTO currency (item_id, currency_code, is_tradeable) VALUES
    (3000, 'EMERALD', FALSE)
ON CONFLICT (item_id) DO NOTHING;

UPDATE material SET material_key = 'green_fields_material', material_grade = 'Rare' WHERE item_id = 1030;
UPDATE material SET material_key = 'ash_basin_material', material_grade = 'Rare' WHERE item_id = 1031;
UPDATE material SET material_key = 'moon_marsh_material', material_grade = 'Rare' WHERE item_id = 1032;

INSERT INTO item (item_id, item_name, item_type, rarity, description) VALUES
    (1011, 'Shattered Plains Spear', 'Weapon', 'Common', 'A long spear made for duels across broken stone and wide chasms.'),
    (1012, 'Reinforced Armor',  'Armor',      'Uncommon', 'Heavy plating that significantly reduces incoming damage.'),
    (1013, 'Swift Elixir',      'Consumable', 'Uncommon', 'A potion that grants a burst of speed for 3 seconds when used.')
ON CONFLICT (item_id) DO NOTHING;

UPDATE item SET item_name = 'Shattered Plains Spear', description = 'A long spear made for duels across broken stone and wide chasms.' WHERE item_id = 1011;

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
    (2003, 'Windrunner Shardplate', 'Common',   '#4CC9F0', NOW()),
    (2004, 'Skybreaker Shardplate', 'Uncommon', '#2B2D42', NOW())
ON CONFLICT (skin_id) DO NOTHING;

UPDATE skin SET skin_name = 'Windrunner Shardplate', skin_color = '#4CC9F0' WHERE skin_id = 2003;
UPDATE skin SET skin_name = 'Skybreaker Shardplate', skin_color = '#2B2D42' WHERE skin_id = 2004;

INSERT INTO item (item_id, item_name, item_type, rarity, description) VALUES
    (1026, 'Windrunner Shardplate', 'Skin', 'Common',   'A storm-blue armor finish inspired by Windrunner Shardplate.'),
    (1027, 'Skybreaker Shardplate', 'Skin', 'Uncommon', 'A smoke-dark armor finish inspired by Skybreaker Shardplate.')
ON CONFLICT (item_id) DO NOTHING;

UPDATE item SET item_name = 'Windrunner Shardplate', description = 'A storm-blue armor finish inspired by Windrunner Shardplate.' WHERE item_id = 1026;
UPDATE item SET item_name = 'Skybreaker Shardplate', description = 'A smoke-dark armor finish inspired by Skybreaker Shardplate.' WHERE item_id = 1027;

INSERT INTO shop_item (shop_item_id, item_id, gold_price, purchase_quantity, is_available, skin_id) VALUES
    (19, 1026, 100, 1, TRUE, 2003),
    (20, 1027, 200, 1, TRUE, 2004)
ON CONFLICT (shop_item_id) DO NOTHING;

-- Keep the seeded skin catalog capped at skin_id 2000 through 2004.
DELETE FROM shop_item WHERE shop_item_id IN (21, 22) OR item_id IN (1028, 1029) OR skin_id IN (2005, 2006);
DELETE FROM item WHERE item_id IN (1028, 1029);
DELETE FROM skin WHERE skin_id IN (2005, 2006);

INSERT INTO item (item_id, item_name, item_type, rarity, description) VALUES
    (1018, 'Alethi Side Sword',       'Weapon', 'Common',   'A practical sword carried by soldiers who expect the duel to get close.'),
    (1019, 'Sunraiser Shardblade',    'Weapon', 'Uncommon', 'A bright sword inspired by the named Shardblade Sunraiser.'),
    (1020, 'Oathbringer Shardblade',  'Weapon', 'Rare',     'A powerful sword inspired by the legendary Shardblade Oathbringer.'),
    (1021, 'Nightblood Sword',        'Weapon', 'Epic',     'A black sword inspired by the terrifying awakened blade Nightblood.'),
    (1022, 'Stormlight Power Orb',    'Weapon', 'Common',   'A simple orb spell that hurls compact bursts of Stormlight at range.'),
    (1023, 'Gravitation Surge Orb',   'Weapon', 'Uncommon', 'A lashing spell orb that pulls power into a focused ranged strike.'),
    (1024, 'Voidlight Power Orb',     'Weapon', 'Rare',     'A violet power orb that releases precise Voidlight pulses from afar.'),
    (1025, 'Highstorm Nova Orb',      'Weapon', 'Epic',     'An epic orb spell that erupts in rapid stormlit blasts.')
ON CONFLICT (item_id) DO NOTHING;

UPDATE item SET item_name = 'Alethi Side Sword', description = 'A practical sword carried by soldiers who expect the duel to get close.' WHERE item_id = 1018;
UPDATE item SET item_name = 'Sunraiser Shardblade', description = 'A bright sword inspired by the named Shardblade Sunraiser.' WHERE item_id = 1019;
UPDATE item SET item_name = 'Oathbringer Shardblade', description = 'A powerful sword inspired by the legendary Shardblade Oathbringer.' WHERE item_id = 1020;
UPDATE item SET item_name = 'Nightblood Sword', description = 'A black sword inspired by the terrifying awakened blade Nightblood.' WHERE item_id = 1021;
UPDATE item SET item_name = 'Stormlight Power Orb', description = 'A simple orb spell that hurls compact bursts of Stormlight at range.' WHERE item_id = 1022;
UPDATE item SET item_name = 'Gravitation Surge Orb', description = 'A lashing spell orb that pulls power into a focused ranged strike.' WHERE item_id = 1023;
UPDATE item SET item_name = 'Voidlight Power Orb', description = 'A violet power orb that releases precise Voidlight pulses from afar.' WHERE item_id = 1024;
UPDATE item SET item_name = 'Highstorm Nova Orb', description = 'An epic orb spell that erupts in rapid stormlit blasts.' WHERE item_id = 1025;

INSERT INTO weapon (item_id, damage, accuracy, range, fire_rate, ammo_type, weapon_type, weapon_color) VALUES
    (1018, 8,  0.78, 1.2, NULL, NULL,     'Sword',  '#8D99AE'),
    (1019, 14, 0.86, 1.35, NULL, NULL,    'Sword',  '#E9C46A'),
    (1020, 22, 0.92, 1.5, NULL, NULL,     'Sword',  '#FFD166'),
    (1021, 34, 0.96, 1.65, NULL, NULL,    'Sword',  '#0B0F19'),
    (1022, 5,  0.72, 4.0, 0.8, 'Stormlight', 'Ranged', '#6C757D'),
    (1023, 10, 0.82, 5.0, 1.2, 'Surge',      'Ranged', '#06D6A0'),
    (1024, 17, 0.90, 6.0, 1.8, 'Voidlight',  'Ranged', '#118AB2'),
    (1025, 24, 0.94, 6.5, 6.0, 'Highstorm',  'Ranged', '#F72585')
ON CONFLICT (item_id) DO NOTHING;

UPDATE weapon SET weapon_type = 'Sword', weapon_color = '#8D99AE' WHERE item_id = 1018;
UPDATE weapon SET weapon_type = 'Sword', weapon_color = '#E9C46A' WHERE item_id = 1019;
UPDATE weapon SET weapon_type = 'Sword', weapon_color = '#FFD166' WHERE item_id = 1020;
UPDATE weapon SET weapon_type = 'Sword', weapon_color = '#0B0F19' WHERE item_id = 1021;
UPDATE weapon SET weapon_type = 'Ranged', ammo_type = 'Stormlight', weapon_color = '#6C757D' WHERE item_id = 1022;
UPDATE weapon SET weapon_type = 'Ranged', ammo_type = 'Surge', weapon_color = '#06D6A0' WHERE item_id = 1023;
UPDATE weapon SET weapon_type = 'Ranged', ammo_type = 'Voidlight', weapon_color = '#118AB2' WHERE item_id = 1024;
UPDATE weapon SET weapon_type = 'Ranged', ammo_type = 'Highstorm', weapon_color = '#F72585' WHERE item_id = 1025;

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
