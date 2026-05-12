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
    private static final long STARTED_TTL_MS = 30_000;

    private final ConcurrentHashMap<String, LobbyEntry> lobby = new ConcurrentHashMap<>();
    private volatile long startedAt = 0;

    @PostMapping("/ping")
    public LobbyResponse ping(@RequestBody PingRequest req) {
        String username = authenticatedUsername();

        lobby.put(username, new LobbyEntry(
            username,
            req.weapon != null ? req.weapon : "",
            req.armor  != null ? req.armor  : "",
            req.item   != null ? req.item   : "",
            req.x, req.y,
            System.currentTimeMillis()
        ));

        long now = System.currentTimeMillis();
        long cutoff = now - ACTIVE_MS;
        lobby.entrySet().removeIf(e -> e.getValue().lastPingAt < cutoff);

        if (lobby.isEmpty()) startedAt = 0;

        boolean started = startedAt > 0 && (now - startedAt) < STARTED_TTL_MS;

        List<PlayerEntry> players = new ArrayList<>();
        for (LobbyEntry e : lobby.values()) {
            players.add(new PlayerEntry(e.username, e.weapon, e.armor, e.item, e.x, e.y));
        }
        return new LobbyResponse(players, started);
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void start() {
        authenticatedUsername();
        startedAt = System.currentTimeMillis();
    }

    @DeleteMapping("/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave() {
        lobby.remove(authenticatedUsername());
        if (lobby.isEmpty()) startedAt = 0;
    }

    private String authenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return auth.getName();
    }

    private record LobbyEntry(String username, String weapon, String armor, String item,
                               float x, float y, long lastPingAt) {}

    public static class PingRequest {
        public String weapon;
        public String armor;
        public String item;
        public float x;
        public float y;
    }

    public static class PlayerEntry {
        public String username;
        public String weapon;
        public String armor;
        public String item;
        public float x;
        public float y;

        public PlayerEntry(String username, String weapon, String armor, String item, float x, float y) {
            this.username = username;
            this.weapon   = weapon;
            this.armor    = armor;
            this.item     = item;
            this.x        = x;
            this.y        = y;
        }
    }

    public static class LobbyResponse {
        public List<PlayerEntry> players;
        public boolean started;

        public LobbyResponse(List<PlayerEntry> players, boolean started) {
            this.players = players;
            this.started = started;
        }
    }
}
