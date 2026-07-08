package co.com.galfields.pos.dto;

import java.time.LocalDateTime;

public record LocationResponse(
        Long locationId,
        String name,
        String address,
        String phone,
        LocalDateTime createdAt) {
}
