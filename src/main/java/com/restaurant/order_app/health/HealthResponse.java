package com.restaurant.order_app.health;

import java.time.LocalDateTime;

public record HealthResponse(
        String status,
        String message,
        LocalDateTime timestamp
) {
}
