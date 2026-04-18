package user_api;

import java.util.ArrayList;
import java.util.List;

public class UserInventoryResponse {

    private Integer userId;
    private List<InventoryItemResponse> items = new ArrayList<>();

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public List<InventoryItemResponse> getItems() {
        return items;
    }

    public void setItems(List<InventoryItemResponse> items) {
        this.items = items;
    }
}
