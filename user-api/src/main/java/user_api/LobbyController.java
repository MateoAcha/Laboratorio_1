package user_api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/lobby")
public class LobbyController {

    private final LobbyRoomService lobbyRoomService;

    public LobbyController(LobbyRoomService lobbyRoomService) {
        this.lobbyRoomService = lobbyRoomService;
    }

    @GetMapping("/rooms")
    public LobbyRoomService.RoomsResponse rooms() {
        authenticatedUsername();
        return lobbyRoomService.rooms();
    }

    @PostMapping("/create")
    public LobbyRoomService.RoomSummary create(@RequestBody(required = false) LobbyRoomService.CreateRequest request) {
        return lobbyRoomService.create(authenticatedUsername(), request);
    }

    @PostMapping("/ping")
    public LobbyRoomService.LobbyResponse ping(@RequestBody LobbyRoomService.PingRequest req) {
        return lobbyRoomService.ping(authenticatedUsername(), req);
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void start(@RequestParam(defaultValue = "1") int roomNumber) {
        lobbyRoomService.start(authenticatedUsername(), roomNumber);
    }

    @DeleteMapping("/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@RequestParam(defaultValue = "1") int roomNumber) {
        lobbyRoomService.leave(authenticatedUsername(), roomNumber);
    }

    public void closeRoom(int roomNumber) {
        lobbyRoomService.closeRoom(roomNumber);
    }

    private String authenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return auth.getName();
    }
}
