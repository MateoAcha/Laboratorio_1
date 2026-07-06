package user_api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LobbyRoomService {

    private static final long ACTIVE_MS = 15_000;
    private static final long STARTED_TTL_MS = 30_000;
    private static final int MAX_PLAYERS = 2;

    private final ConcurrentHashMap<Integer, RoomEntry> rooms = new ConcurrentHashMap<>();
    private final AtomicInteger nextRoomNumber = new AtomicInteger(1);

    public RoomsResponse rooms() {
        pruneInactiveRooms();

        List<RoomSummary> summaries = new ArrayList<>();
        for (RoomEntry room : rooms.values()) {
            summaries.add(room.toSummary());
        }
        summaries.sort(Comparator.comparingInt(RoomSummary::getRoomNumber));
        return new RoomsResponse(summaries);
    }

    public RoomSummary create(String username) {
        return create(username, null);
    }

    public RoomSummary create(String username, CreateRequest request) {
        pruneInactiveRooms();

        int roomNumber;
        do {
            roomNumber = nextRoomNumber.getAndIncrement();
        } while (rooms.containsKey(roomNumber));

        boolean privateMatch = request != null && request.isPrivateMatch();
        String password = privateMatch ? normalizePassword(request.getPassword()) : "";
        if (privateMatch && password.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Private matches need a password.");
        }

        RoomEntry room = new RoomEntry(roomNumber, username, privateMatch, password);
        room.players.put(username, new LobbyEntry(username, "", "", "", 0f, 0f, System.currentTimeMillis()));
        rooms.put(roomNumber, room);
        return room.toSummary();
    }

    public LobbyResponse ping(String username, PingRequest req) {
        PingRequest safeRequest = req != null ? req : new PingRequest();
        int roomNumber = safeRequest.getRoomNumber() > 0 ? safeRequest.getRoomNumber() : 1;
        RoomEntry room = rooms.get(roomNumber);
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby closed.");
        }
        pruneInactiveRoom(room);

        if (room.players.isEmpty()) {
            rooms.remove(roomNumber);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby closed.");
        }

        if (!room.players.containsKey(username)) {
            if (room.players.size() >= MAX_PLAYERS) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby is full.");
            }
            ensureCanJoinRoom(username, room, safeRequest);
        }

        room.players.put(username, new LobbyEntry(
                username,
                safeRequest.getWeapon() != null ? safeRequest.getWeapon() : "",
                safeRequest.getArmor() != null ? safeRequest.getArmor() : "",
                safeRequest.getItem() != null ? safeRequest.getItem() : "",
                safeRequest.getX(),
                safeRequest.getY(),
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
            players.add(new PlayerEntry(e.username(), e.weapon(), e.armor(), e.item(), e.x(), e.y()));
        }
        players.sort(Comparator.comparing(PlayerEntry::getUsername));
        return new LobbyResponse(players, started, roomNumber, room.privateMatch);
    }

    public void start(String username, int roomNumber) {
        RoomEntry room = rooms.get(roomNumber);
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found.");
        }
        if (!username.equals(room.hostUsername)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can start this lobby.");
        }
        pruneInactiveRoom(room);
        if (room.players.size() < MAX_PLAYERS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby needs two players to start.");
        }
        room.startedAt = System.currentTimeMillis();
    }

    public void leave(String username, int roomNumber) {
        RoomEntry room = rooms.get(roomNumber);
        if (room == null) {
            return;
        }

        if (username.equals(room.hostUsername)) {
            rooms.remove(roomNumber);
            return;
        }

        room.players.remove(username);
        if (room.players.isEmpty()) {
            rooms.remove(roomNumber);
        }
    }

    public void closeRoom(int roomNumber) {
        rooms.remove(roomNumber);
    }

    public boolean hasActiveRoom(int roomNumber) {
        return activeRoom(roomNumber).isPresent();
    }

    public boolean isActiveHost(String username, int roomNumber) {
        return activeRoom(roomNumber)
                .map(room -> username.equals(room.hostUsername) && room.players.containsKey(username))
                .orElse(false);
    }

    public Optional<RoomSummary> findActiveRoomSummary(int roomNumber) {
        return activeRoom(roomNumber).map(RoomEntry::toSummary);
    }

    public void authorizeInviteJoin(String username, int roomNumber) {
        RoomEntry room = rooms.get(roomNumber);
        if (room == null || username == null || username.isBlank()) {
            return;
        }

        room.inviteAuthorizedUsers.add(username);
    }

    private void ensureCanJoinRoom(String username, RoomEntry room, PingRequest request) {
        if (!room.privateMatch || username.equals(room.hostUsername) || room.inviteAuthorizedUsers.contains(username)) {
            return;
        }

        String submittedPassword = normalizePassword(request.getPassword());
        if (submittedPassword.isEmpty()) {
            submittedPassword = normalizePassword(request.getLobbyPassword());
        }

        if (!room.password.equals(submittedPassword)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Incorrect lobby password.");
        }
    }

    private static String normalizePassword(String value) {
        return value != null ? value.trim() : "";
    }

    private Optional<RoomEntry> activeRoom(int roomNumber) {
        RoomEntry room = rooms.get(roomNumber);
        if (room == null) {
            return Optional.empty();
        }
        pruneInactiveRoom(room);
        if (room.players.isEmpty()) {
            rooms.remove(roomNumber);
            return Optional.empty();
        }
        return Optional.of(room);
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
        room.players.entrySet().removeIf(e -> e.getValue().lastPingAt() < cutoff);
        if (!room.players.containsKey(room.hostUsername)) {
            room.players.clear();
        }
        if (room.players.isEmpty()) {
            room.startedAt = 0;
        }
    }

    private record LobbyEntry(
            String username,
            String weapon,
            String armor,
            String item,
            float x,
            float y,
            long lastPingAt) {
    }

    private static class RoomEntry {
        final int roomNumber;
        final String hostUsername;
        final boolean privateMatch;
        final String password;
        final Set<String> inviteAuthorizedUsers = ConcurrentHashMap.newKeySet();
        final ConcurrentHashMap<String, LobbyEntry> players = new ConcurrentHashMap<>();
        volatile long startedAt = 0;

        RoomEntry(int roomNumber, String hostUsername, boolean privateMatch, String password) {
            this.roomNumber = roomNumber;
            this.hostUsername = hostUsername;
            this.privateMatch = privateMatch;
            this.password = password;
        }

        RoomSummary toSummary() {
            List<PlayerEntry> playerEntries = new ArrayList<>();
            for (LobbyEntry e : players.values()) {
                playerEntries.add(new PlayerEntry(e.username(), e.weapon(), e.armor(), e.item(), e.x(), e.y()));
            }
            playerEntries.sort(Comparator.comparing(PlayerEntry::getUsername));
            return new RoomSummary(
                    roomNumber,
                    playerEntries,
                    playerEntries.size(),
                    MAX_PLAYERS,
                    playerEntries.size() >= MAX_PLAYERS,
                    privateMatch);
        }
    }

    public static class CreateRequest {
        private boolean privateMatch;
        private String password;

        public boolean isPrivateMatch() {
            return privateMatch;
        }

        public void setPrivateMatch(boolean privateMatch) {
            this.privateMatch = privateMatch;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class PingRequest {
        private int roomNumber;
        private String weapon;
        private String armor;
        private String item;
        private String password;
        private String lobbyPassword;
        private boolean inviteJoin;
        private float x;
        private float y;

        public int getRoomNumber() {
            return roomNumber;
        }

        public void setRoomNumber(int roomNumber) {
            this.roomNumber = roomNumber;
        }

        public String getWeapon() {
            return weapon;
        }

        public void setWeapon(String weapon) {
            this.weapon = weapon;
        }

        public String getArmor() {
            return armor;
        }

        public void setArmor(String armor) {
            this.armor = armor;
        }

        public String getItem() {
            return item;
        }

        public void setItem(String item) {
            this.item = item;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getLobbyPassword() {
            return lobbyPassword;
        }

        public void setLobbyPassword(String lobbyPassword) {
            this.lobbyPassword = lobbyPassword;
        }

        public boolean isInviteJoin() {
            return inviteJoin;
        }

        public void setInviteJoin(boolean inviteJoin) {
            this.inviteJoin = inviteJoin;
        }

        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
        }
    }

    public static class PlayerEntry {
        private String username;
        private String weapon;
        private String armor;
        private String item;
        private float x;
        private float y;

        public PlayerEntry(String username, String weapon, String armor, String item, float x, float y) {
            this.username = username;
            this.weapon = weapon;
            this.armor = armor;
            this.item = item;
            this.x = x;
            this.y = y;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getWeapon() {
            return weapon;
        }

        public void setWeapon(String weapon) {
            this.weapon = weapon;
        }

        public String getArmor() {
            return armor;
        }

        public void setArmor(String armor) {
            this.armor = armor;
        }

        public String getItem() {
            return item;
        }

        public void setItem(String item) {
            this.item = item;
        }

        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
        }
    }

    public static class LobbyResponse {
        private List<PlayerEntry> players;
        private boolean started;
        private int roomNumber;
        private boolean privateMatch;

        public LobbyResponse(List<PlayerEntry> players, boolean started, int roomNumber) {
            this(players, started, roomNumber, false);
        }

        public LobbyResponse(List<PlayerEntry> players, boolean started, int roomNumber, boolean privateMatch) {
            this.players = players;
            this.started = started;
            this.roomNumber = roomNumber;
            this.privateMatch = privateMatch;
        }

        public List<PlayerEntry> getPlayers() {
            return players;
        }

        public void setPlayers(List<PlayerEntry> players) {
            this.players = players;
        }

        public boolean isStarted() {
            return started;
        }

        public void setStarted(boolean started) {
            this.started = started;
        }

        public int getRoomNumber() {
            return roomNumber;
        }

        public void setRoomNumber(int roomNumber) {
            this.roomNumber = roomNumber;
        }

        public boolean isPrivateMatch() {
            return privateMatch;
        }

        public void setPrivateMatch(boolean privateMatch) {
            this.privateMatch = privateMatch;
        }
    }

    public static class RoomSummary {
        private int roomNumber;
        private List<PlayerEntry> players;
        private int playerCount;
        private int maxPlayers;
        private boolean full;
        private boolean privateMatch;

        public RoomSummary(int roomNumber, List<PlayerEntry> players, int playerCount, int maxPlayers, boolean full, boolean privateMatch) {
            this.roomNumber = roomNumber;
            this.players = players;
            this.playerCount = playerCount;
            this.maxPlayers = maxPlayers;
            this.full = full;
            this.privateMatch = privateMatch;
        }

        public int getRoomNumber() {
            return roomNumber;
        }

        public void setRoomNumber(int roomNumber) {
            this.roomNumber = roomNumber;
        }

        public List<PlayerEntry> getPlayers() {
            return players;
        }

        public void setPlayers(List<PlayerEntry> players) {
            this.players = players;
        }

        public int getPlayerCount() {
            return playerCount;
        }

        public void setPlayerCount(int playerCount) {
            this.playerCount = playerCount;
        }

        public int getMaxPlayers() {
            return maxPlayers;
        }

        public void setMaxPlayers(int maxPlayers) {
            this.maxPlayers = maxPlayers;
        }

        public boolean isFull() {
            return full;
        }

        public void setFull(boolean full) {
            this.full = full;
        }

        public boolean isPrivateMatch() {
            return privateMatch;
        }

        public void setPrivateMatch(boolean privateMatch) {
            this.privateMatch = privateMatch;
        }
    }

    public static class RoomsResponse {
        private List<RoomSummary> rooms;

        public RoomsResponse(List<RoomSummary> rooms) {
            this.rooms = rooms;
        }

        public List<RoomSummary> getRooms() {
            return rooms;
        }

        public void setRooms(List<RoomSummary> rooms) {
            this.rooms = rooms;
        }
    }
}
