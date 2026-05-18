package user_api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DailyCoinsServiceTests {

    private static final String USERNAME = "daily-player";
    private static final Integer USER_ID = 42;
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-17T12:00:00Z"),
            ZoneId.of("UTC"));

    private UserRepository userRepository;
    private InventoryService inventoryService;
    private DailyCoinsService dailyCoinsService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        inventoryService = mock(InventoryService.class);
        dailyCoinsService = new DailyCoinsService(userRepository, inventoryService, CLOCK);
    }

    @Test
    void firstClaimSucceedsAndGrantsCoins() {
        LocalDateTime now = LocalDateTime.now(CLOCK);
        User user = user(null);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(userRepository.claimDailyCoins(
                eq(USER_ID),
                eq(now),
                eq(now.minus(DailyCoinsService.COOLDOWN)))).thenReturn(1);
        when(inventoryService.getGoldCoins(USER_ID)).thenReturn(1250);

        DailyCoinsResponse response = dailyCoinsService.claim(USERNAME);

        assertFalse(response.getClaimable());
        assertEquals(DailyCoinsService.COOLDOWN.toSeconds(), response.getRemainingSeconds());
        assertEquals(DailyCoinsService.REWARD_COINS, response.getRewardCoins());
        assertEquals(now, response.getLastClaimedAt());
        assertEquals(1250, response.getGoldCoinTotal());
        verify(inventoryService).addCoins(USER_ID, DailyCoinsService.REWARD_COINS);
    }

    @Test
    void secondImmediateClaimFailsWithRemainingCooldown() {
        LocalDateTime now = LocalDateTime.now(CLOCK);
        User user = user(now);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(userRepository.claimDailyCoins(
                eq(USER_ID),
                eq(now),
                eq(now.minus(DailyCoinsService.COOLDOWN)))).thenReturn(0);

        DailyCoinsCooldownException exception = assertThrows(
                DailyCoinsCooldownException.class,
                () -> dailyCoinsService.claim(USERNAME));

        DailyCoinsResponse response = exception.getResponse();
        assertFalse(response.getClaimable());
        assertEquals(DailyCoinsService.COOLDOWN.toSeconds(), response.getRemainingSeconds());
        assertEquals(DailyCoinsService.REWARD_COINS, response.getRewardCoins());
        assertEquals(now, response.getLastClaimedAt());
        verify(inventoryService, never()).addCoins(USER_ID, DailyCoinsService.REWARD_COINS);
    }

    @Test
    void claimSucceedsAgainAfterTwentyFourHours() {
        LocalDateTime now = LocalDateTime.now(CLOCK);
        User user = user(now.minus(DailyCoinsService.COOLDOWN));

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(userRepository.claimDailyCoins(
                eq(USER_ID),
                eq(now),
                eq(now.minus(DailyCoinsService.COOLDOWN)))).thenReturn(1);
        when(inventoryService.getGoldCoins(USER_ID)).thenReturn(2250);

        DailyCoinsResponse response = dailyCoinsService.claim(USERNAME);

        assertFalse(response.getClaimable());
        assertEquals(DailyCoinsService.COOLDOWN.toSeconds(), response.getRemainingSeconds());
        assertEquals(now, response.getLastClaimedAt());
        assertEquals(2250, response.getGoldCoinTotal());
        verify(inventoryService).addCoins(USER_ID, DailyCoinsService.REWARD_COINS);
    }

    @Test
    void statusIsClaimableBeforeAnyClaim() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user(null)));

        DailyCoinsResponse response = dailyCoinsService.getStatus(USERNAME);

        assertTrue(response.getClaimable());
        assertEquals(0L, response.getRemainingSeconds());
        assertEquals(DailyCoinsService.REWARD_COINS, response.getRewardCoins());
    }

    private User user(LocalDateTime lastClaimedAt) {
        User user = new User();
        user.setUserId(USER_ID);
        user.setUsername(USERNAME);
        user.setLastDailyCoinsClaimedAt(lastClaimedAt);
        return user;
    }
}
