package user_api;

public class FriendRequestActionResponse {

    private String result;
    private FriendRequestResponse friendRequest;
    private FriendSummaryResponse friendship;
    private SocialSummaryResponse summary;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public FriendRequestResponse getFriendRequest() {
        return friendRequest;
    }

    public void setFriendRequest(FriendRequestResponse friendRequest) {
        this.friendRequest = friendRequest;
    }

    public FriendSummaryResponse getFriendship() {
        return friendship;
    }

    public void setFriendship(FriendSummaryResponse friendship) {
        this.friendship = friendship;
    }

    public SocialSummaryResponse getSummary() {
        return summary;
    }

    public void setSummary(SocialSummaryResponse summary) {
        this.summary = summary;
    }
}
