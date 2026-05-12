package user_api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@RestController
@RequestMapping("/game")
public class GameStateController {

    private static final long STATE_TTL_MS = 5_000;

    private volatile GameStateBody latestState = null;
    private volatile long stateWrittenAt = 0;
    private final ConcurrentLinkedQueue<Integer> pendingKills = new ConcurrentLinkedQueue<>();

    @PostMapping("/state")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void postState(@RequestBody GameStateBody state) {
        latestState = state;
        stateWrittenAt = System.currentTimeMillis();
    }

    @GetMapping("/state")
    public GameStateBody getState() {
        if (latestState == null || System.currentTimeMillis() - stateWrittenAt > STATE_TTL_MS)
            return new GameStateBody();
        return latestState;
    }

    @PostMapping("/kill/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reportKill(@PathVariable int id) {
        pendingKills.add(id);
    }

    @GetMapping("/kills")
    public KillResponse getPendingKills() {
        List<Integer> result = new ArrayList<>();
        Integer id;
        while ((id = pendingKills.poll()) != null) result.add(id);
        return new KillResponse(result.stream().mapToInt(Integer::intValue).toArray());
    }

    public static class KillResponse {
        public int[] ids;
        public KillResponse(int[] ids) { this.ids = ids; }
    }

    public static class GameStateBody {
        public List<EnemyData>      enemies     = new ArrayList<>();
        public List<ProjectileData> projectiles = new ArrayList<>();
        public List<RockData>       rocks       = new ArrayList<>();
    }

    public static class EnemyData {
        public int   id;
        public int   type;   // 0 = melee, 1 = ranged
        public float x, y, hp, maxHp, size;
    }

    public static class ProjectileData {
        public int   id;
        public float x, y, vx, vy, life;
    }

    public static class RockData {
        public float x, y, size;
    }
}
