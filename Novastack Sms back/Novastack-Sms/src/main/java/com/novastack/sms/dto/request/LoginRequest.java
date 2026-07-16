package com.novastack.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    /**
     * Email address or phone number (e.g. 2547XXXXXXXX / 07XXXXXXXX).
     * Kept as {@code email} in JSON for backward compatibility with older clients;
     * also accepts {@code emailOrPhone} via setter alias if needed.
     */
    @NotBlank
    @Size(max = 180)
    private String email;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    /** Alias so clients can send {"emailOrPhone": "..."} */
    public void setEmailOrPhone(String emailOrPhone) {
        this.email = emailOrPhone;
    }

    public String getEmailOrPhone() {
        return email;
    }
}
