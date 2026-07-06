package user_api;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final JdbcTemplate jdbcTemplate;

    public InventoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureStarterInventory(Integer userId) {
        if (userId == null) {
            return;
        }

        Number userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE user_id = ?",
                Number.class,
                userId);
        if (userCount == null || userCount.intValue() <= 0) {
            return;
        }

        jdbcTemplate.update(
                """
                INSERT INTO user_inventory (user_inventory_id, user_id, item_id, quantity, acquired_at)
                SELECT nextval('user_inventory_seq'), ?, seed.item_id, seed.quantity, NOW()
                FROM (VALUES
                    (1001, 1),
                    (1002, 1),
                    (1003, 5),
                    (1004, 250),
                    (1005, 7)
                ) AS seed(item_id, quantity)
                ON CONFLICT (user_id, item_id) DO NOTHING
                """,
                userId);
    }

    public void addCoins(Integer userId, int quantity) {
        if (userId == null || quantity <= 0) {
            return;
        }

        jdbcTemplate.update(
                """
                INSERT INTO user_inventory (user_inventory_id, user_id, item_id, quantity, acquired_at)
                VALUES (nextval('user_inventory_seq'), ?, 1004, ?, NOW())
                ON CONFLICT (user_id, item_id)
                DO UPDATE SET quantity = user_inventory.quantity + EXCLUDED.quantity
                """,
                userId, quantity);
    }

    @Transactional
    public void addMaterials(Integer userId, List<MaterialRewardRequest> materials) {
        if (userId == null || materials == null || materials.isEmpty()) {
            return;
        }

        for (MaterialRewardRequest material : materials) {
            if (material == null || material.getQuantity() == null || material.getQuantity() <= 0) {
                continue;
            }

            String materialKey = normalizeMaterialKey(material.getMaterialKey());
            if (materialKey == null) {
                continue;
            }

            int itemId = findOrCreateMaterialItem(
                    materialKey,
                    coalesceText(material.getItemName(), titleFromKey(materialKey)),
                    coalesceText(material.getRarity(), "Rare"));

            jdbcTemplate.update(
                    """
                    INSERT INTO user_inventory (user_inventory_id, user_id, item_id, quantity, acquired_at)
                    VALUES (nextval('user_inventory_seq'), ?, ?, ?, NOW())
                    ON CONFLICT (user_id, item_id)
                    DO UPDATE SET quantity = user_inventory.quantity + EXCLUDED.quantity
                    """,
                    userId, itemId, material.getQuantity());
        }
    }

    @Transactional
    public void spendMaterial(Integer userId, String materialKey, int quantity) {
        if (userId == null || quantity <= 0) {
            throw new IllegalArgumentException("Invalid material quantity");
        }

        String normalizedMaterialKey = normalizeMaterialKey(materialKey);
        if (normalizedMaterialKey == null) {
            throw new IllegalArgumentException("Material is required");
        }

        int updated = jdbcTemplate.update(
                """
                UPDATE user_inventory ui
                SET quantity = ui.quantity - ?
                WHERE ui.user_id = ?
                  AND ui.item_id IN (
                      SELECT item_id
                      FROM material
                      WHERE material_key = ?
                  )
                  AND ui.quantity >= ?
                """,
                quantity,
                userId,
                normalizedMaterialKey,
                quantity);

        if (updated <= 0) {
            throw new IllegalArgumentException("Not enough " + titleFromKey(normalizedMaterialKey));
        }
    }

    @Transactional
    public ChallengeClaimResponse claimChallengeReward(
            Integer userId,
            Integer challengeId,
            String challengeKey,
            int rewardCoins) {

        if (userId == null || challengeId == null) {
            throw new IllegalArgumentException("userId and challengeId are required");
        }

        Integer existingId = jdbcTemplate.query(
                """
                SELECT challenge_id
                FROM user_challenge_claim
                WHERE user_id = ? AND challenge_id = ?
                """,
                rs -> rs.next() ? rs.getInt("challenge_id") : null,
                userId,
                challengeId);

        if (existingId != null) {
            return new ChallengeClaimResponse(challengeId, coalesceText(challengeKey, ""), 0, true);
        }

        String safeChallengeKey = coalesceText(challengeKey, "challenge_" + challengeId);
        int safeRewardCoins = Math.max(0, rewardCoins);

        jdbcTemplate.update(
                """
                INSERT INTO user_challenge_claim (user_id, challenge_id, challenge_key, reward_coins, claimed_at)
                VALUES (?, ?, ?, ?, NOW())
                """,
                userId,
                challengeId,
                safeChallengeKey,
                safeRewardCoins);

        addCoins(userId, safeRewardCoins);
        return new ChallengeClaimResponse(challengeId, safeChallengeKey, safeRewardCoins, false);
    }

    public ChallengeClaimsResponse getClaimedChallengeRewards(Integer userId) {
        List<ChallengeClaimResponse> claims = jdbcTemplate.query(
                """
                SELECT challenge_id, challenge_key, reward_coins, claimed_at
                FROM user_challenge_claim
                WHERE user_id = ?
                ORDER BY challenge_id
                """,
                (rs, rowNum) -> {
                    ChallengeClaimResponse claim = new ChallengeClaimResponse(
                            rs.getInt("challenge_id"),
                            rs.getString("challenge_key"),
                            rs.getInt("reward_coins"),
                            true);
                    Timestamp claimedAt = rs.getTimestamp("claimed_at");
                    claim.setClaimedAt(claimedAt != null ? claimedAt.toLocalDateTime().toString() : null);
                    return claim;
                },
                userId);

        ChallengeClaimsResponse response = new ChallengeClaimsResponse();
        response.setClaims(claims);

        List<Integer> ids = new ArrayList<>();
        for (ChallengeClaimResponse claim : claims) {
            ids.add(claim.getChallengeId());
        }
        response.setClaimedChallengeIds(ids);

        return response;
    }

    public int getGoldCoins(Integer userId) {
        if (userId == null) {
            return 0;
        }

        Integer total = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(MAX(quantity), 0)
                FROM user_inventory
                WHERE user_id = ? AND item_id = 1004
                """,
                Integer.class,
                userId);
        return total != null ? total : 0;
    }

    public void addEmeralds(Integer userId, int quantity) {
        if (userId == null || quantity <= 0) {
            return;
        }

        jdbcTemplate.update(
                """
                INSERT INTO user_inventory (user_inventory_id, user_id, item_id, quantity, acquired_at)
                VALUES (nextval('user_inventory_seq'), ?, 1033, ?, NOW())
                ON CONFLICT (user_id, item_id)
                DO UPDATE SET quantity = user_inventory.quantity + EXCLUDED.quantity
                """,
                userId, quantity);
    }

    public int getEmeralds(Integer userId) {
        if (userId == null) {
            return 0;
        }

        Integer total = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(MAX(quantity), 0)
                FROM user_inventory
                WHERE user_id = ? AND item_id = 1033
                """,
                Integer.class,
                userId);
        return total != null ? total : 0;
    }

    public UserInventoryResponse getInventory(Integer userId) {
        ensureStarterInventory(userId);
        ensureDefaultEquippedItems(userId);

        List<InventoryItemResponse> items = jdbcTemplate.query(
                """
                SELECT
                    ui.user_inventory_id,
                    ui.item_id,
                    i.item_name,
                    i.item_type,
                    i.rarity,
                    i.description,
                    ui.quantity,
                    ui.acquired_at,
                    ui.is_equipped,
                    w.damage,
                    w.accuracy,
                    w.range,
                    w.fire_rate,
                    w.ammo_type,
                    w.weapon_type,
                    w.weapon_color,
                    a.defense,
                    a.durability,
                    a.weight,
                    c.effect_description,
                    c.duration_seconds,
                    c.cooldown_seconds,
                    cur.currency_code,
                    cur.is_tradeable,
                    m.material_key,
                    m.material_grade
                FROM user_inventory ui
                JOIN item i ON i.item_id = ui.item_id
                LEFT JOIN weapon w ON w.item_id = i.item_id
                LEFT JOIN armor a ON a.item_id = i.item_id
                LEFT JOIN consumable c ON c.item_id = i.item_id
                LEFT JOIN currency cur ON cur.item_id = i.item_id
                LEFT JOIN material m ON m.item_id = i.item_id
                WHERE ui.user_id = ?
                ORDER BY LOWER(i.item_type), LOWER(i.item_name), ui.user_inventory_id
                """,
                (rs, rowNum) -> {
                    InventoryItemResponse item = new InventoryItemResponse();
                    item.setUserInventoryId(rs.getInt("user_inventory_id"));
                    item.setItemId(rs.getInt("item_id"));
                    item.setItemName(rs.getString("item_name"));
                    item.setItemType(rs.getString("item_type"));
                    item.setRarity(rs.getString("rarity"));
                    item.setDescription(rs.getString("description"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setEquipped(rs.getBoolean("is_equipped"));

                    Timestamp acquiredAt = rs.getTimestamp("acquired_at");
                    item.setAcquiredAt(acquiredAt != null ? acquiredAt.toLocalDateTime().toString() : null);
                    item.setWeaponType(rs.getString("weapon_type"));
                    item.setWeaponColor(rs.getString("weapon_color"));
                    item.setMaterialKey(rs.getString("material_key"));
                    item.setDetailSummary(buildDetailSummary(rs));
                    return item;
                },
                userId);

        UserInventoryResponse response = new UserInventoryResponse();
        response.setUserId(userId);
        response.setItems(items);
        return response;
    }

    @Transactional
    public void consumeInventoryItem(Integer userId, Integer userInventoryId, int quantity) {
        if (userId == null || userInventoryId == null || quantity <= 0) {
            throw new IllegalArgumentException("Invalid consume request");
        }

        int updated = jdbcTemplate.update(
                """
                UPDATE user_inventory
                SET quantity = quantity - ?
                WHERE user_id = ?
                  AND user_inventory_id = ?
                  AND quantity >= ?
                """,
                quantity, userId, userInventoryId, quantity);

        if (updated <= 0) {
            throw new IllegalArgumentException("Not enough consumable quantity");
        }
    }

    @Transactional
    public UserInventoryResponse equipInventoryItem(Integer userId, Integer userInventoryId) {
        if (userId == null || userInventoryId == null) {
            throw new IllegalArgumentException("Item is required");
        }

        EquippedItemTarget target = jdbcTemplate.query(
                """
                SELECT ui.user_inventory_id, i.item_type
                FROM user_inventory ui
                JOIN item i ON i.item_id = ui.item_id
                WHERE ui.user_id = ?
                  AND ui.user_inventory_id = ?
                  AND ui.quantity > 0
                """,
                rs -> rs.next()
                        ? new EquippedItemTarget(rs.getInt("user_inventory_id"), rs.getString("item_type"))
                        : null,
                userId,
                userInventoryId);

        if (target == null) {
            throw new IllegalArgumentException("Item is not in this inventory");
        }

        if (!isEquippableType(target.itemType())) {
            throw new IllegalArgumentException("Item cannot be equipped");
        }

        jdbcTemplate.update(
                """
                UPDATE user_inventory ui
                SET is_equipped = FALSE
                FROM item i
                WHERE i.item_id = ui.item_id
                  AND ui.user_id = ?
                  AND LOWER(i.item_type) = LOWER(?)
                """,
                userId,
                target.itemType());

        jdbcTemplate.update(
                """
                UPDATE user_inventory
                SET is_equipped = TRUE
                WHERE user_id = ? AND user_inventory_id = ?
                """,
                userId,
                target.userInventoryId());

        return getInventory(userId);
    }

    private void ensureDefaultEquippedItems(Integer userId) {
        if (userId == null) {
            return;
        }

        for (String itemType : List.of("Weapon", "Armor", "Consumable")) {
            jdbcTemplate.update(
                    """
                    UPDATE user_inventory
                    SET is_equipped = TRUE
                    WHERE user_inventory_id = (
                        SELECT ui.user_inventory_id
                        FROM user_inventory ui
                        JOIN item i ON i.item_id = ui.item_id
                        WHERE ui.user_id = ?
                          AND LOWER(i.item_type) = LOWER(?)
                          AND ui.quantity > 0
                          AND NOT EXISTS (
                              SELECT 1
                              FROM user_inventory equipped_ui
                              JOIN item equipped_i ON equipped_i.item_id = equipped_ui.item_id
                              WHERE equipped_ui.user_id = ui.user_id
                                AND LOWER(equipped_i.item_type) = LOWER(i.item_type)
                                AND equipped_ui.is_equipped = TRUE
                                AND equipped_ui.quantity > 0
                          )
                        ORDER BY ui.user_inventory_id
                        LIMIT 1
                    )
                    """,
                    userId,
                    itemType);
        }
    }

    private boolean isEquippableType(String itemType) {
        return itemType != null && (
                itemType.equalsIgnoreCase("Weapon")
                        || itemType.equalsIgnoreCase("Armor")
                        || itemType.equalsIgnoreCase("Consumable"));
    }

    private record EquippedItemTarget(Integer userInventoryId, String itemType) {
    }

    private String buildDetailSummary(java.sql.ResultSet rs) throws java.sql.SQLException {
        String itemType = rs.getString("item_type");
        if (itemType == null) {
            return "";
        }

        return switch (itemType) {
            case "Weapon" -> buildWeaponSummary(
                    getNullableFloat(rs, "damage"),
                    getNullableFloat(rs, "accuracy"),
                    getNullableFloat(rs, "range"),
                    getNullableFloat(rs, "fire_rate"),
                    rs.getString("ammo_type"));
            case "Armor" -> buildArmorSummary(
                    getNullableFloat(rs, "defense"),
                    getNullableFloat(rs, "durability"),
                    getNullableFloat(rs, "weight"));
            case "Consumable" -> buildConsumableSummary(
                    rs.getString("effect_description"),
                    getNullableInteger(rs, "duration_seconds"),
                    getNullableInteger(rs, "cooldown_seconds"));
            case "Currency" -> buildCurrencySummary(
                    rs.getString("currency_code"),
                    getNullableBoolean(rs, "is_tradeable"));
            case "Material" -> buildMaterialSummary(rs.getString("material_grade"));
            default -> "";
        };
    }

    private String buildWeaponSummary(Float damage, Float accuracy, Float range, Float fireRate, String ammoType) {
        StringBuilder text = new StringBuilder();
        appendPart(text, formatFloat("DMG", damage));
        appendPart(text, formatFloat("ACC", accuracy));
        appendPart(text, formatFloat("RNG", range));
        appendPart(text, formatFloat("ROF", fireRate));
        appendPart(text, ammoType != null && !ammoType.isBlank() ? "Ammo: " + ammoType : null);
        return text.toString();
    }

    private String buildArmorSummary(Float defense, Float durability, Float weight) {
        StringBuilder text = new StringBuilder();
        appendPart(text, formatFloat("DEF", defense));
        appendPart(text, formatFloat("Durability", durability));
        appendPart(text, formatFloat("Weight", weight));
        return text.toString();
    }

    private String buildConsumableSummary(String effectDescription, Integer durationSeconds, Integer cooldownSeconds) {
        StringBuilder text = new StringBuilder();
        appendPart(text, effectDescription);
        appendPart(text, durationSeconds != null ? "Duration: " + durationSeconds + "s" : null);
        appendPart(text, cooldownSeconds != null ? "Cooldown: " + cooldownSeconds + "s" : null);
        return text.toString();
    }

    private String buildCurrencySummary(String currencyCode, Boolean tradeable) {
        StringBuilder text = new StringBuilder();
        appendPart(text, currencyCode != null && !currencyCode.isBlank() ? "Code: " + currencyCode : null);
        appendPart(text, tradeable != null ? (tradeable ? "Tradeable" : "Bound") : null);
        return text.toString();
    }

    private String buildMaterialSummary(String materialGrade) {
        return materialGrade != null && !materialGrade.isBlank()
                ? "Grade: " + materialGrade
                : "";
    }

    private String formatFloat(String label, Float value) {
        return value != null ? label + ": " + trimFloat(value) : null;
    }

    private String trimFloat(Float value) {
        if (value == null) {
            return "";
        }

        if (Math.abs(value - value.intValue()) < 0.0001f) {
            return Integer.toString(value.intValue());
        }

        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private void appendPart(StringBuilder text, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        if (text.length() > 0) {
            text.append(" | ");
        }
        text.append(value.trim());
    }

    private int findOrCreateMaterialItem(String materialKey, String itemName, String rarity) {
        Integer existingItemId = jdbcTemplate.query(
                "SELECT item_id FROM material WHERE material_key = ?",
                rs -> rs.next() ? rs.getInt("item_id") : null,
                materialKey);
        if (existingItemId != null) {
            return existingItemId;
        }

        Integer nextItemId = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(item_id), 1029) + 1 FROM item",
                Integer.class);
        if (nextItemId == null) {
            nextItemId = 1030;
        }

        jdbcTemplate.update(
                """
                INSERT INTO item (item_id, item_name, item_type, rarity, description)
                VALUES (?, ?, 'Material', ?, ?)
                """,
                nextItemId,
                itemName,
                rarity,
                "A map material collected from a giant.");

        jdbcTemplate.update(
                """
                INSERT INTO material (item_id, material_key, material_grade)
                VALUES (?, ?, ?)
                """,
                nextItemId,
                materialKey,
                rarity);

        return nextItemId;
    }

    private String normalizeMaterialKey(String materialKey) {
        if (materialKey == null || materialKey.isBlank()) {
            return null;
        }

        return materialKey.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String coalesceText(String value, String fallback) {
        return value != null && !value.isBlank() ? value.trim() : fallback;
    }

    private String titleFromKey(String materialKey) {
        String[] parts = materialKey.split("_+");
        StringBuilder title = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (title.length() > 0) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                title.append(part.substring(1));
            }
        }
        return title.length() > 0 ? title.toString() : "Map Material";
    }

    public static class MaterialRewardRequest {
        private String materialKey;
        private String itemName;
        private String itemType;
        private String rarity;
        private Integer quantity;

        public String getMaterialKey() {
            return materialKey;
        }

        public void setMaterialKey(String materialKey) {
            this.materialKey = materialKey;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public String getItemType() {
            return itemType;
        }

        public void setItemType(String itemType) {
            this.itemType = itemType;
        }

        public String getRarity() {
            return rarity;
        }

        public void setRarity(String rarity) {
            this.rarity = rarity;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    private Float getNullableFloat(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        float value = rs.getFloat(column);
        return rs.wasNull() ? null : value;
    }

    private Integer getNullableInteger(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Boolean getNullableBoolean(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

}
