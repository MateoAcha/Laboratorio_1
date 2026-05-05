package user_api;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SkinService {

    private final JdbcTemplate jdbcTemplate;

    public SkinService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserSkinsResponse getUserSkins(int userId) {
        List<SkinResponse> skins = jdbcTemplate.query(
                """
                SELECT s.skin_id, s.skin_name, s.rarity, us.is_equipped
                FROM user_skin us
                JOIN skin s ON s.skin_id = us.skin_id
                WHERE us.user_id = ?
                ORDER BY s.rarity, s.skin_name
                """,
                (rs, rowNum) -> {
                    SkinResponse skin = new SkinResponse();
                    skin.setSkinId(rs.getInt("skin_id"));
                    skin.setSkinName(rs.getString("skin_name"));
                    skin.setRarity(rs.getString("rarity"));
                    skin.setEquipped(rs.getBoolean("is_equipped"));
                    return skin;
                },
                userId);

        UserSkinsResponse response = new UserSkinsResponse();
        response.setUserId(userId);
        response.setSkins(skins);
        return response;
    }

    @Transactional
    public void equipSkin(int userId, int skinId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_skin WHERE user_id = ? AND skin_id = ?",
                Integer.class, userId, skinId);
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Skin not unlocked");
        }

        jdbcTemplate.update("UPDATE user_skin SET is_equipped = FALSE WHERE user_id = ?", userId);
        jdbcTemplate.update(
                "UPDATE user_skin SET is_equipped = TRUE WHERE user_id = ? AND skin_id = ?",
                userId, skinId);
    }

    public void ensureStarterSkins(int userId) {
        unlockSkinEquipped(userId, 2000, true);
    }

    public void unlockSkin(int userId, int skinId) {
        insertUserSkin(userId, skinId, "shop", false);
    }

    private void unlockSkinEquipped(int userId, int skinId, boolean equipped) {
        insertUserSkin(userId, skinId, "starter", equipped);
    }

    private void insertUserSkin(int userId, int skinId, String source, boolean equipped) {
        jdbcTemplate.update(
                """
                INSERT INTO user_skin (user_skin_id, user_id, skin_id, unlocked_at, unlock_source, is_equipped)
                VALUES (nextval('user_skin_seq'), ?, ?, NOW(), ?, ?)
                ON CONFLICT (user_id, skin_id) DO NOTHING
                """,
                userId, skinId, source, equipped);
    }
}
