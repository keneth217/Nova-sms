package com.novastack.sms.dto.request;

import com.novastack.sms.domain.enums.OrganizationAccountType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrganizationRegisterRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(max = 30)
    private String phone;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @NotBlank
    @Size(max = 150)
    private String adminFullName;

    /**
     * BUSINESS — ongoing use.
     * EVENT — one-time / short-term events; active for 7 days.
     * Defaults to BUSINESS when omitted.
     */
    private OrganizationAccountType accountType = OrganizationAccountType.BUSINESS;

    private boolean termsAccepted;

    @AssertTrue(message = "You must accept the Terms of Service, Privacy Policy, and Acceptable Use Policy")
    public boolean isLegalAccepted() {
        return termsAccepted;
    }
}
