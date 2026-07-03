package user_api;

import java.util.ArrayList;
import java.util.List;

public class SocialSummaryResponse {

    private List<FriendSummaryResponse> friends = new ArrayList<>();
    private List<FriendRequestResponse> incomingFriendRequests = new ArrayList<>();
    private List<FriendRequestResponse> sentFriendRequests = new ArrayList<>();
    private List<GameInviteResponse> gameInvites = new ArrayList<>();

    public List<FriendSummaryResponse> getFriends() {
        return friends;
    }

    public void setFriends(List<FriendSummaryResponse> friends) {
        this.friends = friends;
    }

    public List<FriendRequestResponse> getIncomingFriendRequests() {
        return incomingFriendRequests;
    }

    public void setIncomingFriendRequests(List<FriendRequestResponse> incomingFriendRequests) {
        this.incomingFriendRequests = incomingFriendRequests;
    }

    public List<FriendRequestResponse> getSentFriendRequests() {
        return sentFriendRequests;
    }

    public void setSentFriendRequests(List<FriendRequestResponse> sentFriendRequests) {
        this.sentFriendRequests = sentFriendRequests;
    }

    public List<GameInviteResponse> getGameInvites() {
        return gameInvites;
    }

    public void setGameInvites(List<GameInviteResponse> gameInvites) {
        this.gameInvites = gameInvites;
    }
}
