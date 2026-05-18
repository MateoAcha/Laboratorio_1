package user_api;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DailyCoinsService {

    public static final int REWARD_COINS = 1000;
    public static final Duration COOLDOWN = Duration.ofHours(24);

    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final Clock clock;

    public DailyCoinsService(UserRepository userRepository, InventoryService inventoryService, Clock clock) {
        this.userRepository = userRepository;
        this.inventoryService = inventoryService;
        this.clock = clock;
    }

    public DailyCoinsResponse getStatus(String username) {
        User user = findAuthenticatedUser(username);
        return buildStatus(user.getLastDailyCoinsClaimedAt(), LocalDateTime.now(clock), null);
    }

    @Transactional
    public DailyCoinsResponse claim(String username) {
        User user = findAuthenticatedUser(username);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime claimableBefore = now.minus(COOLDOWN);

        int updated = userRepository.claimDailyCoins(user.getUserId(), now, claimableBefore);
        if (updated == 0) {
            DailyCoinsResponse status = userRepository.findByUsername(username)
                    .map(current -> buildStatus(current.getLastDailyCoinsClaimedAt(), now, null))
                    .orElseGet(() -> buildStatus(user.getLastDailyCoinsClaimedAt(), now, null));
            status.setMessage("Daily coin reward is on cooldown.");
            throw new DailyCoinsCooldownException(status);
        }

        inventoryService.addCoins(user.getUserId(), REWARD_COINS);
        return buildStatus(now, now, inventoryService.getGoldCoins(user.getUserId()));
    }

    private User findAuthenticatedUser(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    private DailyCoinsResponse buildStatus(LocalDateTime lastClaimedAt, LocalDateTime now, Integer goldCoinTotal) {
        DailyCoinsResponse response = new DailyCoinsResponse();
        response.setRewardCoins(REWARD_COINS);
        response.setLastClaimedAt(lastClaimedAt);
        response.setGoldCoinTotal(goldCoinTotal);

        if (lastClaimedAt == null) {
            response.setClaimable(true);
            response.setRemainingSeconds(0L);
            return response;
        }

        LocalDateTime nextClaimAt = lastClaimedAt.plus(COOLDOWN);
        if (!nextClaimAt.isAfter(now)) {
            response.setClaimable(true);
            response.setRemainingSeconds(0L);
            return response;
        }

        Duration remaining = Duration.between(now, nextClaimAt);
        long remainingSeconds = Math.max(1L, (remaining.toNanos() + 999_999_999L) / 1_000_000_000L);
        response.setClaimable(false);
        response.setRemainingSeconds(remainingSeconds);
        return response;
    }
}
