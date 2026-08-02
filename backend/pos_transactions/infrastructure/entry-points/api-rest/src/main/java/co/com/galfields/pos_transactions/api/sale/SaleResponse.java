package co.com.galfields.pos_transactions.api.sale;

public record SaleResponse(
        Long transactionId,
        String clientEventId,
        boolean alreadyProcessed) {
}
