package co.com.galfields.pos_transactions.model;

import java.util.List;

/** Framework-agnostic paging/sort request — the entry-point layer converts
 * Spring's Pageable into this before calling a usecase, keeping Spring Data
 * types out of the domain layer. */
public record PageQuery(int page, int size, List<SortOrder> orders) {
    public record SortOrder(String property, boolean ascending) {
    }
}
