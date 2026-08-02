package co.com.galfields.pos_transactions.model.reportsaccess.gateways;

import co.com.galfields.pos_transactions.model.reportsaccess.ReportsAccessCode;

import java.util.Optional;

public interface ReportsAccessCodeRepository {
    ReportsAccessCode save(ReportsAccessCode code);

    /** The currently valid code is always the most recently generated row —
     * see backend/pos's "no expiry column on purpose" note. */
    Optional<ReportsAccessCode> findLatest();
}
