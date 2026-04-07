package user_api;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository repository;

    public UserController(UserRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@RequestBody User user) {
        user.setUsername(requireText("username", user.getUsername()));
        user.setEmail(requireText("email", user.getEmail()));
        user.setPassword(requireText("password", user.getPassword()));

        if (user.getUserId() == null) {
            user.setUserId(nextUserId());
        } else if (repository.existsById(user.getUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "userId already exists");
        }

        if (repository.existsByUsername(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
        }

        if (repository.existsByEmail(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already exists");
        }

        try {
            return repository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User data conflicts with existing record",
                    ex);
        }
    }

    @PostMapping("/login")
    public User login(@RequestBody LoginRequest request) {
        String username = requireText("username", request.getUsername());
        String password = requireText("password", request.getPassword());

        User user = repository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!password.equals(user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        return user;
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @PatchMapping("/{id}")
    public User patchUser(@PathVariable Integer id, @RequestBody Map<String, Object> updates) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            switch (field) {
                case "username" -> {
                    String newUsername = requireText(field, value);
                    if (!newUsername.equals(user.getUsername()) && repository.existsByUsername(newUsername)) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
                    }
                    user.setUsername(newUsername);
                }
                case "email" -> {
                    String newEmail = requireText(field, value);
                    if (!newEmail.equals(user.getEmail()) && repository.existsByEmail(newEmail)) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "email already exists");
                    }
                    user.setEmail(newEmail);
                }
                case "isPremium" -> user.setIsPremium(requireBoolean(field, value));
                case "premiumSince" -> user.setPremiumSince(parseDateTimeOrNull(field, value));
                case "premiumUntil" -> user.setPremiumUntil(parseDateTimeOrNull(field, value));
                case "password" -> throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Use dedicated password endpoint");
                case "userId", "createdAt" -> throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        field + " cannot be updated");
                default -> throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unknown field: " + field);
            }
        }

        if (Boolean.FALSE.equals(user.getIsPremium())) {
            user.setPremiumSince(null);
            user.setPremiumUntil(null);
        } else if (Boolean.TRUE.equals(user.getIsPremium()) && user.getPremiumSince() == null) {
            user.setPremiumSince(LocalDateTime.now());
        }

        try {
            return repository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User data conflicts with existing record",
                    ex);
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Integer id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        repository.deleteById(id);
    }

    private Integer nextUserId() {
        return repository.findTopByOrderByUserIdDesc()
                .map(user -> user.getUserId() + 1)
                .orElse(1);
    }

    private String requireText(String field, Object value) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be a non-empty string");
        }
        return text.trim();
    }

    private boolean requireBoolean(String field, Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be true or false");
    }

    private LocalDateTime parseDateTimeOrNull(String field, Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be ISO datetime text");
        }
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be ISO datetime text", ex);
        }
    }
}
