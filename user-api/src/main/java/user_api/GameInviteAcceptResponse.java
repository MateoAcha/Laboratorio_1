package user_api;

public class GameInviteAcceptResponse {

    private String result;
    private Long inviteId;
    private Integer roomNumber;
    private Integer hostUserId;
    private String hostUsername;
    private GameInviteResponse gameInvite;
    private LobbyRoomService.RoomSummary lobby;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Long getInviteId() {
        return inviteId;
    }

    public void setInviteId(Long inviteId) {
        this.inviteId = inviteId;
    }

    public Integer getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(Integer roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Integer getHostUserId() {
        return hostUserId;
    }

    public void setHostUserId(Integer hostUserId) {
        this.hostUserId = hostUserId;
    }

    public String getHostUsername() {
        return hostUsername;
    }

    public void setHostUsername(String hostUsername) {
        this.hostUsername = hostUsername;
    }

    public GameInviteResponse getGameInvite() {
        return gameInvite;
    }

    public void setGameInvite(GameInviteResponse gameInvite) {
        this.gameInvite = gameInvite;
    }

    public LobbyRoomService.RoomSummary getLobby() {
        return lobby;
    }

    public void setLobby(LobbyRoomService.RoomSummary lobby) {
        this.lobby = lobby;
    }
}
