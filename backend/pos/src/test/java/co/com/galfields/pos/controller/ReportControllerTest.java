package co.com.galfields.pos.controller;

import static org.assertj.core.api.Assertions.assertThat;

import co.com.galfields.pos.service.ReportService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Covers specs/01-reportes-mobile-pos-zona-horaria.md's boundary fix: report
 * "days" must be Bogotá calendar days, converted to the UTC-naive instant
 * transaction_date is actually stored in — not the server's own runtime zone
 * (this pod runs UTC).
 */
class ReportControllerTest {

    private final ReportController controller = new ReportController(new ReportService(null, null, null, null));

    @Test
    void aLateEveningBogotaSaleFallsOnItsOwnBogotaDay() {
        // The real case that surfaced this bug: FAC-000261, sold at 19:08
        // Bogotá on 2026-08-03, was stored as 2026-08-04T00:08:29 UTC and
        // excluded from the "2026-08-03" report by the old naive boundary.
        LocalDate day = LocalDate.of(2026, 8, 3);

        LocalDateTime start = controller.startOf(day, day);
        LocalDateTime end = controller.endOf(day);

        LocalDateTime saleInstantUtc = LocalDateTime.of(2026, 8, 4, 0, 8, 29);

        assertThat(saleInstantUtc).isAfterOrEqualTo(start).isBeforeOrEqualTo(end);
    }

    @Test
    void startOfIsFiveHoursAheadOfTheNaiveBogotaMidnight() {
        // Bogotá is UTC-5 with no DST, so 2026-08-03T00:00:00 Bogotá is
        // 2026-08-03T05:00:00 UTC — the old bug compared against
        // 2026-08-03T00:00:00 UTC instead (5 hours too early).
        LocalDate day = LocalDate.of(2026, 8, 3);

        LocalDateTime start = controller.startOf(day, day);

        assertThat(start).isEqualTo(LocalDateTime.of(2026, 8, 3, 5, 0, 0));
    }

    @Test
    void endOfIsFiveHoursAheadOfTheNaiveBogotaEndOfDay() {
        LocalDate day = LocalDate.of(2026, 8, 3);

        LocalDateTime end = controller.endOf(day);

        assertThat(end).isEqualTo(LocalDateTime.of(2026, 8, 4, 4, 59, 59, 999_999_999));
    }

    @Test
    void aSaleJustBeforeTheOldBuggyCutoffIsExcludedFromTheNextDay() {
        // 2026-08-04T04:59:59.999999999 UTC is still 2026-08-03 in Bogotá —
        // must NOT be included in the "2026-08-04" report.
        LocalDate nextDay = LocalDate.of(2026, 8, 4);

        LocalDateTime start = controller.startOf(nextDay, nextDay);

        LocalDateTime justBeforeBogotaMidnight = LocalDateTime.of(2026, 8, 4, 4, 59, 59, 999_999_999);
        assertThat(justBeforeBogotaMidnight).isBefore(start);
    }
}
