package user_api;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "friend_request")
public class FriendRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "friend_request_seq_generator")
    @SequenceGenerator(name = "friend_request_seq_generator", sequenceName = "friend_request_seq", allocationSize = 1)
    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "requester_user_id", nullable = false)
    private Integer requesterUserId;

    @Column(name = "recipient_user_id", nullable = false)
    private Integer recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendRequestStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    public FriendRequest() {
    }

    public FriendRequest(Integer requesterUserId, Integer recipientUserId) {
        this.requesterUserId = requesterUserId;
        this.recipientUserId = recipientUserId;
        this.status = FriendRequestStatus.PENDING;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = FriendRequestStatus.PENDING;
        }
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Integer getRequesterUserId() {
        return requesterUserId;
    }

    public void setRequesterUserId(Integer requesterUserId) {
        this.requesterUserId = requesterUserId;
    }

    public Integer getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(Integer recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public FriendRequestStatus getStatus() {
        return status;
    }

    public void setStatus(FriendRequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }

    public void setCanceledAt(LocalDateTime canceledAt) {
        this.canceledAt = canceledAt;
    }
}
