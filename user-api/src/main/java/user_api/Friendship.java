package user_api;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "friendship",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_friendship_pair", columnNames = {"user_one_id", "user_two_id"})
        }
)
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "friendship_seq_generator")
    @SequenceGenerator(name = "friendship_seq_generator", sequenceName = "friendship_seq", allocationSize = 1)
    @Column(name = "friendship_id", nullable = false)
    private Long friendshipId;

    @Column(name = "user_one_id", nullable = false)
    private Integer userOneId;

    @Column(name = "user_two_id", nullable = false)
    private Integer userTwoId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Friendship() {
    }

    public Friendship(Integer firstUserId, Integer secondUserId) {
        if (firstUserId == null || secondUserId == null || firstUserId.equals(secondUserId)) {
            throw new IllegalArgumentException("Friendship requires two distinct users");
        }
        this.userOneId = Math.min(firstUserId, secondUserId);
        this.userTwoId = Math.max(firstUserId, secondUserId);
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Integer otherUserId(Integer userId) {
        if (userOneId != null && userOneId.equals(userId)) {
            return userTwoId;
        }
        if (userTwoId != null && userTwoId.equals(userId)) {
            return userOneId;
        }
        throw new IllegalArgumentException("User is not part of this friendship");
    }

    public Long getFriendshipId() {
        return friendshipId;
    }

    public void setFriendshipId(Long friendshipId) {
        this.friendshipId = friendshipId;
    }

    public Integer getUserOneId() {
        return userOneId;
    }

    public void setUserOneId(Integer userOneId) {
        this.userOneId = userOneId;
    }

    public Integer getUserTwoId() {
        return userTwoId;
    }

    public void setUserTwoId(Integer userTwoId) {
        this.userTwoId = userTwoId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
