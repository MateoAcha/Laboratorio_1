package user_api;

import java.util.List;

public class UserSkinsResponse {

    private int userId;
    private List<SkinResponse> skins;

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public List<SkinResponse> getSkins() { return skins; }
    public void setSkins(List<SkinResponse> skins) { this.skins = skins; }
}
