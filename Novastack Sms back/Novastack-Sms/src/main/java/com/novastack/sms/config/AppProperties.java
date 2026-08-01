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
    private DataBundles dataBundles = new DataBundles();
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
        private String username = "";
        private String apiKey = "";
        private String baseUrl = "https://api.africastalking.com";
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
        private String baseUrl = "https://api.safaricom.co.ke";
        private String accountReferencePrefix = "NOVA";
        private String transactionDesc = "Novastack SMS wallet top-up";
    }

    @Getter
    @Setter
    public static class DataBundles {
        private String consumerKey = "";
        private String consumerSecret = "";
        private String baseUrl = "https://api.safaricom.co.ke";
        private String oauthPath = "/oauth/v1/generate";
        /** GET /v1/dynamic-offers/fetch?msisdn= */
        private String offersPath = "/v1/dynamic-offers/fetch";
        /** POST /v1/dynamic-offers/facebook-bundle/purchase */
        private String purchasePath = "/v1/dynamic-offers/facebook-bundle/purchase";
        /** GET /v2/bundles/get/status?id=&serviceAccountId= */
        private String statusPath = "/v2/bundles/get/status";
        /** Use 0 for dynamic offers status queries. */
        private String serviceAccountId = "0";
        /**
         * Required purchase header {@code x-source-system}.
         * Facebook-bundle path examples use {@code fb}.
         */
        private String sourceSystem = "fb";
        /** Public HTTPS base used to build Safaricom callbacks. */
        private String callbackBaseUrl = "https://smsapi.novastack.co.ke";
        /** Optional shared secret expected in X-Callback-Token header. */
        private String callbackToken = "";
        private int rateLimitPerMinute = 30;
        /**
         * Organization used for unauthenticated public data-bundle purchases.
         * Seeded by Flyway as the Public Data Bundles system org.
         */
        private String publicOrganizationId = "a0000000-0000-4000-8000-0000000000db";
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
