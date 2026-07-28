package co.com.galfields.pos.repository;

import co.com.galfields.pos.entity.AttachFile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachFileRepository extends JpaRepository<AttachFile, Long> {

    Optional<AttachFile> findByName(String name);
}
