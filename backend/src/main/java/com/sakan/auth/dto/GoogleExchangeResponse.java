package com.sakan.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoogleExchangeResponse(boolean registrationRequired, String email, String token, String tokenType) {

    public static GoogleExchangeResponse registration(String email) {
        return new GoogleExchangeResponse(true, email, null, null);
    }

    public static GoogleExchangeResponse authenticated(String token) {
        return new GoogleExchangeResponse(false, null, token, "Bearer");
    }
}
