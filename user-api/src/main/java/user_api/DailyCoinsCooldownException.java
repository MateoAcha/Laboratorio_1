package user_api;

public class DailyCoinsCooldownException extends RuntimeException {

    private final DailyCoinsResponse response;

    public DailyCoinsCooldownException(DailyCoinsResponse response) {
        super("Daily coin reward is on cooldown.");
        this.response = response;
    }

    public DailyCoinsResponse getResponse() {
        return response;
    }
}
