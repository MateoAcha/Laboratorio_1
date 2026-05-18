package user_api;

public class SkinResponse {

    private int skinId;
    private String skinName;
    private String rarity;
    private String skinColor;
    private boolean equipped;

    public int getSkinId() { return skinId; }
    public void setSkinId(int skinId) { this.skinId = skinId; }

    public String getSkinName() { return skinName; }
    public void setSkinName(String skinName) { this.skinName = skinName; }

    public String getRarity() { return rarity; }
    public void setRarity(String rarity) { this.rarity = rarity; }

    public String getSkinColor() { return skinColor; }
    public void setSkinColor(String skinColor) { this.skinColor = skinColor; }

    public boolean isEquipped() { return equipped; }
    public void setEquipped(boolean equipped) { this.equipped = equipped; }
}
