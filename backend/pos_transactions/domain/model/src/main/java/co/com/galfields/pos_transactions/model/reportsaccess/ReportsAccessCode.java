package co.com.galfields.pos_transactions.model.reportsaccess;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class ReportsAccessCode {
    private Long reportsAccessCodeId;
    private String code;
    private LocalDateTime generatedAt;
}
