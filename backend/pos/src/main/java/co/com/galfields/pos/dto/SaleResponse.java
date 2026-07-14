package co.com.galfields.pos.dto;

public record SaleResponse(
        Long transactionId,
        String clientEventId,
        boolean alreadyProcessed) {
}
