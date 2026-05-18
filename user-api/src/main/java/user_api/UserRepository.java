package user_api;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findTopByOrderByUserIdDesc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE User u
            SET u.lastDailyCoinsClaimedAt = :claimedAt
            WHERE u.userId = :userId
              AND (u.lastDailyCoinsClaimedAt IS NULL OR u.lastDailyCoinsClaimedAt <= :claimableBefore)
            """)
    int claimDailyCoins(
            @Param("userId") Integer userId,
            @Param("claimedAt") LocalDateTime claimedAt,
            @Param("claimableBefore") LocalDateTime claimableBefore);
}
