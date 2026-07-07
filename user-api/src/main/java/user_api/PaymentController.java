package user_api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    public PaymentController(PaymentService paymentService, UserRepository userRepository) {
        this.paymentService = paymentService;
        this.userRepository = userRepository;
    }

    @PostMapping("/{userId}/create-preference")
    public PaymentPreferenceResponse createPreference(
            @PathVariable Integer userId,
            @RequestBody CreatePreferenceRequest request) {

        if (!userRepository.existsById(userId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        if (request.getEmeralds() <= 0 || request.getPesosPrice() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pack");

        try {
            return paymentService.createPreference(userId, request.getEmeralds(), request.getPesosPrice());
        } catch (Exception e) {
            System.err.println("[Payment] createPreference failed: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create payment preference: " + e.getMessage());
        }
    }

    @GetMapping("/status/{recordId}")
    public PaymentStatusResponse getStatus(@PathVariable Long recordId) {
        return new PaymentStatusResponse(paymentService.getPaymentStatus(recordId));
    }

    @PostMapping("/verify/{recordId}")
    public PaymentStatusResponse verify(@PathVariable Long recordId) {
        return new PaymentStatusResponse(paymentService.verifyAndGrant(recordId));
    }

    // MercadoPago sends webhooks in two formats:
    // Legacy IPN:  POST /payments/webhook?topic=payment&id=12345
    // New webhook: POST /payments/webhook  body: {"type":"payment","data":{"id":"12345"}}
    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.OK)
    public void handleWebhook(
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "id", required = false) String idParam,
            @RequestBody(required = false) String rawBody) {

        String type = topic;
        Long mpPaymentId = parseLong(idParam);

        if (rawBody != null && !rawBody.isBlank()) {
            try {
                JsonObject root = JsonParser.parseString(rawBody).getAsJsonObject();
                if (type == null && root.has("type"))
                    type = root.get("type").getAsString();
                if (mpPaymentId == null && root.has("data")) {
                    JsonObject data = root.getAsJsonObject("data");
                    if (data.has("id"))
                        mpPaymentId = parseLong(data.get("id").getAsString());
                }
            } catch (Exception ignored) {}
        }

        paymentService.handleWebhook(type, mpPaymentId);
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Long.parseLong(value.trim()); } catch (NumberFormatException e) { return null; }
    }
}
