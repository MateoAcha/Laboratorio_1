package user_api;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/lobby")
public class LobbyController {

    private static final long ACTIVE_MS = 15_000;
    private static final long STARTED_TTL_MS = 30_000;
    private static final int MAX_PLAYERS = 2;

    private final ConcurrentHashMap<Integer, RoomEntry> rooms = new ConcurrentHashMap<>();
    private final AtomicInteger nextRoomNumber = new AtomicInteger(1);

    @GetMapping("/rooms")
    public RoomsResponse rooms() {
        authenticatedUsername();
        pruneInactiveRooms();

        List<RoomSummary> summaries = new ArrayList<>();
        for (RoomEntry room : rooms.values()) {
            summaries.add(room.toSummary());
        }
        summaries.sort(Comparator.comparingInt(r -> r.roomNumber));
        return new RoomsResponse(summaries);
    }

    @PostMapping("/create")
    public RoomSummary create() {
        String username = authenticatedUsername();
        pruneInactiveRooms();

        int roomNumber;
        do {
            roomNumber = nextRoomNumber.getAndIncrement();
        } while (rooms.containsKey(roomNumber));

        RoomEntry room = new RoomEntry(roomNumber);
        room.players.put(username, new LobbyEntry(username, "", "", "", 0f, 0f, System.currentTimeMillis()));
        rooms.put(roomNumber, room);
        return room.toSummary();
    }

    @PostMapping("/ping")
    public LobbyResponse ping(@RequestBody PingRequest req) {
        String username = authenticatedUsername();
        int roomNumber = req.roomNumber > 0 ? req.roomNumber : 1;
        RoomEntry room = rooms.computeIfAbsent(roomNumber, RoomEntry::new);
        pruneInactiveRoom(room);

        if (!room.players.containsKey(username) && room.players.size() >= MAX_PLAYERS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby is full.");
        }

        room.players.put(username, new LobbyEntry(
            username,
            req.weapon != null ? req.weapon : "",
            req.armor  != null ? req.armor  : "",
            req.item   != null ? req.item   : "",
            req.x, req.y,
            System.currentTimeMillis()
        ));

        long now = System.currentTimeMillis();
        pruneInactiveRoom(room);
        if (room.players.isEmpty()) {
            rooms.remove(roomNumber);
            return new LobbyResponse(new ArrayList<>(), false, roomNumber);
        }

        boolean started = room.startedAt > 0 && (now - room.startedAt) < STARTED_TTL_MS;

        List<PlayerEntry> players = new ArrayList<>();
        for (LobbyEntry e : room.players.values()) {
            players.add(new PlayerEntry(e.username, e.weapon, e.armor, e.item, e.x, e.y));
        }
        return new LobbyResponse(players, started, roomNumber);
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void start(@RequestParam(defaultValue = "1") int roomNumber) {
        authenticatedUsername();
        RoomEntry room = rooms.get(roomNumber);
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found.");
        }
        room.startedAt = System.currentTimeMillis();
    }

    @DeleteMapping("/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@RequestParam(defaultValue = "1") int roomNumber) {
        RoomEntry room = rooms.get(roomNumber);
        if (room == null) return;

        room.players.remove(authenticatedUsername());
        if (room.players.isEmpty()) {
            rooms.remove(roomNumber);
        }
    }

    private void pruneInactiveRooms() {
        for (RoomEntry room : rooms.values()) {
            pruneInactiveRoom(room);
        }
        rooms.entrySet().removeIf(e -> e.getValue().players.isEmpty());
    }

    private void pruneInactiveRoom(RoomEntry room) {
        long now = System.currentTimeMillis();
        long cutoff = now - ACTIVE_MS;
        room.players.entrySet().removeIf(e -> e.getValue().lastPingAt < cutoff);
        if (room.players.isEmpty()) room.startedAt = 0;
    }

    private String authenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return auth.getName();
    }

    private record LobbyEntry(String username, String weapon, String armor, String item,
                               float x, float y, long lastPingAt) {}

    private static class RoomEntry {
        final int roomNumber;
        final ConcurrentHashMap<String, LobbyEntry> players = new ConcurrentHashMap<>();
        volatile long startedAt = 0;

        RoomEntry(int roomNumber) {
            this.roomNumber = roomNumber;
        }

        RoomSummary toSummary() {
            List<PlayerEntry> playerEntries = new ArrayList<>();
            for (LobbyEntry e : players.values()) {
                playerEntries.add(new PlayerEntry(e.username, e.weapon, e.armor, e.item, e.x, e.y));
            }
            playerEntries.sort(Comparator.comparing(p -> p.username));
            return new RoomSummary(roomNumber, playerEntries, playerEntries.size(), MAX_PLAYERS, playerEntries.size() >= MAX_PLAYERS);
        }
    }

    public static class PingRequest {
        public int roomNumber;
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
        public int roomNumber;

        public LobbyResponse(List<PlayerEntry> players, boolean started, int roomNumber) {
            this.players = players;
            this.started = started;
            this.roomNumber = roomNumber;
        }
    }

    public static class RoomSummary {
        public int roomNumber;
        public List<PlayerEntry> players;
        public int playerCount;
        public int maxPlayers;
        public boolean full;

        public RoomSummary(int roomNumber, List<PlayerEntry> players, int playerCount, int maxPlayers, boolean full) {
            this.roomNumber = roomNumber;
            this.players = players;
            this.playerCount = playerCount;
            this.maxPlayers = maxPlayers;
            this.full = full;
        }
    }

    public static class RoomsResponse {
        public List<RoomSummary> rooms;

        public RoomsResponse(List<RoomSummary> rooms) {
            this.rooms = rooms;
        }
    }
}
