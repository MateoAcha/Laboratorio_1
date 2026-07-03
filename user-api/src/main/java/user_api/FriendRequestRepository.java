package user_api;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    Optional<FriendRequest> findByRequesterUserIdAndRecipientUserIdAndStatus(
            Integer requesterUserId,
            Integer recipientUserId,
            FriendRequestStatus status);

    List<FriendRequest> findByRecipientUserIdAndStatusOrderByCreatedAtDesc(
            Integer recipientUserId,
            FriendRequestStatus status);

    List<FriendRequest> findByRequesterUserIdAndStatusOrderByCreatedAtDesc(
            Integer requesterUserId,
            FriendRequestStatus status);
}
