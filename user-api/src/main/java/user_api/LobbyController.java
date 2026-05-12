package user_api;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/lobby")
public class LobbyController {

    private static final long ACTIVE_MS = 15_000;

    private final ConcurrentHashMap<String, LobbyEntry> lobby = new ConcurrentHashMap<>();

    @PostMapping("/ping")
    public LobbyResponse ping(@RequestBody PingRequest req) {
        String username = authenticatedUsername();

        lobby.put(username, new LobbyEntry(
            username,
            req.weapon != null ? req.weapon : "",
            req.armor  != null ? req.armor  : "",
            req.item   != null ? req.item   : "",
            System.currentTimeMillis()
        ));

        long cutoff = System.currentTimeMillis() - ACTIVE_MS;
        lobby.entrySet().removeIf(e -> e.getValue().lastPingAt < cutoff);

        List<PlayerEntry> players = new ArrayList<>();
        for (LobbyEntry e : lobby.values()) {
            players.add(new PlayerEntry(e.username, e.weapon, e.armor, e.item));
        }
        return new LobbyResponse(players);
    }

    @DeleteMapping("/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave() {
        lobby.remove(authenticatedUsername());
    }

    private String authenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return auth.getName();
    }

    private record LobbyEntry(String username, String weapon, String armor, String item, long lastPingAt) {}

    public static class PingRequest {
        public String weapon;
        public String armor;
        public String item;
    }

    public static class PlayerEntry {
        public String username;
        public String weapon;
        public String armor;
        public String item;

        public PlayerEntry(String username, String weapon, String armor, String item) {
            this.username = username;
            this.weapon   = weapon;
            this.armor    = armor;
            this.item     = item;
        }
    }

    public static class LobbyResponse {
        public List<PlayerEntry> players;
        public LobbyResponse(List<PlayerEntry> players) { this.players = players; }
    }
}
