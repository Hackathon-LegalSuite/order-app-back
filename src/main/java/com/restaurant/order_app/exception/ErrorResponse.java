package com.restaurant.order_app.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String error,
        String path,
        LocalDateTime timestamp
) {
}
