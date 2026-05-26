package user_api;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkillTreeService {

    private static final int MAX_SKILL_LEVEL = 3;
    private static final int MATERIAL_UPGRADE_COST = 3;
    private static final String[] DEFAULT_EQUIPPED_SKILLS = {
            "swordspear_active_1",
            "swordspear_passive_1",
            "ranged_active_1",
            "ranged_passive_1"
    };

    private final JdbcTemplate jdbcTemplate;
    private final InventoryService inventoryService;
    private final Map<String, SkillDefinition> skills = new LinkedHashMap<>();

    public SkillTreeService(JdbcTemplate jdbcTemplate, InventoryService inventoryService) {
        this.jdbcTemplate = jdbcTemplate;
        this.inventoryService = inventoryService;
        registerSkills();
    }

    public UserSkillTreeResponse getSkillTree(Integer userId) {
        ensureDefaultSkills(userId);

        List<UserSkillResponse> userSkills = jdbcTemplate.query(
                """
                SELECT skill_id, skill_level, unlocked_at
                FROM user_skill
                WHERE user_id = ?
                ORDER BY skill_id
                """,
                (rs, rowNum) -> {
                    UserSkillResponse skill = new UserSkillResponse();
                    skill.setSkillId(rs.getString("skill_id"));
                    skill.setUnlocked(true);
                    skill.setLevel(rs.getInt("skill_level"));
                    Timestamp unlockedAt = rs.getTimestamp("unlocked_at");
                    skill.setUnlockedAt(unlockedAt != null ? unlockedAt.toLocalDateTime().toString() : null);
                    return skill;
                },
                userId);

        List<EquippedSkillResponse> equippedSkills = jdbcTemplate.query(
                """
                SELECT branch, slot_kind, skill_id
                FROM user_skill_loadout
                WHERE user_id = ?
                ORDER BY branch, slot_kind
                """,
                (rs, rowNum) -> {
                    EquippedSkillResponse equipped = new EquippedSkillResponse();
                    equipped.setBranch(rs.getString("branch"));
                    equipped.setSlotKind(rs.getString("slot_kind"));
                    equipped.setSkillId(rs.getString("skill_id"));
                    return equipped;
                },
                userId);

        UserSkillTreeResponse response = new UserSkillTreeResponse();
        response.setSkills(userSkills);
        response.setEquippedSkills(equippedSkills);
        return response;
    }

    @Transactional
    public SkillTreeActionResponse unlockSkill(Integer userId, String skillId) {
        SkillDefinition skill = requireSkill(skillId);
        if (isUnlocked(userId, skill.id())) {
            return actionResponse(userId);
        }

        if (skill.prerequisiteId() != null && !skill.prerequisiteId().isBlank() &&
                !isUnlocked(userId, skill.prerequisiteId())) {
            throw new IllegalArgumentException("Previous skill must be unlocked first");
        }

        ensureStats(userId);
        int updated = jdbcTemplate.update(
                """
                UPDATE player_stats
                SET unspent_skill_points = unspent_skill_points - ?,
                    spent_skill_points = spent_skill_points + ?
                WHERE user_id = ?
                  AND unspent_skill_points >= ?
                """,
                skill.cost(),
                skill.cost(),
                userId,
                skill.cost());

        if (updated <= 0) {
            throw new IllegalArgumentException("Not enough skill points");
        }

        jdbcTemplate.update(
                """
                INSERT INTO user_skill (user_id, skill_id, skill_level, unlocked_at)
                VALUES (?, ?, 0, NOW())
                ON CONFLICT (user_id, skill_id) DO NOTHING
                """,
                userId,
                skill.id());

        equipSkillInternal(userId, skill);
        return actionResponse(userId);
    }

    @Transactional
    public SkillTreeActionResponse equipSkill(Integer userId, String skillId) {
        SkillDefinition skill = requireSkill(skillId);
        if (!isUnlocked(userId, skill.id())) {
            throw new IllegalArgumentException("Skill is not unlocked");
        }

        equipSkillInternal(userId, skill);
        return actionResponse(userId);
    }

    @Transactional
    public SkillTreeActionResponse levelUpSkill(Integer userId, String skillId) {
        SkillDefinition skill = requireSkill(skillId);
        Integer currentLevel = getSkillLevel(userId, skill.id());
        if (currentLevel == null) {
            throw new IllegalArgumentException("Skill is not unlocked");
        }
        if (currentLevel >= MAX_SKILL_LEVEL) {
            return actionResponse(userId);
        }

        int nextLevel = currentLevel + 1;
        inventoryService.spendMaterial(userId, materialKeyForLevel(nextLevel), MATERIAL_UPGRADE_COST);
        jdbcTemplate.update(
                """
                UPDATE user_skill
                SET skill_level = ?
                WHERE user_id = ? AND skill_id = ?
                """,
                nextLevel,
                userId,
                skill.id());

        return actionResponse(userId);
    }

    private SkillTreeActionResponse actionResponse(Integer userId) {
        ensureStats(userId);
        SkillTreeActionResponse response = new SkillTreeActionResponse();
        response.setSkillTree(getSkillTree(userId));
        response.setStats(getStats(userId));
        return response;
    }

    private void equipSkillInternal(Integer userId, SkillDefinition skill) {
        jdbcTemplate.update(
                """
                INSERT INTO user_skill_loadout (user_id, branch, slot_kind, skill_id, equipped_at)
                VALUES (?, ?, ?, ?, NOW())
                ON CONFLICT (user_id, branch, slot_kind)
                DO UPDATE SET skill_id = EXCLUDED.skill_id, equipped_at = NOW()
                """,
                userId,
                skill.branch(),
                skill.slotKind(),
                skill.id());
    }

    private void ensureDefaultSkills(Integer userId) {
        if (userId == null) {
            return;
        }

        for (String skillId : DEFAULT_EQUIPPED_SKILLS) {
            SkillDefinition skill = requireSkill(skillId);
            jdbcTemplate.update(
                    """
                    INSERT INTO user_skill (user_id, skill_id, skill_level, unlocked_at)
                    VALUES (?, ?, 0, NOW())
                    ON CONFLICT (user_id, skill_id) DO NOTHING
                    """,
                    userId,
                    skill.id());

            jdbcTemplate.update(
                    """
                    INSERT INTO user_skill_loadout (user_id, branch, slot_kind, skill_id, equipped_at)
                    VALUES (?, ?, ?, ?, NOW())
                    ON CONFLICT (user_id, branch, slot_kind) DO NOTHING
                    """,
                    userId,
                    skill.branch(),
                    skill.slotKind(),
                    skill.id());
        }
    }

    private boolean isUnlocked(Integer userId, String skillId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_skill WHERE user_id = ? AND skill_id = ?",
                Integer.class,
                userId,
                skillId);
        return count != null && count > 0;
    }

    private Integer getSkillLevel(Integer userId, String skillId) {
        return jdbcTemplate.query(
                """
                SELECT skill_level
                FROM user_skill
                WHERE user_id = ? AND skill_id = ?
                """,
                rs -> rs.next() ? rs.getInt("skill_level") : null,
                userId,
                skillId);
    }

    private void ensureStats(Integer userId) {
        jdbcTemplate.update(
                """
                INSERT INTO player_stats (
                    user_id, matches_played, melee_enemies_killed, ranged_enemies_killed,
                    giant_enemies_killed, deaths, games_won, high_score, time_played_seconds,
                    coins, total_xp, level, unspent_skill_points, spent_skill_points
                )
                VALUES (?, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0)
                ON CONFLICT (user_id) DO NOTHING
                """,
                userId);
    }

    private PlayerStats getStats(Integer userId) {
        return jdbcTemplate.query(
                """
                SELECT user_id, matches_played, melee_enemies_killed, ranged_enemies_killed,
                       giant_enemies_killed, deaths, games_won, high_score, time_played_seconds,
                       coins, total_xp, level, unspent_skill_points, spent_skill_points
                FROM player_stats
                WHERE user_id = ?
                """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    PlayerStats stats = new PlayerStats();
                    stats.setUserId(rs.getInt("user_id"));
                    stats.setMatchesPlayed(rs.getInt("matches_played"));
                    stats.setMeleeEnemiesKilled(rs.getInt("melee_enemies_killed"));
                    stats.setRangedEnemiesKilled(rs.getInt("ranged_enemies_killed"));
                    stats.setGiantEnemiesKilled(rs.getInt("giant_enemies_killed"));
                    stats.setDeaths(rs.getInt("deaths"));
                    stats.setGamesWon(rs.getInt("games_won"));
                    stats.setHighScore(rs.getInt("high_score"));
                    stats.setTimePlayedSeconds(rs.getLong("time_played_seconds"));
                    stats.setCoins(rs.getInt("coins"));
                    stats.setTotalXp(rs.getLong("total_xp"));
                    stats.setLevel(rs.getInt("level"));
                    stats.setUnspentSkillPoints(rs.getInt("unspent_skill_points"));
                    stats.setSpentSkillPoints(rs.getInt("spent_skill_points"));
                    return stats;
                },
                userId);
    }

    private SkillDefinition requireSkill(String skillId) {
        SkillDefinition skill = skills.get(skillId);
        if (skill == null) {
            throw new IllegalArgumentException("Unknown skill");
        }
        return skill;
    }

    private String materialKeyForLevel(int level) {
        if (level <= 1) {
            return "Rusty_Scrap";
        }
        if (level == 2) {
            return "Metal_Scrap";
        }
        return "Diamond_Scrap";
    }

    private void registerSkills() {
        add("swordspear_active_1", "SwordSpear", "Active", 1, "");
        add("swordspear_active_2", "SwordSpear", "Active", 2, "swordspear_active_1");
        add("swordspear_active_3", "SwordSpear", "Active", 3, "swordspear_active_2");
        add("swordspear_passive_1", "SwordSpear", "Passive", 1, "");
        add("swordspear_passive_2", "SwordSpear", "Passive", 2, "swordspear_passive_1");
        add("swordspear_passive_3", "SwordSpear", "Passive", 3, "swordspear_passive_2");
        add("ranged_active_1", "Ranged", "Active", 1, "");
        add("ranged_active_2", "Ranged", "Active", 2, "ranged_active_1");
        add("ranged_active_3", "Ranged", "Active", 3, "ranged_active_2");
        add("ranged_passive_1", "Ranged", "Passive", 1, "");
        add("ranged_passive_2", "Ranged", "Passive", 2, "ranged_passive_1");
        add("ranged_passive_3", "Ranged", "Passive", 3, "ranged_passive_2");
    }

    private void add(String id, String branch, String slotKind, int cost, String prerequisiteId) {
        skills.put(id, new SkillDefinition(id, branch, slotKind, cost, prerequisiteId));
    }

    private record SkillDefinition(String id, String branch, String slotKind, int cost, String prerequisiteId) {
    }

    public static class UserSkillTreeResponse {
        private List<UserSkillResponse> skills = new ArrayList<>();
        private List<EquippedSkillResponse> equippedSkills = new ArrayList<>();

        public List<UserSkillResponse> getSkills() {
            return skills;
        }

        public void setSkills(List<UserSkillResponse> skills) {
            this.skills = skills;
        }

        public List<EquippedSkillResponse> getEquippedSkills() {
            return equippedSkills;
        }

        public void setEquippedSkills(List<EquippedSkillResponse> equippedSkills) {
            this.equippedSkills = equippedSkills;
        }
    }

    public static class UserSkillResponse {
        private String skillId;
        private Boolean unlocked;
        private Integer level;
        private String unlockedAt;

        public String getSkillId() {
            return skillId;
        }

        public void setSkillId(String skillId) {
            this.skillId = skillId;
        }

        public Boolean getUnlocked() {
            return unlocked;
        }

        public void setUnlocked(Boolean unlocked) {
            this.unlocked = unlocked;
        }

        public Integer getLevel() {
            return level;
        }

        public void setLevel(Integer level) {
            this.level = level;
        }

        public String getUnlockedAt() {
            return unlockedAt;
        }

        public void setUnlockedAt(String unlockedAt) {
            this.unlockedAt = unlockedAt;
        }
    }

    public static class EquippedSkillResponse {
        private String branch;
        private String slotKind;
        private String skillId;

        public String getBranch() {
            return branch;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }

        public String getSlotKind() {
            return slotKind;
        }

        public void setSlotKind(String slotKind) {
            this.slotKind = slotKind;
        }

        public String getSkillId() {
            return skillId;
        }

        public void setSkillId(String skillId) {
            this.skillId = skillId;
        }
    }

    public static class SkillTreeActionResponse {
        private UserSkillTreeResponse skillTree;
        private PlayerStats stats;

        public UserSkillTreeResponse getSkillTree() {
            return skillTree;
        }

        public void setSkillTree(UserSkillTreeResponse skillTree) {
            this.skillTree = skillTree;
        }

        public PlayerStats getStats() {
            return stats;
        }

        public void setStats(PlayerStats stats) {
            this.stats = stats;
        }
    }
}
