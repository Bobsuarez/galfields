package co.com.galfields.pos.repository;

import co.com.galfields.pos.entity.Location;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findByName(String name);
}
