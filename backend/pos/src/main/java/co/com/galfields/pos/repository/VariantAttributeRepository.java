package co.com.galfields.pos.repository;

import co.com.galfields.pos.entity.VariantAttribute;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VariantAttributeRepository extends JpaRepository<VariantAttribute, Long> {

    List<VariantAttribute> findByVariant_VariantId(Long variantId);
}
