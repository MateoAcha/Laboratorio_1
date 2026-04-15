package user_api;

import java.util.List;
import java.util.function.Function;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
public class GlobalStatsController {

    private final PlayerStatsRepository playerStatsRepository;

    public GlobalStatsController(PlayerStatsRepository playerStatsRepository) {
        this.playerStatsRepository = playerStatsRepository;
    }

    @GetMapping("/global")
    public GlobalStatsResponse getGlobalStats() {
        List<PlayerStats> allStats = playerStatsRepository.findAll();

        GlobalStatEntry highestMatchesPlayed = findLeader(
                allStats,
                stats -> toLong(stats.getMatchesPlayed()));

        GlobalStatEntry highestKillCount = findLeader(
                allStats,
                stats -> toLong(stats.getMeleeEnemiesKilled()) + toLong(stats.getRangedEnemiesKilled()));

        GlobalStatEntry highestTimePlayed = findLeader(
                allStats,
                stats -> stats.getTimePlayedSeconds() == null ? 0L : Math.max(0L, stats.getTimePlayedSeconds()));

        return new GlobalStatsResponse(highestMatchesPlayed, highestKillCount, highestTimePlayed);
    }

    private GlobalStatEntry findLeader(List<PlayerStats> allStats, Function<PlayerStats, Long> selector) {
        String bestUsername = "-";
        long bestValue = -1L;

        for (PlayerStats stats : allStats) {
            long value = Math.max(0L, selector.apply(stats));
            String username = resolveUsername(stats);

            boolean isBetter = value > bestValue;
            boolean isTieBreakBetter = value == bestValue && username.compareToIgnoreCase(bestUsername) < 0;

            if (isBetter || isTieBreakBetter) {
                bestValue = value;
                bestUsername = username;
            }
        }

        if (bestValue < 0) {
            return new GlobalStatEntry("-", 0L);
        }

        return new GlobalStatEntry(bestUsername, bestValue);
    }

    private String resolveUsername(PlayerStats stats) {
        if (stats == null || stats.getUser() == null) {
            return "unknown";
        }

        String username = stats.getUser().getUsername();
        if (username == null || username.isBlank()) {
            return "unknown";
        }

        return username;
    }

    private long toLong(Integer value) {
        return value == null ? 0L : Math.max(0, value);
    }

    public static class GlobalStatsResponse {
        private final GlobalStatEntry highestMatchesPlayed;
        private final GlobalStatEntry highestKillCount;
        private final GlobalStatEntry highestTimePlayed;

        public GlobalStatsResponse(
                GlobalStatEntry highestMatchesPlayed,
                GlobalStatEntry highestKillCount,
                GlobalStatEntry highestTimePlayed) {
            this.highestMatchesPlayed = highestMatchesPlayed;
            this.highestKillCount = highestKillCount;
            this.highestTimePlayed = highestTimePlayed;
        }

        public GlobalStatEntry getHighestMatchesPlayed() {
            return highestMatchesPlayed;
        }

        public GlobalStatEntry getHighestKillCount() {
            return highestKillCount;
        }

        public GlobalStatEntry getHighestTimePlayed() {
            return highestTimePlayed;
        }
    }

    public static class GlobalStatEntry {
        private final String username;
        private final long value;

        public GlobalStatEntry(String username, long value) {
            this.username = username;
            this.value = value;
        }

        public String getUsername() {
            return username;
        }

        public long getValue() {
            return value;
        }
    }
}
