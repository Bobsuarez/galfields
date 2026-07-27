package co.com.galfields.pos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "reports_access_codes")
@Getter
@Setter
public class ReportsAccessCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reports_access_code_id")
    private Long reportsAccessCodeId;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
}
