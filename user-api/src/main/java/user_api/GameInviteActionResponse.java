package user_api;

public class GameInviteActionResponse {

    private String result;
    private GameInviteResponse gameInvite;
    private SocialSummaryResponse summary;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public GameInviteResponse getGameInvite() {
        return gameInvite;
    }

    public void setGameInvite(GameInviteResponse gameInvite) {
        this.gameInvite = gameInvite;
    }

    public SocialSummaryResponse getSummary() {
        return summary;
    }

    public void setSummary(SocialSummaryResponse summary) {
        this.summary = summary;
    }
}
