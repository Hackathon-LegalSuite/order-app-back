package com.restaurant.order_app.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/** DTO de entrada para el login del staff (cocinero o mesero). */
@Getter
public class StaffAuthRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
