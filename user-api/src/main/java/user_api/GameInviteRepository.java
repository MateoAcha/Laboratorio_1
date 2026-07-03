package user_api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameInviteRepository extends JpaRepository<GameInvite, Long> {

    Optional<GameInvite> findByHostUserIdAndRecipientUserIdAndRoomNumberAndStatusAndExpiresAtAfter(
            Integer hostUserId,
            Integer recipientUserId,
            Integer roomNumber,
            GameInviteStatus status,
            LocalDateTime now);

    List<GameInvite> findByRecipientUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            Integer recipientUserId,
            GameInviteStatus status,
            LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE GameInvite i
            SET i.status = :expired
            WHERE i.status = :pending
              AND i.expiresAt <= :now
            """)
    int markExpired(
            @Param("now") LocalDateTime now,
            @Param("pending") GameInviteStatus pending,
            @Param("expired") GameInviteStatus expired);
}
