package co.com.galfields.pos_transactions.model.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Category {
    private Long categoryId;
    private String name;
    private String description;
    private LocalDateTime createdAt;
}
