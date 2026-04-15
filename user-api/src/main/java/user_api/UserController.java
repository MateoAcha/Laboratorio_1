package user_api;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PlayerStatsRepository playerStatsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserController(
            UserRepository repository,
            PlayerStatsRepository playerStatsRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.repository = repository;
        this.playerStatsRepository = playerStatsRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@RequestBody User user) {
        user.setUsername(requireText("username", user.getUsername()));
        user.setEmail(requireText("email", user.getEmail()));
        user.setPassword(passwordEncoder.encode(requireText("password", user.getPassword())));

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
    public LoginResponse login(@RequestBody LoginRequest request) {
        String username = requireText("username", request.getUsername());
        String password = requireText("password", request.getPassword());

        User user = repository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        String token = jwtService.generateToken(user);
        return new LoginResponse(token, user.getUserId(), user.getUsername());
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @GetMapping("/{id}/stats")
    public PlayerStats getPlayerStats(@PathVariable Integer id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ensureSameAuthenticatedUser(user);
        return findOrCreateStats(user);
    }

    @PostMapping("/{id}/stats/session")
    public PlayerStats addSessionStats(
            @PathVariable Integer id,
            @RequestBody PlayerStatsSessionUpdateRequest update) {

        User user = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ensureSameAuthenticatedUser(user);

        PlayerStats stats = findOrCreateStats(user);

        stats.setMatchesPlayed(stats.getMatchesPlayed() + requireNonNegative("matchesPlayed", update.getMatchesPlayed()));
        stats.setMeleeEnemiesKilled(stats.getMeleeEnemiesKilled() + requireNonNegative("meleeEnemiesKilled", update.getMeleeEnemiesKilled()));
        stats.setRangedEnemiesKilled(stats.getRangedEnemiesKilled() + requireNonNegative("rangedEnemiesKilled", update.getRangedEnemiesKilled()));
        stats.setDeaths(stats.getDeaths() + requireNonNegative("deaths", update.getDeaths()));
        stats.setGamesWon(stats.getGamesWon() + requireNonNegative("gamesWon", update.getGamesWon()));
        stats.setCoins(stats.getCoins() + requireNonNegative("coins", update.getCoins()));

        int incomingHighScore = requireNonNegative("highScore", update.getHighScore());
        if (incomingHighScore > stats.getHighScore()) {
            stats.setHighScore(incomingHighScore);
        }

        long incomingTime = requireNonNegativeLong("timePlayedSeconds", update.getTimePlayedSeconds());
        stats.setTimePlayedSeconds(stats.getTimePlayedSeconds() + incomingTime);

        return playerStatsRepository.save(stats);
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

    private PlayerStats findOrCreateStats(User user) {
        return playerStatsRepository.findById(user.getUserId())
                .orElseGet(() -> {
                    PlayerStats stats = new PlayerStats();
                    stats.setUser(user);
                    return playerStatsRepository.save(stats);
                });
    }

    private void ensureSameAuthenticatedUser(User user) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        if (!auth.getName().equals(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own stats");
        }
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

    private int requireNonNegative(String field, Integer value) {
        int safeValue = value == null ? 0 : value;
        if (safeValue < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be >= 0");
        }
        return safeValue;
    }

    private long requireNonNegativeLong(String field, Long value) {
        long safeValue = value == null ? 0L : value;
        if (safeValue < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be >= 0");
        }
        return safeValue;
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

    public static class PlayerStatsSessionUpdateRequest {
        private Integer matchesPlayed;
        private Integer meleeEnemiesKilled;
        private Integer rangedEnemiesKilled;
        private Integer deaths;
        private Integer gamesWon;
        private Integer highScore;
        private Long timePlayedSeconds;
        private Integer coins;

        public Integer getMatchesPlayed() {
            return matchesPlayed;
        }

        public void setMatchesPlayed(Integer matchesPlayed) {
            this.matchesPlayed = matchesPlayed;
        }

        public Integer getMeleeEnemiesKilled() {
            return meleeEnemiesKilled;
        }

        public void setMeleeEnemiesKilled(Integer meleeEnemiesKilled) {
            this.meleeEnemiesKilled = meleeEnemiesKilled;
        }

        public Integer getRangedEnemiesKilled() {
            return rangedEnemiesKilled;
        }

        public void setRangedEnemiesKilled(Integer rangedEnemiesKilled) {
            this.rangedEnemiesKilled = rangedEnemiesKilled;
        }

        public Integer getDeaths() {
            return deaths;
        }

        public void setDeaths(Integer deaths) {
            this.deaths = deaths;
        }

        public Integer getGamesWon() {
            return gamesWon;
        }

        public void setGamesWon(Integer gamesWon) {
            this.gamesWon = gamesWon;
        }

        public Integer getHighScore() {
            return highScore;
        }

        public void setHighScore(Integer highScore) {
            this.highScore = highScore;
        }

        public Long getTimePlayedSeconds() {
            return timePlayedSeconds;
        }

        public void setTimePlayedSeconds(Long timePlayedSeconds) {
            this.timePlayedSeconds = timePlayedSeconds;
        }

        public Integer getCoins() {
            return coins;
        }

        public void setCoins(Integer coins) {
            this.coins = coins;
        }
    }
}
