package user_api;

public class InventoryItemResponse {

    private Integer userInventoryId;
    private Integer itemId;
    private String itemName;
    private String itemType;
    private String rarity;
    private String description;
    private Integer quantity;
    private String acquiredAt;
    private String detailSummary;

    public Integer getUserInventoryId() {
        return userInventoryId;
    }

    public void setUserInventoryId(Integer userInventoryId) {
        this.userInventoryId = userInventoryId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getAcquiredAt() {
        return acquiredAt;
    }

    public void setAcquiredAt(String acquiredAt) {
        this.acquiredAt = acquiredAt;
    }

    public String getDetailSummary() {
        return detailSummary;
    }

    public void setDetailSummary(String detailSummary) {
        this.detailSummary = detailSummary;
    }
}
