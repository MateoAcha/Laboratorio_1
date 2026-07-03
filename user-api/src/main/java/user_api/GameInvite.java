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
@Table(name = "game_invite")
public class GameInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_invite_seq_generator")
    @SequenceGenerator(name = "game_invite_seq_generator", sequenceName = "game_invite_seq", allocationSize = 1)
    @Column(name = "invite_id", nullable = false)
    private Long inviteId;

    @Column(name = "host_user_id", nullable = false)
    private Integer hostUserId;

    @Column(name = "recipient_user_id", nullable = false)
    private Integer recipientUserId;

    @Column(name = "room_number", nullable = false)
    private Integer roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameInviteStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    public GameInvite() {
    }

    public GameInvite(Integer hostUserId, Integer recipientUserId, Integer roomNumber, LocalDateTime expiresAt) {
        this.hostUserId = hostUserId;
        this.recipientUserId = recipientUserId;
        this.roomNumber = roomNumber;
        this.expiresAt = expiresAt;
        this.status = GameInviteStatus.PENDING;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = GameInviteStatus.PENDING;
        }
    }

    public Long getInviteId() {
        return inviteId;
    }

    public void setInviteId(Long inviteId) {
        this.inviteId = inviteId;
    }

    public Integer getHostUserId() {
        return hostUserId;
    }

    public void setHostUserId(Integer hostUserId) {
        this.hostUserId = hostUserId;
    }

    public Integer getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(Integer recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public Integer getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(Integer roomNumber) {
        this.roomNumber = roomNumber;
    }

    public GameInviteStatus getStatus() {
        return status;
    }

    public void setStatus(GameInviteStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
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
