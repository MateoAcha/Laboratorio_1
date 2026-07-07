package user_api;

public class PaymentPreferenceResponse {

    private Long paymentRecordId;
    private String checkoutUrl;

    public PaymentPreferenceResponse(Long paymentRecordId, String checkoutUrl) {
        this.paymentRecordId = paymentRecordId;
        this.checkoutUrl = checkoutUrl;
    }

    public Long getPaymentRecordId() { return paymentRecordId; }
    public void setPaymentRecordId(Long paymentRecordId) { this.paymentRecordId = paymentRecordId; }

    public String getCheckoutUrl() { return checkoutUrl; }
    public void setCheckoutUrl(String checkoutUrl) { this.checkoutUrl = checkoutUrl; }
}
