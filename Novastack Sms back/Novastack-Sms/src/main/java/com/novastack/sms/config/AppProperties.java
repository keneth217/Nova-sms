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
        private String secret = "7f3c9e2a8d4b1f6c5e7a9d2c4b8f1e6a3c7d9b2f5e8a1c4d6f9b3e7a2d5c8f1b4e6a9d3c7f2b5e8a1d4c6f9b2e7a5";
        private long expirationMs = 86400000L;
    }

    @Getter
    @Setter
    public static class Sms {
        private BigDecimal defaultCost = new BigDecimal("1.00");
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
        /** Public HTTPS callback base, e.g. https://smsapi.novastack.co.ke */
        private String callbackBaseUrl = "https://smsapi.novastack.co.ke";
        private String baseUrl = "https://sandbox.safaricom.co.ke";
        private String accountReferencePrefix = "NOVA";
        private String transactionDesc = "Novastack SMS wallet top-up";
    }

    @Getter
    @Setter
    public static class SuperAdmin {
        private String email = "kipyegonkeneth03@gmail.com";
        private String password = "Designer@3689.";
        private String fullName = "Keneth Kipyegon";
    }

    @Getter
    @Setter
    public static class OrganizationDefaults {
        /** How long EVENT accounts stay active after registration. */
        private int eventActiveDays = 7;
    }
}
