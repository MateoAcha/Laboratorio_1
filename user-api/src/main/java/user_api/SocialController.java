package user_api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SocialController {

    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    @GetMapping("/social/summary")
    public SocialSummaryResponse summary() {
        return socialService.getSummary(authenticatedUsername());
    }

    @PostMapping("/friends/requests")
    public FriendRequestActionResponse sendFriendRequest(@RequestBody FriendRequestCreateRequest request) {
        return socialService.sendFriendRequest(
                authenticatedUsername(),
                request != null ? request.getUsername() : null);
    }

    @PostMapping("/friends/requests/{requestId}/accept")
    public FriendRequestActionResponse acceptFriendRequest(@PathVariable Long requestId) {
        return socialService.acceptFriendRequest(authenticatedUsername(), requestId);
    }

    @PostMapping("/friends/requests/{requestId}/decline")
    public FriendRequestActionResponse declineFriendRequest(@PathVariable Long requestId) {
        return socialService.declineFriendRequest(authenticatedUsername(), requestId);
    }

    @DeleteMapping("/friends/requests/{requestId}")
    public FriendRequestActionResponse cancelFriendRequest(@PathVariable Long requestId) {
        return socialService.cancelFriendRequest(authenticatedUsername(), requestId);
    }

    @DeleteMapping("/friends/{friendUserId}")
    public SocialSummaryResponse removeFriend(@PathVariable Integer friendUserId) {
        return socialService.removeFriend(authenticatedUsername(), friendUserId);
    }

    @PostMapping("/lobby/invites")
    public GameInviteActionResponse sendLobbyInvite(@RequestBody LobbyInviteCreateRequest request) {
        return socialService.sendGameInvite(
                authenticatedUsername(),
                request != null ? request.getUsername() : null,
                request != null ? request.getRoomNumber() : null);
    }

    @PostMapping("/lobby/invites/{inviteId}/accept")
    public GameInviteAcceptResponse acceptLobbyInvite(@PathVariable Long inviteId) {
        return socialService.acceptGameInvite(authenticatedUsername(), inviteId);
    }

    @PostMapping("/lobby/invites/{inviteId}/decline")
    public GameInviteActionResponse declineLobbyInvite(@PathVariable Long inviteId) {
        return socialService.declineGameInvite(authenticatedUsername(), inviteId);
    }

    private String authenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return auth.getName();
    }
}
