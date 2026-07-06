package user_api;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final InventoryService inventoryService;
    private final SkinService skinService;

    public UserProfileService(
            UserRepository userRepository,
            PlayerStatsRepository playerStatsRepository,
            InventoryService inventoryService,
            SkinService skinService) {
        this.userRepository = userRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.inventoryService = inventoryService;
        this.skinService = skinService;
    }

    public UserProfileSummaryResponse getByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username must be a non-empty string");
        }
        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return buildProfile(user);
    }

    public UserProfileSummaryResponse getByUserId(Integer userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return buildProfile(user);
    }

    private UserProfileSummaryResponse buildProfile(User user) {
        PlayerStats stats = playerStatsRepository.findById(user.getUserId()).orElse(null);

        UserProfileSummaryResponse response = new UserProfileSummaryResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setStats(toStats(stats));
        response.setLevel(response.getStats().getLevel());
        response.setLoadout(toLoadout(user.getUserId()));
        return response;
    }

    private UserProfileSummaryResponse.ProfileStatsResponse toStats(PlayerStats stats) {
        UserProfileSummaryResponse.ProfileStatsResponse response = new UserProfileSummaryResponse.ProfileStatsResponse();
        int matchesPlayed = stats != null ? nullToZero(stats.getMatchesPlayed()) : 0;
        int meleeKills = stats != null ? nullToZero(stats.getMeleeEnemiesKilled()) : 0;
        int rangedKills = stats != null ? nullToZero(stats.getRangedEnemiesKilled()) : 0;
        int giantKills = stats != null ? nullToZero(stats.getGiantEnemiesKilled()) : 0;
        int gamesWon = stats != null ? nullToZero(stats.getGamesWon()) : 0;

        response.setGamesPlayed(matchesPlayed);
        response.setWins(gamesWon);
        response.setKills(meleeKills + rangedKills + giantKills);
        response.setBestTimeSeconds(0L);
        response.setMatchesPlayed(matchesPlayed);
        response.setMeleeEnemiesKilled(meleeKills);
        response.setRangedEnemiesKilled(rangedKills);
        response.setGiantEnemiesKilled(giantKills);
        response.setDeaths(stats != null ? nullToZero(stats.getDeaths()) : 0);
        response.setGamesWon(gamesWon);
        response.setHighScore(stats != null ? nullToZero(stats.getHighScore()) : 0);
        response.setTimePlayedSeconds(stats != null ? nullToZero(stats.getTimePlayedSeconds()) : 0L);
        response.setCoins(stats != null ? nullToZero(stats.getCoins()) : 0);
        response.setEmeralds(inventoryService.getEmeralds(user.getUserId()));
        response.setTotalXp(stats != null ? nullToZero(stats.getTotalXp()) : 0L);
        response.setLevel(stats != null ? Math.max(1, nullToZero(stats.getLevel())) : 1);
        response.setUnspentSkillPoints(stats != null ? nullToZero(stats.getUnspentSkillPoints()) : 0);
        response.setSpentSkillPoints(stats != null ? nullToZero(stats.getSpentSkillPoints()) : 0);
        return response;
    }

    private UserProfileSummaryResponse.ProfileLoadoutResponse toLoadout(Integer userId) {
        UserProfileSummaryResponse.ProfileLoadoutResponse response =
                new UserProfileSummaryResponse.ProfileLoadoutResponse();
        response.setSkinId(0);
        response.setSkinColor("#FFFFFF");
        response.setWeaponItemId(0);
        response.setWeaponType("Spear");
        response.setWeaponColor("#FFFFFF");
        response.setArmorName("");
        response.setConsumableName("");

        UserSkinsResponse skins = skinService.getUserSkins(userId);
        if (skins.getSkins() != null) {
            for (SkinResponse skin : skins.getSkins()) {
                if (skin.isEquipped()) {
                    response.setSkinId(skin.getSkinId());
                    response.setSkinColor(coalesceText(skin.getSkinColor(), "#FFFFFF"));
                    break;
                }
            }
        }

        UserInventoryResponse inventory = inventoryService.getInventory(userId);
        if (inventory.getItems() != null) {
            for (InventoryItemResponse item : inventory.getItems()) {
                if (!Boolean.TRUE.equals(item.getEquipped()) || item.getQuantity() == null || item.getQuantity() <= 0) {
                    continue;
                }
                String itemType = item.getItemType();
                if ("Weapon".equalsIgnoreCase(itemType)) {
                    response.setWeaponItemId(nullToZero(item.getItemId()));
                    response.setWeaponType(coalesceText(item.getWeaponType(), "Spear"));
                    response.setWeaponColor(coalesceText(item.getWeaponColor(), "#FFFFFF"));
                } else if ("Armor".equalsIgnoreCase(itemType)) {
                    response.setArmorName(coalesceText(item.getItemName(), ""));
                } else if ("Consumable".equalsIgnoreCase(itemType)) {
                    response.setConsumableName(coalesceText(item.getItemName(), ""));
                }
            }
        }

        return response;
    }

    private int nullToZero(Integer value) {
        return value != null ? value : 0;
    }

    private long nullToZero(Long value) {
        return value != null ? value : 0L;
    }

    private String coalesceText(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
