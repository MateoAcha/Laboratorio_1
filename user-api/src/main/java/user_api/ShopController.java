package user_api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping("/shop/items")
    public ShopCatalogResponse getCatalog() {
        return shopService.getCatalog();
    }

    @PostMapping("/users/{id}/shop/buy")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void buyItem(@PathVariable Integer id, @RequestBody BuyRequest request) {
        shopService.purchaseItem(id, request.shopItemId);
    }

    static class BuyRequest {
        public int shopItemId;
    }
}
