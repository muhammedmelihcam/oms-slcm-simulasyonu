package com.melih.omsslcm.dto;

import jakarta.validation.constraints.NotBlank;

public record SigninRequest(
        @NotBlank String email,
        @NotBlank String password) {
}
