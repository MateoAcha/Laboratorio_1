package user_api;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SocialService {

    static final Duration GAME_INVITE_TTL = Duration.ofMinutes(2);

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final GameInviteRepository gameInviteRepository;
    private final LobbyRoomService lobbyRoomService;
    private final Clock clock;

    public SocialService(
            UserRepository userRepository,
            FriendshipRepository friendshipRepository,
            FriendRequestRepository friendRequestRepository,
            GameInviteRepository gameInviteRepository,
            LobbyRoomService lobbyRoomService,
            Clock clock) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.gameInviteRepository = gameInviteRepository;
        this.lobbyRoomService = lobbyRoomService;
        this.clock = clock;
    }

    @Transactional
    public SocialSummaryResponse getSummary(String username) {
        User currentUser = findAuthenticatedUser(username);
        expirePendingInvites();
        return buildSummary(currentUser);
    }

    @Transactional
    public FriendRequestActionResponse sendFriendRequest(String username, String targetUsername) {
        User currentUser = findAuthenticatedUser(username);
        User targetUser = findTargetUser(targetUsername);

        if (currentUser.getUserId().equals(targetUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot friend yourself");
        }

        if (friendshipExists(currentUser.getUserId(), targetUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already friends");
        }

        Optional<FriendRequest> existingIncoming = friendRequestRepository
                .findByRequesterUserIdAndRecipientUserIdAndStatus(
                        targetUser.getUserId(),
                        currentUser.getUserId(),
                        FriendRequestStatus.PENDING);

        if (existingIncoming.isPresent()) {
            LocalDateTime now = now();
            FriendRequest request = existingIncoming.get();
            request.setStatus(FriendRequestStatus.ACCEPTED);
            request.setRespondedAt(now);
            FriendRequest savedRequest = friendRequestRepository.save(request);
            Friendship friendship = createFriendship(currentUser.getUserId(), targetUser.getUserId());
            return friendRequestAction(
                    "FRIENDSHIP_CREATED",
                    savedRequest,
                    friendship,
                    targetUser,
                    currentUser,
                    currentUser);
        }

        if (friendRequestRepository.findByRequesterUserIdAndRecipientUserIdAndStatus(
                currentUser.getUserId(),
                targetUser.getUserId(),
                FriendRequestStatus.PENDING).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friend request already pending");
        }

        FriendRequest request = new FriendRequest(currentUser.getUserId(), targetUser.getUserId());
        FriendRequest savedRequest;
        try {
            savedRequest = friendRequestRepository.saveAndFlush(request);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friend request already pending", ex);
        }

        return friendRequestAction("REQUEST_SENT", savedRequest, null, currentUser, targetUser, currentUser);
    }

    @Transactional
    public FriendRequestActionResponse acceptFriendRequest(String username, Long requestId) {
        User currentUser = findAuthenticatedUser(username);
        FriendRequest request = findFriendRequest(requestId);
        ensureFriendRequestRecipient(request, currentUser);
        ensurePendingFriendRequest(request);

        LocalDateTime now = now();
        request.setStatus(FriendRequestStatus.ACCEPTED);
        request.setRespondedAt(now);
        FriendRequest savedRequest = friendRequestRepository.save(request);
        Friendship friendship = createFriendship(request.getRequesterUserId(), request.getRecipientUserId());
        User requester = findUserById(request.getRequesterUserId());
        return friendRequestAction("FRIENDSHIP_CREATED", savedRequest, friendship, requester, currentUser, currentUser);
    }

    @Transactional
    public FriendRequestActionResponse declineFriendRequest(String username, Long requestId) {
        User currentUser = findAuthenticatedUser(username);
        FriendRequest request = findFriendRequest(requestId);
        ensureFriendRequestRecipient(request, currentUser);
        ensurePendingFriendRequest(request);

        request.setStatus(FriendRequestStatus.DECLINED);
        request.setRespondedAt(now());
        FriendRequest savedRequest = friendRequestRepository.save(request);
        User requester = findUserById(request.getRequesterUserId());
        return friendRequestAction("DECLINED", savedRequest, null, requester, currentUser, currentUser);
    }

    @Transactional
    public FriendRequestActionResponse cancelFriendRequest(String username, Long requestId) {
        User currentUser = findAuthenticatedUser(username);
        FriendRequest request = findFriendRequest(requestId);
        if (!currentUser.getUserId().equals(request.getRequesterUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the requester can cancel this request");
        }
        ensurePendingFriendRequest(request);

        request.setStatus(FriendRequestStatus.CANCELED);
        request.setCanceledAt(now());
        FriendRequest savedRequest = friendRequestRepository.save(request);
        User recipient = findUserById(request.getRecipientUserId());
        return friendRequestAction("CANCELED", savedRequest, null, currentUser, recipient, currentUser);
    }

    @Transactional
    public SocialSummaryResponse removeFriend(String username, Integer friendUserId) {
        User currentUser = findAuthenticatedUser(username);
        if (friendUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "friendUserId is required");
        }
        if (currentUser.getUserId().equals(friendUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot remove yourself as a friend");
        }

        FriendshipKey key = friendshipKey(currentUser.getUserId(), friendUserId);
        Friendship friendship = friendshipRepository.findByUserOneIdAndUserTwoId(key.userOneId(), key.userTwoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friendship not found"));

        friendshipRepository.delete(friendship);
        expirePendingInvites();
        return buildSummary(currentUser);
    }

    @Transactional
    public GameInviteActionResponse sendGameInvite(String username, String targetUsername, Integer roomNumber) {
        User hostUser = findAuthenticatedUser(username);
        User targetUser = findTargetUser(targetUsername);
        int safeRoomNumber = requireRoomNumber(roomNumber);

        if (hostUser.getUserId().equals(targetUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot invite yourself");
        }

        if (!lobbyRoomService.hasActiveRoom(safeRoomNumber)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
        }
        if (!lobbyRoomService.isActiveHost(hostUser.getUsername(), safeRoomNumber)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host of this active lobby can invite players");
        }

        if (!friendshipExists(hostUser.getUserId(), targetUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only friends can be invited");
        }

        LocalDateTime now = now();
        expirePendingInvites(now);

        if (gameInviteRepository.findByHostUserIdAndRecipientUserIdAndRoomNumberAndStatusAndExpiresAtAfter(
                hostUser.getUserId(),
                targetUser.getUserId(),
                safeRoomNumber,
                GameInviteStatus.PENDING,
                now).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game invite already pending");
        }

        GameInvite invite = new GameInvite(
                hostUser.getUserId(),
                targetUser.getUserId(),
                safeRoomNumber,
                now.plus(GAME_INVITE_TTL));
        GameInvite savedInvite;
        try {
            savedInvite = gameInviteRepository.saveAndFlush(invite);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game invite already pending", ex);
        }

        GameInviteActionResponse response = new GameInviteActionResponse();
        response.setResult("INVITE_SENT");
        response.setGameInvite(toGameInviteResponse(savedInvite, hostUser, targetUser));
        response.setSummary(buildSummary(hostUser));
        return response;
    }

    @Transactional
    public GameInviteAcceptResponse acceptGameInvite(String username, Long inviteId) {
        User currentUser = findAuthenticatedUser(username);
        GameInvite invite = findGameInvite(inviteId);
        ensureGameInviteRecipient(invite, currentUser);
        ensurePendingGameInvite(invite);

        LocalDateTime now = now();
        if (!invite.getExpiresAt().isAfter(now)) {
            invite.setStatus(GameInviteStatus.EXPIRED);
            gameInviteRepository.save(invite);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game invite has expired");
        }

        LobbyRoomService.RoomSummary lobby = lobbyRoomService.findActiveRoomSummary(invite.getRoomNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Lobby is no longer active"));
        lobbyRoomService.authorizeInviteJoin(currentUser.getUsername(), invite.getRoomNumber());

        User hostUser = findUserById(invite.getHostUserId());
        invite.setStatus(GameInviteStatus.ACCEPTED);
        invite.setRespondedAt(now);
        GameInvite savedInvite = gameInviteRepository.save(invite);

        GameInviteAcceptResponse response = new GameInviteAcceptResponse();
        response.setResult("ACCEPTED");
        response.setInviteId(savedInvite.getInviteId());
        response.setRoomNumber(savedInvite.getRoomNumber());
        response.setHostUserId(hostUser.getUserId());
        response.setHostUsername(hostUser.getUsername());
        response.setGameInvite(toGameInviteResponse(savedInvite, hostUser, currentUser));
        response.setLobby(lobby);
        return response;
    }

    @Transactional
    public GameInviteActionResponse declineGameInvite(String username, Long inviteId) {
        User currentUser = findAuthenticatedUser(username);
        GameInvite invite = findGameInvite(inviteId);
        ensureGameInviteRecipient(invite, currentUser);
        ensurePendingGameInvite(invite);

        LocalDateTime now = now();
        if (!invite.getExpiresAt().isAfter(now)) {
            invite.setStatus(GameInviteStatus.EXPIRED);
            gameInviteRepository.save(invite);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game invite has expired");
        }

        User hostUser = findUserById(invite.getHostUserId());
        invite.setStatus(GameInviteStatus.DECLINED);
        invite.setRespondedAt(now);
        GameInvite savedInvite = gameInviteRepository.save(invite);

        GameInviteActionResponse response = new GameInviteActionResponse();
        response.setResult("DECLINED");
        response.setGameInvite(toGameInviteResponse(savedInvite, hostUser, currentUser));
        response.setSummary(buildSummary(currentUser));
        return response;
    }

    private FriendRequestActionResponse friendRequestAction(
            String result,
            FriendRequest request,
            Friendship friendship,
            User requester,
            User recipient,
            User summaryUser) {
        FriendRequestActionResponse response = new FriendRequestActionResponse();
        response.setResult(result);
        response.setFriendRequest(toFriendRequestResponse(request, requester, recipient));
        if (friendship != null) {
            Integer friendUserId = friendship.otherUserId(summaryUser.getUserId());
            User friendUser = friendUserId.equals(requester.getUserId()) ? requester : recipient;
            response.setFriendship(toFriendSummaryResponse(friendship, friendUser));
        }
        response.setSummary(buildSummary(summaryUser));
        return response;
    }

    private SocialSummaryResponse buildSummary(User currentUser) {
        LocalDateTime now = now();
        expirePendingInvites(now);

        List<Friendship> friendships = friendshipRepository.findForUser(currentUser.getUserId());
        List<FriendRequest> incomingRequests = friendRequestRepository
                .findByRecipientUserIdAndStatusOrderByCreatedAtDesc(
                        currentUser.getUserId(),
                        FriendRequestStatus.PENDING);
        List<FriendRequest> sentRequests = friendRequestRepository
                .findByRequesterUserIdAndStatusOrderByCreatedAtDesc(
                        currentUser.getUserId(),
                        FriendRequestStatus.PENDING);
        List<GameInvite> gameInvites = gameInviteRepository
                .findByRecipientUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        currentUser.getUserId(),
                        GameInviteStatus.PENDING,
                        now);

        Set<Integer> userIds = new HashSet<>();
        for (Friendship friendship : friendships) {
            userIds.add(friendship.getUserOneId());
            userIds.add(friendship.getUserTwoId());
        }
        collectFriendRequestUserIds(incomingRequests, userIds);
        collectFriendRequestUserIds(sentRequests, userIds);
        collectGameInviteUserIds(gameInvites, userIds);
        userIds.add(currentUser.getUserId());

        Map<Integer, User> users = usersById(userIds);
        users.put(currentUser.getUserId(), currentUser);

        SocialSummaryResponse response = new SocialSummaryResponse();
        response.setFriends(friendships.stream()
                .map(friendship -> toFriendSummaryResponse(
                        friendship,
                        users.get(friendship.otherUserId(currentUser.getUserId()))))
                .filter(friend -> friend.getFriendUserId() != null)
                .collect(Collectors.toList()));
        response.setIncomingFriendRequests(incomingRequests.stream()
                .map(request -> toFriendRequestResponse(
                        request,
                        users.get(request.getRequesterUserId()),
                        users.get(request.getRecipientUserId())))
                .collect(Collectors.toList()));
        response.setSentFriendRequests(sentRequests.stream()
                .map(request -> toFriendRequestResponse(
                        request,
                        users.get(request.getRequesterUserId()),
                        users.get(request.getRecipientUserId())))
                .collect(Collectors.toList()));
        response.setGameInvites(gameInvites.stream()
                .map(invite -> toGameInviteResponse(
                        invite,
                        users.get(invite.getHostUserId()),
                        users.get(invite.getRecipientUserId())))
                .collect(Collectors.toList()));
        return response;
    }

    private void collectFriendRequestUserIds(List<FriendRequest> requests, Set<Integer> userIds) {
        for (FriendRequest request : requests) {
            userIds.add(request.getRequesterUserId());
            userIds.add(request.getRecipientUserId());
        }
    }

    private void collectGameInviteUserIds(List<GameInvite> invites, Set<Integer> userIds) {
        for (GameInvite invite : invites) {
            userIds.add(invite.getHostUserId());
            userIds.add(invite.getRecipientUserId());
        }
    }

    private Map<Integer, User> usersById(Collection<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, User> users = new HashMap<>();
        for (User user : userRepository.findAllById(userIds)) {
            users.put(user.getUserId(), user);
        }
        return users;
    }

    private Friendship createFriendship(Integer firstUserId, Integer secondUserId) {
        FriendshipKey key = friendshipKey(firstUserId, secondUserId);
        return friendshipRepository.findByUserOneIdAndUserTwoId(key.userOneId(), key.userTwoId())
                .orElseGet(() -> {
                    try {
                        return friendshipRepository.saveAndFlush(new Friendship(firstUserId, secondUserId));
                    } catch (DataIntegrityViolationException ex) {
                        return friendshipRepository.findByUserOneIdAndUserTwoId(key.userOneId(), key.userTwoId())
                                .orElseThrow(() -> ex);
                    }
                });
    }

    private boolean friendshipExists(Integer firstUserId, Integer secondUserId) {
        FriendshipKey key = friendshipKey(firstUserId, secondUserId);
        return friendshipRepository.existsByUserOneIdAndUserTwoId(key.userOneId(), key.userTwoId());
    }

    private FriendshipKey friendshipKey(Integer firstUserId, Integer secondUserId) {
        if (firstUserId == null || secondUserId == null || firstUserId.equals(secondUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Friendship requires two distinct users");
        }
        return new FriendshipKey(Math.min(firstUserId, secondUserId), Math.max(firstUserId, secondUserId));
    }

    private FriendRequest findFriendRequest(Long requestId) {
        if (requestId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "requestId is required");
        }
        return friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found"));
    }

    private GameInvite findGameInvite(Long inviteId) {
        if (inviteId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "inviteId is required");
        }
        return gameInviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game invite not found"));
    }

    private void ensureFriendRequestRecipient(FriendRequest request, User currentUser) {
        if (!currentUser.getUserId().equals(request.getRecipientUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the recipient can respond to this request");
        }
    }

    private void ensurePendingFriendRequest(FriendRequest request) {
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friend request is not pending");
        }
    }

    private void ensureGameInviteRecipient(GameInvite invite, User currentUser) {
        if (!currentUser.getUserId().equals(invite.getRecipientUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the recipient can respond to this invite");
        }
    }

    private void ensurePendingGameInvite(GameInvite invite) {
        if (invite.getStatus() != GameInviteStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game invite is not pending");
        }
    }

    private User findAuthenticatedUser(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    private User findTargetUser(String username) {
        String safeUsername = requireText("username", username);
        return userRepository.findByUsername(safeUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private User findUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be a non-empty string");
        }
        return value.trim();
    }

    private int requireRoomNumber(Integer roomNumber) {
        if (roomNumber == null || roomNumber <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roomNumber must be > 0");
        }
        return roomNumber;
    }

    private void expirePendingInvites() {
        expirePendingInvites(now());
    }

    private void expirePendingInvites(LocalDateTime now) {
        gameInviteRepository.markExpired(now, GameInviteStatus.PENDING, GameInviteStatus.EXPIRED);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private FriendSummaryResponse toFriendSummaryResponse(Friendship friendship, User friendUser) {
        FriendSummaryResponse response = new FriendSummaryResponse();
        response.setFriendshipId(friendship.getFriendshipId());
        if (friendUser != null) {
            response.setFriendUserId(friendUser.getUserId());
            response.setFriendUsername(friendUser.getUsername());
        }
        response.setCreatedAt(toText(friendship.getCreatedAt()));
        return response;
    }

    private FriendRequestResponse toFriendRequestResponse(FriendRequest request, User requester, User recipient) {
        FriendRequestResponse response = new FriendRequestResponse();
        response.setRequestId(request.getRequestId());
        response.setRequesterUserId(request.getRequesterUserId());
        response.setRequesterUsername(requester != null ? requester.getUsername() : "");
        response.setRecipientUserId(request.getRecipientUserId());
        response.setRecipientUsername(recipient != null ? recipient.getUsername() : "");
        response.setStatus(request.getStatus().name());
        response.setCreatedAt(toText(request.getCreatedAt()));
        response.setRespondedAt(toText(request.getRespondedAt()));
        response.setCanceledAt(toText(request.getCanceledAt()));
        return response;
    }

    private GameInviteResponse toGameInviteResponse(GameInvite invite, User host, User recipient) {
        GameInviteResponse response = new GameInviteResponse();
        response.setInviteId(invite.getInviteId());
        response.setHostUserId(invite.getHostUserId());
        response.setHostUsername(host != null ? host.getUsername() : "");
        response.setRecipientUserId(invite.getRecipientUserId());
        response.setRecipientUsername(recipient != null ? recipient.getUsername() : "");
        response.setRoomNumber(invite.getRoomNumber());
        response.setStatus(invite.getStatus().name());
        response.setCreatedAt(toText(invite.getCreatedAt()));
        response.setExpiresAt(toText(invite.getExpiresAt()));
        response.setRespondedAt(toText(invite.getRespondedAt()));
        response.setCanceledAt(toText(invite.getCanceledAt()));
        return response;
    }

    private String toText(LocalDateTime value) {
        return value != null ? value.toString() : null;
    }

    private record FriendshipKey(Integer userOneId, Integer userTwoId) {
    }
}
