package user_api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class UserControllerDailyCoinsTests {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unauthenticatedClaimFailsBeforeServiceCall() {
        DailyCoinsService dailyCoinsService = mock(DailyCoinsService.class);
        UserController controller = new UserController(
                mock(UserRepository.class),
                mock(PlayerStatsRepository.class),
                mock(PasswordEncoder.class),
                mock(JwtService.class),
                mock(InventoryService.class),
                mock(SkinService.class),
                dailyCoinsService);

        SecurityContextHolder.clearContext();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                controller::claimDailyCoins);

        assertEquals(401, exception.getStatusCode().value());
        verifyNoInteractions(dailyCoinsService);
    }
}
