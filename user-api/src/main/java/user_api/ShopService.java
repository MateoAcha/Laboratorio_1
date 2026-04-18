package user_api;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ShopService {

    private static final int GOLD_ITEM_ID = 1004;

    private final JdbcTemplate jdbcTemplate;

    public ShopService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ShopCatalogResponse getCatalog() {
        List<ShopItemResponse> items = jdbcTemplate.query(
                """
                SELECT
                    s.shop_item_id,
                    s.gold_price,
                    s.purchase_quantity,
                    i.item_id,
                    i.item_name,
                    i.item_type,
                    i.rarity,
                    i.description,
                    w.damage, w.accuracy, w.range,
                    a.defense, a.durability,
                    c.effect_description, c.cooldown_seconds
                FROM shop_item s
                JOIN item i ON i.item_id = s.item_id
                LEFT JOIN weapon w ON w.item_id = i.item_id
                LEFT JOIN armor a ON a.item_id = i.item_id
                LEFT JOIN consumable c ON c.item_id = i.item_id
                WHERE s.is_available = TRUE
                ORDER BY s.gold_price
                """,
                (rs, rowNum) -> {
                    ShopItemResponse item = new ShopItemResponse();
                    item.setShopItemId(rs.getInt("shop_item_id"));
                    item.setGoldPrice(rs.getInt("gold_price"));
                    item.setPurchaseQuantity(rs.getInt("purchase_quantity"));
                    item.setItemId(rs.getInt("item_id"));
                    item.setItemName(rs.getString("item_name"));
                    item.setItemType(rs.getString("item_type"));
                    item.setRarity(rs.getString("rarity"));
                    item.setDescription(rs.getString("description"));
                    item.setDetailSummary(buildSummary(rs));
                    return item;
                });

        ShopCatalogResponse catalog = new ShopCatalogResponse();
        catalog.setItems(items);
        return catalog;
    }

    @Transactional
    public void purchaseItem(Integer userId, int shopItemId) {
        // Resolve shop item
        List<ShopItemRow> rows = jdbcTemplate.query(
                "SELECT item_id, gold_price, purchase_quantity FROM shop_item WHERE shop_item_id = ? AND is_available = TRUE",
                (rs, n) -> new ShopItemRow(rs.getInt("item_id"), rs.getInt("gold_price"), rs.getInt("purchase_quantity")),
                shopItemId);

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop item not found");
        }

        ShopItemRow shop = rows.get(0);

        // Check user exists
        Number userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE user_id = ?", Number.class, userId);
        if (userCount == null || userCount.intValue() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        // Check coins — atomically deduct; if 0 rows affected the user had insufficient funds
        int updated = jdbcTemplate.update(
                "UPDATE user_inventory SET quantity = quantity - ? WHERE user_id = ? AND item_id = ? AND quantity >= ?",
                shop.goldPrice, userId, GOLD_ITEM_ID, shop.goldPrice);

        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Not enough gold coins");
        }

        // Add purchased item to inventory (or increase quantity)
        jdbcTemplate.update(
                """
                INSERT INTO user_inventory (user_inventory_id, user_id, item_id, quantity, acquired_at)
                VALUES (nextval('user_inventory_seq'), ?, ?, ?, NOW())
                ON CONFLICT (user_id, item_id)
                DO UPDATE SET quantity = user_inventory.quantity + EXCLUDED.quantity
                """,
                userId, shop.itemId, shop.purchaseQuantity);
    }

    private String buildSummary(java.sql.ResultSet rs) throws java.sql.SQLException {
        String type = rs.getString("item_type");
        if (type == null) return "";
        return switch (type) {
            case "Weapon" -> buildWeapon(getNullableFloat(rs, "damage"),
                    getNullableFloat(rs, "accuracy"), getNullableFloat(rs, "range"));
            case "Armor" -> buildArmor(getNullableFloat(rs, "defense"),
                    getNullableFloat(rs, "durability"));
            case "Consumable" -> buildConsumable(rs.getString("effect_description"),
                    getNullableInteger(rs, "cooldown_seconds"));
            default -> "";
        };
    }

    private String buildWeapon(Float dmg, Float acc, Float rng) {
        StringBuilder sb = new StringBuilder();
        append(sb, fmt("DMG", dmg));
        append(sb, fmt("ACC", acc));
        append(sb, fmt("RNG", rng));
        return sb.toString();
    }

    private String buildArmor(Float def, Float dur) {
        StringBuilder sb = new StringBuilder();
        append(sb, fmt("DEF", def));
        append(sb, fmt("Durability", dur));
        return sb.toString();
    }

    private String buildConsumable(String effect, Integer cooldown) {
        StringBuilder sb = new StringBuilder();
        append(sb, effect);
        if (cooldown != null) append(sb, "Cooldown: " + cooldown + "s");
        return sb.toString();
    }

    private String fmt(String label, Float v) {
        if (v == null) return null;
        String num = (Math.abs(v - v.intValue()) < 0.0001f)
                ? Integer.toString(v.intValue())
                : String.format(java.util.Locale.US, "%.2f", v);
        return label + ": " + num;
    }

    private void append(StringBuilder sb, String val) {
        if (val == null || val.isBlank()) return;
        if (sb.length() > 0) sb.append(" | ");
        sb.append(val.trim());
    }

    private Float getNullableFloat(java.sql.ResultSet rs, String col) throws java.sql.SQLException {
        float v = rs.getFloat(col); return rs.wasNull() ? null : v;
    }

    private Integer getNullableInteger(java.sql.ResultSet rs, String col) throws java.sql.SQLException {
        int v = rs.getInt(col); return rs.wasNull() ? null : v;
    }

    private record ShopItemRow(int itemId, int goldPrice, int purchaseQuantity) {}
}
