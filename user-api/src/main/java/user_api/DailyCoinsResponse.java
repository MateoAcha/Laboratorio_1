package user_api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DailyCoinsResponse {

    private Boolean claimable;
    private Long remainingSeconds;
    private Integer rewardCoins;
    private LocalDateTime lastClaimedAt;
    private Integer goldCoinTotal;
    private String message;

    public Boolean getClaimable() {
        return claimable;
    }

    public void setClaimable(Boolean claimable) {
        this.claimable = claimable;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public Integer getRewardCoins() {
        return rewardCoins;
    }

    public void setRewardCoins(Integer rewardCoins) {
        this.rewardCoins = rewardCoins;
    }

    public LocalDateTime getLastClaimedAt() {
        return lastClaimedAt;
    }

    public void setLastClaimedAt(LocalDateTime lastClaimedAt) {
        this.lastClaimedAt = lastClaimedAt;
    }

    public Integer getGoldCoinTotal() {
        return goldCoinTotal;
    }

    public void setGoldCoinTotal(Integer goldCoinTotal) {
        this.goldCoinTotal = goldCoinTotal;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
