package user_api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "player_stats")
public class PlayerStats {

    @Id
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @JsonIgnore
    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_player_stats_user"))
    private User user;

    @Column(name = "matches_played", nullable = false)
    private Integer matchesPlayed;

    @Column(name = "melee_enemies_killed", nullable = false)
    private Integer meleeEnemiesKilled;

    @Column(name = "ranged_enemies_killed", nullable = false)
    private Integer rangedEnemiesKilled;

    @Column(name = "giant_enemies_killed")
    private Integer giantEnemiesKilled;

    @Column(name = "deaths", nullable = false)
    private Integer deaths;

    @Column(name = "games_won", nullable = false)
    private Integer gamesWon;

    @Column(name = "high_score", nullable = false)
    private Integer highScore;

    @Column(name = "time_played_seconds", nullable = false)
    private Long timePlayedSeconds;

    @Column(name = "coins", nullable = false)
    private Integer coins;

    @Column(name = "total_xp", nullable = false)
    private Long totalXp;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "unspent_skill_points", nullable = false)
    private Integer unspentSkillPoints;

    @Column(name = "spent_skill_points", nullable = false)
    private Integer spentSkillPoints;

    public PlayerStats() {
    }

    @PrePersist
    public void prePersist() {
        if (matchesPlayed == null) matchesPlayed = 0;
        if (meleeEnemiesKilled == null) meleeEnemiesKilled = 0;
        if (rangedEnemiesKilled == null) rangedEnemiesKilled = 0;
        if (giantEnemiesKilled == null) giantEnemiesKilled = 0;
        if (deaths == null) deaths = 0;
        if (gamesWon == null) gamesWon = 0;
        if (highScore == null) highScore = 0;
        if (timePlayedSeconds == null) timePlayedSeconds = 0L;
        if (coins == null) coins = 0;
        if (totalXp == null) totalXp = 0L;
        if (level == null || level < 1) level = 1;
        if (unspentSkillPoints == null) unspentSkillPoints = 0;
        if (spentSkillPoints == null) spentSkillPoints = 0;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
        return giantEnemiesKilled != null ? giantEnemiesKilled : 0;
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

    public Long getTotalXp() {
        return totalXp != null ? totalXp : 0L;
    }

    public void setTotalXp(Long totalXp) {
        this.totalXp = totalXp;
    }

    public Integer getLevel() {
        return level != null && level > 0 ? level : 1;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getUnspentSkillPoints() {
        return unspentSkillPoints != null ? unspentSkillPoints : 0;
    }

    public void setUnspentSkillPoints(Integer unspentSkillPoints) {
        this.unspentSkillPoints = unspentSkillPoints;
    }

    public Integer getSpentSkillPoints() {
        return spentSkillPoints != null ? spentSkillPoints : 0;
    }

    public void setSpentSkillPoints(Integer spentSkillPoints) {
        this.spentSkillPoints = spentSkillPoints;
    }
}
