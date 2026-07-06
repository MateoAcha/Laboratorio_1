package user_api;

public class UserProfileSummaryResponse {

    private Integer userId;
    private String username;
    private Integer level;
    private ProfileStatsResponse stats;
    private ProfileLoadoutResponse loadout;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public ProfileStatsResponse getStats() {
        return stats;
    }

    public void setStats(ProfileStatsResponse stats) {
        this.stats = stats;
    }

    public ProfileLoadoutResponse getLoadout() {
        return loadout;
    }

    public void setLoadout(ProfileLoadoutResponse loadout) {
        this.loadout = loadout;
    }

    public static class ProfileStatsResponse {
        private Integer gamesPlayed;
        private Integer wins;
        private Integer kills;
        private Long bestTimeSeconds;
        private Integer matchesPlayed;
        private Integer meleeEnemiesKilled;
        private Integer rangedEnemiesKilled;
        private Integer giantEnemiesKilled;
        private Integer deaths;
        private Integer gamesWon;
        private Integer highScore;
        private Long timePlayedSeconds;
        private Integer coins;
        private Integer emeralds;
        private Long totalXp;
        private Integer level;
        private Integer unspentSkillPoints;
        private Integer spentSkillPoints;

        public Integer getGamesPlayed() {
            return gamesPlayed;
        }

        public void setGamesPlayed(Integer gamesPlayed) {
            this.gamesPlayed = gamesPlayed;
        }

        public Integer getWins() {
            return wins;
        }

        public void setWins(Integer wins) {
            this.wins = wins;
        }

        public Integer getKills() {
            return kills;
        }

        public void setKills(Integer kills) {
            this.kills = kills;
        }

        public Long getBestTimeSeconds() {
            return bestTimeSeconds;
        }

        public void setBestTimeSeconds(Long bestTimeSeconds) {
            this.bestTimeSeconds = bestTimeSeconds;
        }

        public Integer getMatchesPlayed() {
            return matchesPlayed;
        }

        public void setMatchesPlayed(Integer matchesPlayed) {
            this.matchesPlayed = matchesPlayed;
        }

        public Integer getMeleeEnemiesKilled() {
            return meleeEnemiesKilled;
        }

        public void setMeleeEnemiesKilled(Integer meleeEnemiesKilled) {
            this.meleeEnemiesKilled = meleeEnemiesKilled;
        }

        public Integer getRangedEnemiesKilled() {
            return rangedEnemiesKilled;
        }

        public void setRangedEnemiesKilled(Integer rangedEnemiesKilled) {
            this.rangedEnemiesKilled = rangedEnemiesKilled;
        }

        public Integer getGiantEnemiesKilled() {
            return giantEnemiesKilled;
        }

        public void setGiantEnemiesKilled(Integer giantEnemiesKilled) {
            this.giantEnemiesKilled = giantEnemiesKilled;
        }

        public Integer getDeaths() {
            return deaths;
        }

        public void setDeaths(Integer deaths) {
            this.deaths = deaths;
        }

        public Integer getGamesWon() {
            return gamesWon;
        }

        public void setGamesWon(Integer gamesWon) {
            this.gamesWon = gamesWon;
        }

        public Integer getHighScore() {
            return highScore;
        }

        public void setHighScore(Integer highScore) {
            this.highScore = highScore;
        }

        public Long getTimePlayedSeconds() {
            return timePlayedSeconds;
        }

        public void setTimePlayedSeconds(Long timePlayedSeconds) {
            this.timePlayedSeconds = timePlayedSeconds;
        }

        public Integer getCoins() {
            return coins;
        }

        public void setCoins(Integer coins) {
            this.coins = coins;
        }

        public Integer getEmeralds() {
            return emeralds;
        }

        public void setEmeralds(Integer emeralds) {
            this.emeralds = emeralds;
        }

        public Long getTotalXp() {
            return totalXp;
        }

        public void setTotalXp(Long totalXp) {
            this.totalXp = totalXp;
        }

        public Integer getLevel() {
            return level;
        }

        public void setLevel(Integer level) {
            this.level = level;
        }

        public Integer getUnspentSkillPoints() {
            return unspentSkillPoints;
        }

        public void setUnspentSkillPoints(Integer unspentSkillPoints) {
            this.unspentSkillPoints = unspentSkillPoints;
        }

        public Integer getSpentSkillPoints() {
            return spentSkillPoints;
        }

        public void setSpentSkillPoints(Integer spentSkillPoints) {
            this.spentSkillPoints = spentSkillPoints;
        }
    }

    public static class ProfileLoadoutResponse {
        private Integer skinId;
        private String skinColor;
        private Integer weaponItemId;
        private String weaponType;
        private String weaponColor;
        private String armorName;
        private String consumableName;

        public Integer getSkinId() {
            return skinId;
        }

        public void setSkinId(Integer skinId) {
            this.skinId = skinId;
        }

        public String getSkinColor() {
            return skinColor;
        }

        public void setSkinColor(String skinColor) {
            this.skinColor = skinColor;
        }

        public Integer getWeaponItemId() {
            return weaponItemId;
        }

        public void setWeaponItemId(Integer weaponItemId) {
            this.weaponItemId = weaponItemId;
        }

        public String getWeaponType() {
            return weaponType;
        }

        public void setWeaponType(String weaponType) {
            this.weaponType = weaponType;
        }

        public String getWeaponColor() {
            return weaponColor;
        }

        public void setWeaponColor(String weaponColor) {
            this.weaponColor = weaponColor;
        }

        public String getArmorName() {
            return armorName;
        }

        public void setArmorName(String armorName) {
            this.armorName = armorName;
        }

        public String getConsumableName() {
            return consumableName;
        }

        public void setConsumableName(String consumableName) {
            this.consumableName = consumableName;
        }
    }
}
