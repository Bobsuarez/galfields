package co.com.galfields.pos_transactions.usecase.sale;

import co.com.galfields.pos_transactions.model.sale.Sale;

public record RecordSaleResult(Sale sale, boolean alreadyProcessed) {
}
