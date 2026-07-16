package com.novastack.sms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Getter
@Setter
@ConfigurationProperties(prefix = "novastack")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Sms sms = new Sms();
    private AfricasTalking africastalking = new AfricasTalking();
    private Mpesa mpesa = new Mpesa();
    private SuperAdmin superAdmin = new SuperAdmin();
    private OrganizationDefaults organization = new OrganizationDefaults();

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "change-me-to-a-very-long-secure-secret-key-at-least-256-bits";
        private long expirationMs = 86400000L;
    }

    @Getter
    @Setter
    public static class Sms {
        private BigDecimal defaultCost = new BigDecimal("0.80");
        private String platformSenderId = "NOVASTACK";
        private int maxRetries = 3;
    }

    @Getter
    @Setter
    public static class AfricasTalking {
        private String username = "sandbox";
        private String apiKey = "";
        private String baseUrl = "https://api.sandbox.africastalking.com";
        private String messagingPath = "/version1/messaging";
    }

    @Getter
    @Setter
    public static class Mpesa {
        /** Paybill / Business short code */
        private String shortcode = "174379";
        private String passkey = "";
        private String consumerKey = "";
        private String consumerSecret = "";
        /** Public HTTPS callback base, e.g. https://api.yourdomain.com */
        private String callbackBaseUrl = "https://localhost:8080";
        private String baseUrl = "https://sandbox.safaricom.co.ke";
        private String accountReferencePrefix = "NOVA";
        private String transactionDesc = "Novastack SMS wallet top-up";
    }

    @Getter
    @Setter
    public static class SuperAdmin {
        private String email = "admin@novastack.com";
        private String password = "ChangeMe123!";
        private String fullName = "Novastack Super Admin";
    }

    @Getter
    @Setter
    public static class OrganizationDefaults {
        /** How long EVENT accounts stay active after registration. */
        private int eventActiveDays = 7;
    }
}
