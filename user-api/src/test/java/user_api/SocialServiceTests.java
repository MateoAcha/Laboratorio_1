package user_api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SocialServiceTests {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-03T12:00:00Z"),
            ZoneId.of("UTC"));

    private UserRepository userRepository;
    private FriendshipRepository friendshipRepository;
    private FriendRequestRepository friendRequestRepository;
    private GameInviteRepository gameInviteRepository;
    private LobbyRoomService lobbyRoomService;
    private SocialService socialService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        friendshipRepository = mock(FriendshipRepository.class);
        friendRequestRepository = mock(FriendRequestRepository.class);
        gameInviteRepository = mock(GameInviteRepository.class);
        lobbyRoomService = mock(LobbyRoomService.class);
        socialService = new SocialService(
                userRepository,
                friendshipRepository,
                friendRequestRepository,
                gameInviteRepository,
                lobbyRoomService,
                CLOCK);
    }

    @Test
    void reciprocalFriendRequestAutoAcceptsIncomingRequestAndCreatesFriendship() {
        User alice = user(1, "alice");
        User bob = user(2, "bob");
        FriendRequest incoming = pendingFriendRequest(42L, bob.getUserId(), alice.getUserId());

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(friendshipRepository.existsByUserOneIdAndUserTwoId(1, 2)).thenReturn(false);
        when(friendRequestRepository.findByRequesterUserIdAndRecipientUserIdAndStatus(
                bob.getUserId(),
                alice.getUserId(),
                FriendRequestStatus.PENDING)).thenReturn(Optional.of(incoming));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(friendshipRepository.findByUserOneIdAndUserTwoId(1, 2)).thenReturn(Optional.empty());
        when(friendshipRepository.saveAndFlush(any(Friendship.class))).thenAnswer(invocation -> {
            Friendship friendship = invocation.getArgument(0);
            friendship.setFriendshipId(100L);
            friendship.setCreatedAt(LocalDateTime.now(CLOCK));
            return friendship;
        });
        mockEmptySummary(alice);

        FriendRequestActionResponse response = socialService.sendFriendRequest("alice", "bob");

        assertEquals("FRIENDSHIP_CREATED", response.getResult());
        assertEquals(FriendRequestStatus.ACCEPTED, incoming.getStatus());
        assertEquals(LocalDateTime.now(CLOCK), incoming.getRespondedAt());
        assertEquals(bob.getUserId(), response.getFriendRequest().getRequesterUserId());
        assertEquals("bob", response.getFriendRequest().getRequesterUsername());
        assertEquals(alice.getUserId(), response.getFriendRequest().getRecipientUserId());
        assertEquals("alice", response.getFriendRequest().getRecipientUsername());
        assertEquals(2, response.getFriendship().getFriendUserId());
        verify(friendshipRepository).saveAndFlush(any(Friendship.class));
        verify(friendRequestRepository, never()).saveAndFlush(any(FriendRequest.class));
    }

    @Test
    void duplicatePendingFriendRequestInSameDirectionIsRejected() {
        User alice = user(1, "alice");
        User bob = user(2, "bob");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(friendshipRepository.existsByUserOneIdAndUserTwoId(1, 2)).thenReturn(false);
        when(friendRequestRepository.findByRequesterUserIdAndRecipientUserIdAndStatus(
                bob.getUserId(),
                alice.getUserId(),
                FriendRequestStatus.PENDING)).thenReturn(Optional.empty());
        when(friendRequestRepository.findByRequesterUserIdAndRecipientUserIdAndStatus(
                alice.getUserId(),
                bob.getUserId(),
                FriendRequestStatus.PENDING)).thenReturn(Optional.of(pendingFriendRequest(10L, 1, 2)));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> socialService.sendFriendRequest("alice", "bob"));

        assertEquals(409, exception.getStatusCode().value());
        verify(friendRequestRepository, never()).saveAndFlush(any(FriendRequest.class));
    }

    @Test
    void duplicatePendingGameInviteForSameHostRecipientAndRoomIsRejected() {
        User alice = user(1, "alice");
        User bob = user(2, "bob");
        GameInvite duplicate = pendingInvite(77L, alice.getUserId(), bob.getUserId(), 123);
        duplicate.setExpiresAt(LocalDateTime.now(CLOCK).plusSeconds(30));

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(lobbyRoomService.hasActiveRoom(123)).thenReturn(true);
        when(lobbyRoomService.isActiveHost("alice", 123)).thenReturn(true);
        when(friendshipRepository.existsByUserOneIdAndUserTwoId(1, 2)).thenReturn(true);
        when(gameInviteRepository.findByHostUserIdAndRecipientUserIdAndRoomNumberAndStatusAndExpiresAtAfter(
                alice.getUserId(),
                bob.getUserId(),
                123,
                GameInviteStatus.PENDING,
                LocalDateTime.now(CLOCK))).thenReturn(Optional.of(duplicate));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> socialService.sendGameInvite("alice", "bob", 123));

        assertEquals(409, exception.getStatusCode().value());
        verify(gameInviteRepository, never()).saveAndFlush(any(GameInvite.class));
    }

    @Test
    void acceptingExpiredGameInviteMarksExpiredAndReturnsConflict() {
        User bob = user(2, "bob");
        GameInvite invite = pendingInvite(77L, 1, bob.getUserId(), 123);
        invite.setExpiresAt(LocalDateTime.now(CLOCK).minusSeconds(1));

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(gameInviteRepository.findById(77L)).thenReturn(Optional.of(invite));
        when(gameInviteRepository.save(any(GameInvite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> socialService.acceptGameInvite("bob", 77L));

        assertEquals(409, exception.getStatusCode().value());
        assertEquals(GameInviteStatus.EXPIRED, invite.getStatus());
        verify(gameInviteRepository).save(invite);
    }

    @Test
    void nonHostCannotSendInviteForActiveRoom() {
        User alice = user(1, "alice");
        User bob = user(2, "bob");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(lobbyRoomService.hasActiveRoom(123)).thenReturn(true);
        when(lobbyRoomService.isActiveHost("alice", 123)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> socialService.sendGameInvite("alice", "bob", 123));

        assertEquals(403, exception.getStatusCode().value());
        verify(gameInviteRepository, never()).saveAndFlush(any(GameInvite.class));
    }

    @Test
    void friendRequestCanOnlyBeAcceptedByRecipient() {
        User alice = user(1, "alice");
        FriendRequest request = pendingFriendRequest(55L, alice.getUserId(), 2);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(friendRequestRepository.findById(55L)).thenReturn(Optional.of(request));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> socialService.acceptFriendRequest("alice", 55L));

        assertEquals(403, exception.getStatusCode().value());
        verify(friendRequestRepository, never()).save(any(FriendRequest.class));
    }

    @Test
    void inviteCanOnlyBeAcceptedByRecipient() {
        User alice = user(1, "alice");
        GameInvite invite = pendingInvite(77L, alice.getUserId(), 2, 123);
        invite.setExpiresAt(LocalDateTime.now(CLOCK).plusSeconds(30));

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(gameInviteRepository.findById(77L)).thenReturn(Optional.of(invite));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> socialService.acceptGameInvite("alice", 77L));

        assertEquals(403, exception.getStatusCode().value());
        verify(gameInviteRepository, never()).save(any(GameInvite.class));
    }

    private void mockEmptySummary(User user) {
        when(friendshipRepository.findForUser(user.getUserId())).thenReturn(List.of());
        when(friendRequestRepository.findByRecipientUserIdAndStatusOrderByCreatedAtDesc(
                user.getUserId(),
                FriendRequestStatus.PENDING)).thenReturn(List.of());
        when(friendRequestRepository.findByRequesterUserIdAndStatusOrderByCreatedAtDesc(
                user.getUserId(),
                FriendRequestStatus.PENDING)).thenReturn(List.of());
        when(gameInviteRepository.findByRecipientUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                user.getUserId(),
                GameInviteStatus.PENDING,
                LocalDateTime.now(CLOCK))).thenReturn(List.of());
    }

    private User user(Integer userId, String username) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);
        return user;
    }

    private FriendRequest pendingFriendRequest(Long requestId, Integer requesterUserId, Integer recipientUserId) {
        FriendRequest request = new FriendRequest(requesterUserId, recipientUserId);
        request.setRequestId(requestId);
        request.setCreatedAt(LocalDateTime.now(CLOCK).minusSeconds(30));
        return request;
    }

    private GameInvite pendingInvite(Long inviteId, Integer hostUserId, Integer recipientUserId, Integer roomNumber) {
        GameInvite invite = new GameInvite(
                hostUserId,
                recipientUserId,
                roomNumber,
                LocalDateTime.now(CLOCK).plus(SocialService.GAME_INVITE_TTL));
        invite.setInviteId(inviteId);
        invite.setCreatedAt(LocalDateTime.now(CLOCK).minusSeconds(30));
        return invite;
    }
}
