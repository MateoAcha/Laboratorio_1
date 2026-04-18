package user_api;

public class ShopItemResponse {

    private int shopItemId;
    private int itemId;
    private String itemName;
    private String itemType;
    private String rarity;
    private String description;
    private String detailSummary;
    private int goldPrice;
    private int purchaseQuantity;

    public int getShopItemId() { return shopItemId; }
    public void setShopItemId(int shopItemId) { this.shopItemId = shopItemId; }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getRarity() { return rarity; }
    public void setRarity(String rarity) { this.rarity = rarity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDetailSummary() { return detailSummary; }
    public void setDetailSummary(String detailSummary) { this.detailSummary = detailSummary; }

    public int getGoldPrice() { return goldPrice; }
    public void setGoldPrice(int goldPrice) { this.goldPrice = goldPrice; }

    public int getPurchaseQuantity() { return purchaseQuantity; }
    public void setPurchaseQuantity(int purchaseQuantity) { this.purchaseQuantity = purchaseQuantity; }
}
