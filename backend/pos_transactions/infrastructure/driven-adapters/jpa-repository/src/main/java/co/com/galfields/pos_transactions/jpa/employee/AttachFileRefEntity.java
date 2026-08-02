package co.com.galfields.pos_transactions.jpa.employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Read-only shadow of {@code attach_files}, scoped to resolving the shared
 * 'no-employee-photo' placeholder id by name — own copy for the Empleados
 * module, same shape as jpa.sale.shadow's pattern; coexists with
 * jpa.catalog.AttachFileEntity (Fase 4's real owner) over the same table. */
@Entity
@Table(name = "attach_files")
@Getter
@Setter
public class AttachFileRefEntity {

    @Id
    @Column(name = "attach_files_id")
    private Long attachFilesId;

    @Column(nullable = false, length = 255)
    private String name;
}
