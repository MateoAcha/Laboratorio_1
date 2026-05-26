package user_api;

public class ChallengeClaimResponse {

    private Integer challengeId;
    private String challengeKey;
    private Integer rewardCoins;
    private Boolean alreadyClaimed;
    private String claimedAt;

    public ChallengeClaimResponse() {
    }

    public ChallengeClaimResponse(Integer challengeId, String challengeKey, Integer rewardCoins, Boolean alreadyClaimed) {
        this.challengeId = challengeId;
        this.challengeKey = challengeKey;
        this.rewardCoins = rewardCoins;
        this.alreadyClaimed = alreadyClaimed;
    }

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

    public Boolean getAlreadyClaimed() {
        return alreadyClaimed;
    }

    public void setAlreadyClaimed(Boolean alreadyClaimed) {
        this.alreadyClaimed = alreadyClaimed;
    }

    public String getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(String claimedAt) {
        this.claimedAt = claimedAt;
    }
}
