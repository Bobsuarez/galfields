package co.com.galfields.pos_transactions.jpa.employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_roles")
@Getter
@Setter
public class EmployeeRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    /** Raw JSON string — Map<String,Boolean> (de)serialization happens in
     * the adapter, domain layer only ever sees the real Map. */
    @JdbcTypeCode(SqlTypes.JSON)
    private String permissions;

    @Column(name = "can_login_mobile", nullable = false)
    private boolean canLoginMobile;

    @Column(name = "can_login_desktop", nullable = false)
    private boolean canLoginDesktop;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
