package user_api;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

/**
 * Relay between host and guest. The first message each side sends is a
 * registration: {"type":"register","role":"host"} or "guest".
 * After that, every message from the host is forwarded to the guest and
 * every message from the guest is forwarded to the host.
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private volatile WebSocketSession hostSession  = null;
    private volatile WebSocketSession guestSession = null;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload();

        // Registration messages
        if (payload.contains("\"register\"")) {
            if (payload.contains("\"host\"")) {
                hostSession = session;
            } else {
                guestSession = session;
            }
            return;
        }

        // Relay: host state → guest
        if (session == hostSession) {
            WebSocketSession guest = guestSession;
            if (guest != null && guest.isOpen()) {
                synchronized (guest) {
                    guest.sendMessage(message);
                }
            }
            return;
        }

        // Relay: guest pos/kill → host
        if (session == guestSession) {
            WebSocketSession host = hostSession;
            if (host != null && host.isOpen()) {
                synchronized (host) {
                    host.sendMessage(message);
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        if (session == hostSession)  hostSession  = null;
        if (session == guestSession) guestSession = null;
    }

    @Override
    public boolean supportsPartialMessages() { return false; }
}
