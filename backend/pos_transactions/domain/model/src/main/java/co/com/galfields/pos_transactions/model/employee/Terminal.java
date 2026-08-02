package co.com.galfields.pos_transactions.model.employee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Terminal {
    private Long terminalId;
    private String terminalCode;
    private String name;
    private boolean active;
    private LocalDateTime createdAt;
}
