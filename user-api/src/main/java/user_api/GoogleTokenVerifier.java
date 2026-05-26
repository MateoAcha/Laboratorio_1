package user_api;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GoogleTokenVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleTokenVerifier.class);

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String clientId;
    private final String clientSecret;

    public GoogleTokenVerifier(
            @Value("${google.client-id:}") String clientId,
            @Value("${google.client-secret:}") String clientSecret) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
    }

    public GoogleProfile verifyAuthorizationCode(String authCode, String codeVerifier, String redirectUri) {
        String code = requireText("Google authorization code", authCode);
        String verifier = requireText("Google code verifier", codeVerifier);
        String callback = requireText("Google redirect URI", redirectUri);

        if (clientId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google client id is not configured");
        }

        String form = "client_id=" + encode(clientId)
                + "&code=" + encode(code)
                + "&code_verifier=" + encode(verifier)
                + "&grant_type=authorization_code"
                + "&redirect_uri=" + encode(callback);
        if (!clientSecret.isBlank()) {
            form += "&client_secret=" + encode(clientSecret);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not complete Google login", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google login was interrupted", ex);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            LOGGER.warn("Google token exchange rejected with status {}: {}", response.statusCode(), response.body());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google login was rejected");
        }

        String idToken = text(response.body() == null ? "" : response.body(), "id_token");
        if (idToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google did not return an identity token");
        }

        return verify(idToken);
    }

    public GoogleProfile verify(String idToken) {
        String token = requireToken(idToken);
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not verify Google account", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google verification interrupted", ex);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google login");
        }

        String body = response.body() == null ? "" : response.body();
        String subject = text(body, "sub");
        String email = text(body, "email");
        String audience = text(body, "aud");
        String emailVerified = text(body, "email_verified");

        if (subject.isBlank() || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google account is missing required profile data");
        }

        if (!clientId.isBlank() && !clientId.equals(audience)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google login was issued for a different client");
        }

        if (!"true".equalsIgnoreCase(emailVerified)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google email must be verified");
        }

        return new GoogleProfile(subject, email.trim().toLowerCase());
    }

    private String requireToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google login token is required");
        }
        return idToken.trim();
    }

    private String requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return value.trim();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String text(String json, String field) {
        String search = "\"" + field + "\"";
        int index = json.indexOf(search);
        if (index < 0) {
            return "";
        }

        index += search.length();
        while (index < json.length() && (json.charAt(index) == ' ' || json.charAt(index) == ':')) {
            index++;
        }

        if (index >= json.length() || json.charAt(index) != '"') {
            return "";
        }

        index++;
        StringBuilder builder = new StringBuilder();
        while (index < json.length() && json.charAt(index) != '"') {
            char value = json.charAt(index);
            if (value == '\\' && index + 1 < json.length()) {
                index++;
                value = json.charAt(index);
            }
            builder.append(value);
            index++;
        }

        return builder.toString();
    }

    public record GoogleProfile(String subject, String email) {}
}
