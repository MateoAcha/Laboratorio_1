package user_api;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private static final String ACCOUNT_LOCAL = "LOCAL";
    private static final String ACCOUNT_GOOGLE = "GOOGLE";
    private static final Pattern GOOGLE_USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    private static final long GOOGLE_SIGNUP_TOKEN_TTL_SECONDS = 600L;
    private static final int XP_PER_SECOND = 1;
    private static final int XP_PER_MELEE_KILL = 5;
    private static final int XP_PER_RANGED_KILL = 8;
    private static final int XP_PER_GIANT_KILL = 60;
    private static final int BASE_XP_FOR_NEXT_LEVEL = 100;
    private static final int EXTRA_XP_PER_LEVEL = 50;

    private final UserRepository repository;
    private final PlayerStatsRepository playerStatsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final InventoryService inventoryService;
    private final SkillTreeService skillTreeService;
    private final SkinService skinService;
    private final DailyCoinsService dailyCoinsService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final Map<String, PendingGoogleSignup> pendingGoogleSignups = new ConcurrentHashMap<>();

    public UserController(
            UserRepository repository,
            PlayerStatsRepository playerStatsRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            InventoryService inventoryService,
            SkillTreeService skillTreeService,
            SkinService skinService,
            DailyCoinsService dailyCoinsService,
            GoogleTokenVerifier googleTokenVerifier) {
        this.repository = repository;
        this.playerStatsRepository = playerStatsRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.inventoryService = inventoryService;
        this.skillTreeService = skillTreeService;
        this.skinService = skinService;
        this.dailyCoinsService = dailyCoinsService;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@RequestBody User user) {
        user.setUsername(requireText("username", user.getUsername()));
        user.setEmail(normalizeEmail(requireText("email", user.getEmail())));
        user.setPassword(passwordEncoder.encode(requireText("password", user.getPassword())));
        user.setAccountType(ACCOUNT_LOCAL);
        user.setGoogleSubject(null);

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
            User savedUser = repository.save(user);
            inventoryService.ensureStarterInventory(savedUser.getUserId());
            skinService.ensureStarterSkins(savedUser.getUserId());
            return withSessionToken(savedUser);
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

        if (!ACCOUNT_LOCAL.equals(user.getAccountType())
                || user.getPassword() == null
                || !passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        return createLoginResponse(user);
    }

    @PostMapping("/login/google")
    public GoogleLoginResponse googleLogin(@RequestBody GoogleLoginRequest request) {
        GoogleLoginRequest safeRequest = request == null ? new GoogleLoginRequest() : request;
        if (safeRequest.hasSignupToken()) {
            return createGoogleUserFromSignupToken(safeRequest.getSignupToken(), safeRequest.getUsername());
        }

        GoogleTokenVerifier.GoogleProfile profile = safeRequest.hasAuthorizationCode()
                ? googleTokenVerifier.verifyAuthorizationCode(
                        safeRequest.getAuthCode(),
                        safeRequest.getCodeVerifier(),
                        safeRequest.getRedirectUri())
                : googleTokenVerifier.verify(safeRequest.getIdToken());

        return repository.findByGoogleSubject(profile.subject())
                .map(user -> GoogleLoginResponse.loggedIn(createLoginResponse(user)))
                .orElseGet(() -> createGoogleUserOrAskForUsername(profile, safeRequest.getUsername()));
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @GetMapping("/me/daily-coins")
    public DailyCoinsResponse getDailyCoinsStatus() {
        return dailyCoinsService.getStatus(authenticatedUsername());
    }

    @PostMapping("/me/daily-coins/claim")
    public DailyCoinsResponse claimDailyCoins() {
        return dailyCoinsService.claim(authenticatedUsername());
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

        int incomingMeleeKills = requireNonNegative("meleeEnemiesKilled", update.getMeleeEnemiesKilled());
        int incomingRangedKills = requireNonNegative("rangedEnemiesKilled", update.getRangedEnemiesKilled());
        int incomingGiantKills = requireNonNegative("giantEnemiesKilled", update.getGiantEnemiesKilled());
        long incomingTime = requireNonNegativeLong("timePlayedSeconds", update.getTimePlayedSeconds());

        stats.setMatchesPlayed(stats.getMatchesPlayed() + requireNonNegative("matchesPlayed", update.getMatchesPlayed()));
        stats.setMeleeEnemiesKilled(stats.getMeleeEnemiesKilled() + incomingMeleeKills);
        stats.setRangedEnemiesKilled(stats.getRangedEnemiesKilled() + incomingRangedKills);
        stats.setGiantEnemiesKilled(stats.getGiantEnemiesKilled() + incomingGiantKills);
        stats.setDeaths(stats.getDeaths() + requireNonNegative("deaths", update.getDeaths()));
        stats.setGamesWon(stats.getGamesWon() + requireNonNegative("gamesWon", update.getGamesWon()));
        stats.setCoins(stats.getCoins() + requireNonNegative("coins", update.getCoins()));

        int incomingHighScore = requireNonNegative("highScore", update.getHighScore());
        if (incomingHighScore > stats.getHighScore()) {
            stats.setHighScore(incomingHighScore);
        }

        stats.setTimePlayedSeconds(stats.getTimePlayedSeconds() + incomingTime);
        applyXp(stats, calculateSessionXp(incomingMeleeKills, incomingRangedKills, incomingGiantKills, incomingTime));

        return playerStatsRepository.save(stats);
    }

    @GetMapping("/{id}/skins")
    public UserSkinsResponse getUserSkins(@PathVariable Integer id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return skinService.getUserSkins(id);
    }

    @PostMapping("/{id}/skins/{skinId}/equip")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void equipSkin(@PathVariable Integer id, @PathVariable Integer skinId) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        skinService.equipSkin(id, skinId);
    }

    @GetMapping("/{id}/inventory")
    public UserInventoryResponse getInventory(@PathVariable Integer id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        return inventoryService.getInventory(id);
    }

    @PostMapping("/{id}/inventory/add-materials")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addMaterials(@PathVariable Integer id, @RequestBody AddMaterialsRequest request) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ensureSameAuthenticatedUser(user);
        inventoryService.addMaterials(id, request != null ? request.getMaterials() : null);
    }

    @PostMapping("/{id}/inventory/spend-material")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void spendMaterial(@PathVariable Integer id, @RequestBody SpendMaterialRequest request) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ensureSameAuthenticatedUser(user);
        try {
            inventoryService.spendMaterial(
                    id,
                    request != null ? request.getMaterialKey() : null,
                    request != null && request.getQuantity() != null ? request.getQuantity() : 0);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/inventory/{userInventoryId}/equip")
    public UserInventoryResponse equipInventoryItem(
            @PathVariable Integer id,
            @PathVariable Integer userInventoryId) {

        User user = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ensureSameAuthenticatedUser(user);
        try {
            return inventoryService.equipInventoryItem(id, userInventoryId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/inventory/{userInventoryId}/consume")
    public ResponseEntity<Void> consumeInventoryItem(
            @PathVariable Integer id,
            @PathVariable Integer userInventoryId,
            @RequestBody(required = false) ConsumeRequest body) {

        User user = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ensureSameAuthenticatedUser(user);
        int quantity = (body != null && body.quantity > 0) ? body.quantity : 1;
        try {
            inventoryService.consumeInventoryItem(id, userInventoryId, quantity);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    static class ConsumeRequest {
        public int quantity;
    }

    @GetMapping("/{id}/skills")
    public SkillTreeService.UserSkillTreeResponse getSkillTree(@PathVariable Integer id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ensureSameAuthenticatedUser(user);
        return skillTreeService.getSkillTree(id);
    }

    @PostMapping("/{id}/skills/{skillId}/unlock")
    public SkillTreeService.SkillTreeActionResponse unlockSkill(
            @PathVariable Integer id,
            @PathVariable String skillId) {

        User user = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ensureSameAuthenticatedUser(user);
        try {
            return skillTreeService.unlockSkill(id, skillId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/skills/{skillId}/equip")
    public SkillTreeService.SkillTreeActionResponse equipSkill(
            @PathVariable Integer id,
            @PathVariable String skillId) {

        User user = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ensureSameAuthenticatedUser(user);
        try {
            return skillTreeService.equipSkill(id, skillId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/skills/{skillId}/level-up")
    public SkillTreeService.SkillTreeActionResponse levelUpSkill(
            @PathVariable Integer id,
            @PathVariable String skillId) {

        User user = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ensureSameAuthenticatedUser(user);
        try {
            return skillTreeService.levelUpSkill(id, skillId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/{id}/challenges/claimed")
    public ChallengeClaimsResponse getClaimedChallenges(@PathVariable Integer id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ensureSameAuthenticatedUser(user);
        return inventoryService.getClaimedChallengeRewards(id);
    }

    @PostMapping("/{id}/challenges/claimed")
    public ChallengeClaimResponse claimChallengeReward(
            @PathVariable Integer id,
            @RequestBody ChallengeClaimRequest request) {

        User user = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ensureSameAuthenticatedUser(user);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }

        return inventoryService.claimChallengeReward(
                id,
                requireNonNegative("challengeId", request.getChallengeId()),
                request.getChallengeKey(),
                requireNonNegative("rewardCoins", request.getRewardCoins()));
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
                    if (ACCOUNT_GOOGLE.equals(user.getAccountType())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google user email cannot be changed");
                    }
                    String newEmail = requireText(field, value);
                    newEmail = normalizeEmail(newEmail);
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
                case "userId", "createdAt", "accountType", "googleSubject" -> throw new ResponseStatusException(
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

    @PostMapping("/{id}/inventory/add-coins")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addCoins(@PathVariable Integer id, @RequestBody AddCoinsRequest request) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        inventoryService.addCoins(id, Math.max(0, request.quantity));
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
        String username = authenticatedUsername();
        if (!username.equals(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own stats");
        }
    }

    private String authenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        return auth.getName();
    }

    private User withSessionToken(User user) {
        user.setAccessToken("dev-token-" + user.getUserId() + "-" + UUID.randomUUID());
        user.setTokenType("Bearer");
        return user;
    }

    private GoogleLoginResponse createGoogleUserOrAskForUsername(
            GoogleTokenVerifier.GoogleProfile profile,
            String requestedUsername) {

        if (repository.findByEmail(profile.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already exists");
        }

        if (requestedUsername == null || requestedUsername.isBlank()) {
            return GoogleLoginResponse.needsUsername(profile.email(), createPendingGoogleSignup(profile));
        }

        String username = requireValidGoogleUsername(requestedUsername);
        if (repository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
        }

        User user = new User();
        user.setUserId(nextUserId());
        user.setUsername(username);
        user.setEmail(profile.email());
        user.setAccountType(ACCOUNT_GOOGLE);
        user.setGoogleSubject(profile.subject());
        user.setPassword(null);
        user.setIsPremium(Boolean.FALSE);

        try {
            User savedUser = repository.save(user);
            inventoryService.ensureStarterInventory(savedUser.getUserId());
            skinService.ensureStarterSkins(savedUser.getUserId());
            return GoogleLoginResponse.loggedIn(createLoginResponse(savedUser));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User data conflicts with existing record",
                    ex);
        }
    }

    private GoogleLoginResponse createGoogleUserFromSignupToken(String signupToken, String requestedUsername) {
        PendingGoogleSignup pendingSignup = pendingGoogleSignups.get(signupToken);
        if (pendingSignup == null || pendingSignup.expiresAt().isBefore(Instant.now())) {
            pendingGoogleSignups.remove(signupToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google signup expired. Please sign in again.");
        }

        GoogleLoginResponse response = createGoogleUserOrAskForUsername(pendingSignup.profile(), requestedUsername);
        if (!response.isRequiresUsername()) {
            pendingGoogleSignups.remove(signupToken);
        }
        return response;
    }

    private String createPendingGoogleSignup(GoogleTokenVerifier.GoogleProfile profile) {
        cleanupExpiredGoogleSignups();
        String token = UUID.randomUUID().toString();
        pendingGoogleSignups.put(
                token,
                new PendingGoogleSignup(profile, Instant.now().plusSeconds(GOOGLE_SIGNUP_TOKEN_TTL_SECONDS)));
        return token;
    }

    private void cleanupExpiredGoogleSignups() {
        Instant now = Instant.now();
        pendingGoogleSignups.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private LoginResponse createLoginResponse(User user) {
        String sessionToken = UUID.randomUUID().toString();
        repository.updateSessionToken(user.getUserId(), sessionToken);
        String token = jwtService.generateToken(user, sessionToken);
        return new LoginResponse(token, user.getUserId(), user.getUsername());
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

    private String requireValidGoogleUsername(String username) {
        String safeUsername = requireText("username", username);
        if (!GOOGLE_USERNAME_PATTERN.matcher(safeUsername).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "username must be 3-20 letters, numbers or underscores");
        }
        return safeUsername;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
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

    private long calculateSessionXp(int meleeKills, int rangedKills, int giantKills, long timePlayedSeconds) {
        return Math.max(0L, timePlayedSeconds) * XP_PER_SECOND
                + (long) Math.max(0, meleeKills) * XP_PER_MELEE_KILL
                + (long) Math.max(0, rangedKills) * XP_PER_RANGED_KILL
                + (long) Math.max(0, giantKills) * XP_PER_GIANT_KILL;
    }

    private void applyXp(PlayerStats stats, long xp) {
        if (stats == null || xp <= 0) {
            return;
        }

        long totalXp = Math.max(0L, stats.getTotalXp()) + xp;
        int oldLevel = Math.max(1, stats.getLevel());
        int newLevel = calculateLevelForTotalXp(totalXp);
        int levelsGained = Math.max(0, newLevel - oldLevel);

        stats.setTotalXp(totalXp);
        stats.setLevel(newLevel);
        stats.setUnspentSkillPoints(stats.getUnspentSkillPoints() + levelsGained);
    }

    private int calculateLevelForTotalXp(long totalXp) {
        long safeTotalXp = Math.max(0L, totalXp);
        int level = 1;
        while (safeTotalXp >= getTotalXpForLevel(level + 1)) {
            level++;
        }
        return level;
    }

    private long getTotalXpForLevel(int level) {
        int safeLevel = Math.max(1, level);
        long completedLevels = safeLevel - 1L;
        return completedLevels * BASE_XP_FOR_NEXT_LEVEL
                + (completedLevels * Math.max(0L, completedLevels - 1L) / 2L) * EXTRA_XP_PER_LEVEL;
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
        private Integer giantEnemiesKilled;
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

        public Integer getGiantEnemiesKilled() {
            return giantEnemiesKilled;
        }

        public void setGiantEnemiesKilled(Integer giantEnemiesKilled) {
            this.giantEnemiesKilled = giantEnemiesKilled;
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

    public static class AddMaterialsRequest {
        private java.util.List<InventoryService.MaterialRewardRequest> materials;

        public java.util.List<InventoryService.MaterialRewardRequest> getMaterials() {
            return materials;
        }

        public void setMaterials(java.util.List<InventoryService.MaterialRewardRequest> materials) {
            this.materials = materials;
        }
    }

    public static class SpendMaterialRequest {
        private String materialKey;
        private Integer quantity;

        public String getMaterialKey() {
            return materialKey;
        }

        public void setMaterialKey(String materialKey) {
            this.materialKey = materialKey;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    public static class ChallengeClaimRequest {
        private Integer challengeId;
        private String challengeKey;
        private Integer rewardCoins;

        public Integer getChallengeId() {
            return challengeId;
        }

        public void setChallengeId(Integer challengeId) {
            this.challengeId = challengeId;
        }

        public String getChallengeKey() {
            return challengeKey;
        }

        public void setChallengeKey(String challengeKey) {
            this.challengeKey = challengeKey;
        }

        public Integer getRewardCoins() {
            return rewardCoins;
        }

        public void setRewardCoins(Integer rewardCoins) {
            this.rewardCoins = rewardCoins;
        }
    }

    public static class GoogleLoginRequest {
        private String idToken;
        private String authCode;
        private String codeVerifier;
        private String redirectUri;
        private String signupToken;
        private String username;

        public String getIdToken() {
            return idToken;
        }

        public void setIdToken(String idToken) {
            this.idToken = idToken;
        }

        public String getAuthCode() {
            return authCode;
        }

        public void setAuthCode(String authCode) {
            this.authCode = authCode;
        }

        public String getCodeVerifier() {
            return codeVerifier;
        }

        public void setCodeVerifier(String codeVerifier) {
            this.codeVerifier = codeVerifier;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public boolean hasAuthorizationCode() {
            return authCode != null && !authCode.isBlank();
        }

        public String getSignupToken() {
            return signupToken;
        }

        public void setSignupToken(String signupToken) {
            this.signupToken = signupToken;
        }

        public boolean hasSignupToken() {
            return signupToken != null && !signupToken.isBlank();
        }
    }

    public static class GoogleLoginResponse extends LoginResponse {
        private boolean requiresUsername;
        private String email;
        private String signupToken;

        public static GoogleLoginResponse needsUsername(String email, String signupToken) {
            GoogleLoginResponse response = new GoogleLoginResponse();
            response.requiresUsername = true;
            response.email = email;
            response.signupToken = signupToken;
            return response;
        }

        public static GoogleLoginResponse loggedIn(LoginResponse loginResponse) {
            GoogleLoginResponse response = new GoogleLoginResponse();
            response.setAccessToken(loginResponse.getAccessToken());
            response.setTokenType(loginResponse.getTokenType());
            response.setUserId(loginResponse.getUserId());
            response.setUsername(loginResponse.getUsername());
            response.requiresUsername = false;
            return response;
        }

        public boolean isRequiresUsername() {
            return requiresUsername;
        }

        public void setRequiresUsername(boolean requiresUsername) {
            this.requiresUsername = requiresUsername;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getSignupToken() {
            return signupToken;
        }

        public void setSignupToken(String signupToken) {
            this.signupToken = signupToken;
        }
    }

    private record PendingGoogleSignup(GoogleTokenVerifier.GoogleProfile profile, Instant expiresAt) {}

    static class AddCoinsRequest {
        public int quantity;
    }
}
