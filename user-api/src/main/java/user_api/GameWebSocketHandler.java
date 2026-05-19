package user_api;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Relay between host and guest. The first message each side sends is a
 * registration: {"type":"register","role":"host"} or "guest".
 * After that, every message from the host is forwarded to the guest and
 * every message from the guest is forwarded to the host.
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<Integer, RoomSessions> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> sessionRooms = new ConcurrentHashMap<>();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload();

        // Registration messages
        if (payload.contains("\"register\"")) {
            int roomNumber = roomNumber(session);
            RoomSessions room = rooms.computeIfAbsent(roomNumber, ignored -> new RoomSessions());
            sessionRooms.put(session.getId(), roomNumber);

            if (payload.contains("\"host\"")) {
                room.hostSession = session;
            } else {
                room.guestSession = session;
            }
            return;
        }

        RoomSessions room = rooms.get(roomNumber(session));
        if (room == null) return;

        // Relay: host state → guest
        if (session == room.hostSession) {
            WebSocketSession guest = room.guestSession;
            if (guest != null && guest.isOpen()) {
                synchronized (guest) {
                    guest.sendMessage(message);
                }
            }
            return;
        }

        // Relay: guest pos/kill → host
        if (session == room.guestSession) {
            WebSocketSession host = room.hostSession;
            if (host != null && host.isOpen()) {
                synchronized (host) {
                    host.sendMessage(message);
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Integer roomNumber = sessionRooms.remove(session.getId());
        if (roomNumber == null) return;

        RoomSessions room = rooms.get(roomNumber);
        if (room == null) return;

        if (session == room.hostSession)  room.hostSession  = null;
        if (session == room.guestSession) room.guestSession = null;
        if (room.hostSession == null && room.guestSession == null) rooms.remove(roomNumber);
    }

    @Override
    public boolean supportsPartialMessages() { return false; }

    private int roomNumber(WebSocketSession session) {
        Integer existing = sessionRooms.get(session.getId());
        if (existing != null) return existing;

        URI uri = session.getUri();
        String query = uri != null ? uri.getQuery() : null;
        if (query == null) return 1;

        for (String part : query.split("&")) {
            String[] pieces = part.split("=", 2);
            if (pieces.length == 2 && pieces[0].equals("room")) {
                try {
                    return Math.max(1, Integer.parseInt(pieces[1]));
                } catch (NumberFormatException ignored) {
                    return 1;
                }
            }
        }
        return 1;
    }

    private static class RoomSessions {
        volatile WebSocketSession hostSession;
        volatile WebSocketSession guestSession;
    }
}
