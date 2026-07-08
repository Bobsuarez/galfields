package co.com.galfields.pos.dto;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long categoryId,
        String name,
        String description,
        LocalDateTime createdAt) {
}
