package user_api;

import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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

    public UserInventoryResponse getInventory(Integer userId) {
        ensureStarterInventory(userId);

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
                    w.damage,
                    w.accuracy,
                    w.range,
                    w.fire_rate,
                    w.ammo_type,
                    a.defense,
                    a.durability,
                    a.weight,
                    c.effect_description,
                    c.duration_seconds,
                    c.cooldown_seconds,
                    cur.currency_code,
                    cur.is_tradeable,
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

                    Timestamp acquiredAt = rs.getTimestamp("acquired_at");
                    item.setAcquiredAt(acquiredAt != null ? acquiredAt.toLocalDateTime().toString() : null);
                    item.setDetailSummary(buildDetailSummary(rs));
                    return item;
                },
                userId);

        UserInventoryResponse response = new UserInventoryResponse();
        response.setUserId(userId);
        response.setItems(items);
        return response;
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
