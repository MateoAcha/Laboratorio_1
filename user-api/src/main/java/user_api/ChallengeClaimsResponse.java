package user_api;

import java.util.ArrayList;
import java.util.List;

public class ChallengeClaimsResponse {

    private List<Integer> claimedChallengeIds = new ArrayList<>();
    private List<ChallengeClaimResponse> claims = new ArrayList<>();

    public List<Integer> getClaimedChallengeIds() {
        return claimedChallengeIds;
    }

    public void setClaimedChallengeIds(List<Integer> claimedChallengeIds) {
        this.claimedChallengeIds = claimedChallengeIds;
    }

    public List<ChallengeClaimResponse> getClaims() {
        return claims;
    }

    public void setClaims(List<ChallengeClaimResponse> claims) {
        this.claims = claims;
    }
}
