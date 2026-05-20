package com.restaurant.order_app.health;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public HealthResponse getHealth() {

        return new HealthResponse(
                "UP",
                "Backend Running",
                LocalDateTime.now()
        );
    }
}