package user_api;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PaymentService {

    private final JdbcTemplate jdbcTemplate;
    private final InventoryService inventoryService;

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.webhook-url}")
    private String webhookUrl;

    @Value("${mercadopago.sandbox:true}")
    private boolean sandbox;

    public PaymentService(JdbcTemplate jdbcTemplate, InventoryService inventoryService) {
        this.jdbcTemplate = jdbcTemplate;
        this.inventoryService = inventoryService;
    }

    public PaymentPreferenceResponse createPreference(Integer userId, int emeralds, int pesosPrice) throws Exception {
        MercadoPagoConfig.setAccessToken(accessToken);

        // Insert preliminary record first so we can use its id as externalReference
        Long recordId = jdbcTemplate.queryForObject(
                """
                INSERT INTO payment_record (user_id, preference_id, emeralds, pesos_price)
                VALUES (?, 'pending', ?, ?)
                RETURNING id
                """,
                Long.class,
                userId, emeralds, pesosPrice);

        System.out.println("[Payment] Creating preference. webhookUrl=" + webhookUrl + "  accessToken starts with: " + (accessToken != null && accessToken.length() > 10 ? accessToken.substring(0, 10) : "MISSING"));

        PreferenceRequest req = PreferenceRequest.builder()
                .items(List.of(
                        PreferenceItemRequest.builder()
                                .title(emeralds + " Emeralds")
                                .quantity(1)
                                .unitPrice(new BigDecimal(pesosPrice))
                                .currencyId("ARS")
                                .build()))
                .externalReference(String.valueOf(recordId))
                .notificationUrl(webhookUrl + "/payments/webhook")
                .build();

        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(req);

        jdbcTemplate.update(
                "UPDATE payment_record SET preference_id = ? WHERE id = ?",
                preference.getId(), recordId);

        String checkoutUrl = sandbox ? preference.getSandboxInitPoint() : preference.getInitPoint();
        return new PaymentPreferenceResponse(recordId, checkoutUrl);
    }

    public String getPaymentStatus(Long recordId) {
        return jdbcTemplate.query(
                "SELECT status FROM payment_record WHERE id = ?",
                rs -> rs.next() ? rs.getString("status") : "NOT_FOUND",
                recordId);
    }

    @Transactional
    public String verifyAndGrant(Long recordId) {
        PaymentRecordRow record = jdbcTemplate.query(
                "SELECT id, user_id, emeralds, preference_id FROM payment_record WHERE id = ?",
                rs -> rs.next()
                        ? new PaymentRecordRow(rs.getLong("id"), rs.getInt("user_id"), rs.getInt("emeralds"), rs.getString("preference_id"))
                        : null,
                recordId);

        if (record == null) return "NOT_FOUND";

        String currentStatus = jdbcTemplate.query(
                "SELECT status FROM payment_record WHERE id = ?",
                rs -> rs.next() ? rs.getString("status") : "NOT_FOUND",
                recordId);

        if ("APPROVED".equals(currentStatus)) {
            System.out.println("[Payment] verify recordId=" + recordId + " already APPROVED in DB — skipping grant");
            return "APPROVED";
        }

        try {
            java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.mercadopago.com/v1/payments/search?external_reference=" + recordId))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response =
                    http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

            System.out.println("[Payment] verify recordId=" + recordId + " httpStatus=" + response.statusCode() + " body=" + response.body());

            com.google.gson.JsonObject root =
                    com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject();

            com.google.gson.JsonArray results = root.getAsJsonArray("results");
            if (results == null || results.isEmpty()) {
                System.out.println("[Payment] verify: no results found");
                return "PENDING";
            }

            for (com.google.gson.JsonElement el : results) {
                com.google.gson.JsonObject payment = el.getAsJsonObject();
                String status = payment.has("status") ? payment.get("status").getAsString() : "";
                System.out.println("[Payment] verify: found payment status=" + status);
                if ("approved".equalsIgnoreCase(status)) {
                    long mpPaymentId = payment.has("id") ? payment.get("id").getAsLong() : 0L;
                    jdbcTemplate.update(
                            "UPDATE payment_record SET status = 'APPROVED', mp_payment_id = ?, updated_at = NOW() WHERE id = ? AND status = 'PENDING'",
                            mpPaymentId, recordId);
                    inventoryService.addEmeralds(record.userId(), record.emeralds());
                    System.out.println("[Payment] verify: APPROVED — granted " + record.emeralds() + " emeralds to userId=" + record.userId());
                    return "APPROVED";
                }
            }
            return "PENDING";
        } catch (Exception e) {
            System.err.println("[Payment] verifyAndGrant failed: " + e.getMessage());
            e.printStackTrace();
            return "PENDING";
        }
    }

    @Transactional
    public void handleWebhook(String type, Long mpPaymentId) {
        if (mpPaymentId == null) return;
        if (!"payment".equalsIgnoreCase(type)) return;

        try {
            MercadoPagoConfig.setAccessToken(accessToken);
            PaymentClient paymentClient = new PaymentClient();
            Payment payment = paymentClient.get(mpPaymentId);

            if (payment == null || !"approved".equalsIgnoreCase(payment.getStatus())) return;

            String externalRef = payment.getExternalReference();
            if (externalRef == null || externalRef.isBlank()) return;

            long recordId;
            try { recordId = Long.parseLong(externalRef.trim()); }
            catch (NumberFormatException e) { return; }

            PaymentRecordRow record = jdbcTemplate.query(
                    """
                    SELECT id, user_id, emeralds
                    FROM payment_record
                    WHERE id = ? AND status = 'PENDING'
                    """,
                    rs -> rs.next()
                            ? new PaymentRecordRow(rs.getLong("id"), rs.getInt("user_id"), rs.getInt("emeralds"), null)
                            : null,
                    recordId);

            if (record == null) return;

            jdbcTemplate.update(
                    "UPDATE payment_record SET status = 'APPROVED', mp_payment_id = ?, updated_at = NOW() WHERE id = ?",
                    mpPaymentId, record.id());

            inventoryService.addEmeralds(record.userId(), record.emeralds());

        } catch (Exception e) {
            throw new RuntimeException("Webhook processing failed", e);
        }
    }

    private record PaymentRecordRow(long id, int userId, int emeralds, String preferenceId) {}
}
