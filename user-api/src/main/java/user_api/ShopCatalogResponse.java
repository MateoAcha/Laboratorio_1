package user_api;

import java.util.List;

public class ShopCatalogResponse {

    private List<ShopItemResponse> items;

    public List<ShopItemResponse> getItems() { return items; }
    public void setItems(List<ShopItemResponse> items) { this.items = items; }
}
