package co.com.galfields.pos.repository;

import co.com.galfields.pos.entity.AttachFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachFileRepository extends JpaRepository<AttachFile, Long> {
}
