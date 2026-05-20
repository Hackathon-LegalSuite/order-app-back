package com.restaurant.order_app.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;  

@RestController
@RequestMapping("/health")
public class HealthController {

    private final HealthService service = new HealthService();

    @GetMapping
    public HealthResponse health() {
        return service.getHealth();
    }
}