package com.novastack.sms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "novastack")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Api api = new Api();
    private Sms sms = new Sms();
    private AfricasTalking africastalking = new AfricasTalking();
    private Mpesa mpesa = new Mpesa();
    private DataBundles dataBundles = new DataBundles();
    private SuperAdmin superAdmin = new SuperAdmin();
    private OrganizationDefaults organization = new OrganizationDefaults();
    private Notifications notifications = new Notifications();

    @Getter
    @Setter
    public static class Api {
        private String publicBaseUrl = "https://smsapi.novastack.co.ke";
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "7f3c9e2a8d4b1f6c5e7a9d2c4b8f1e6a3c7d9b2f5e8a1c4d6f9b3e7a2d5c8f1b4e6a9d3c7f2b5e8a1d4c6f9b2e7a5";
        private long expirationMs = 86400000L;
    }

    @Getter
    @Setter
    public static class Sms {
        private String provider = "talksasa";
        private BigDecimal defaultCost = new BigDecimal("1.00");
        private String platformSenderId = "NOVASTACK";
        private int maxRetries = 3;
        private int batchSize = 100;
        private Pricing pricing = new Pricing();
        private Billing billing = new Billing();
        private StatusSync statusSync = new StatusSync();
        private ProviderUnitsAlert providerUnitsAlert = new ProviderUnitsAlert();
        private TalkSasa talksasa = new TalkSasa();
    }

    @Getter
    @Setter
    public static class Pricing {
        private BigDecimal pricePerUnit = new BigDecimal("1.00");
        private BigDecimal whatsappPricePerUnit;
        private String currency = "KES";
    }

    @Getter
    @Setter
    public static class Billing {
        private BigDecimal customerPrice = new BigDecimal("1.00");
        private BigDecimal providerCost = new BigDecimal("0.35");
        private String currency = "KES";
    }

    @Getter
    @Setter
    public static class StatusSync {
        private boolean enabled = true;
        private String cron = "0 */5 * * * *";
        private int batchSize = 50;
    }

    @Getter
    @Setter
    public static class ProviderUnitsAlert {
        private boolean enabled = true;
        private String cron = "0 */10 * * * *";
    }

    @Getter
    @Setter
    public static class TalkSasa {
        public static final String DEFAULT_SENDER_ID = "TALK-SASA";

        private String baseUrl = "https://bulksms.talksasa.com/api/v3";
        private String apiToken = "";
        private String defaultSenderId = DEFAULT_SENDER_ID;
        private int connectTimeoutMs = 5_000;
        private int readTimeoutMs = 30_000;
        private boolean syncContactGroups = true;

        public String resolvedDefaultSenderId() {
            String value = defaultSenderId == null || defaultSenderId.isBlank()
                    ? DEFAULT_SENDER_ID
                    : defaultSenderId.trim();
            if ("TALK_SASA".equalsIgnoreCase(value)) {
                return DEFAULT_SENDER_ID;
            }
            return value;
        }
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
        /**
         * API operator username from the M-Pesa portal. Required for Transaction Status
         * (internal C2B reconciliation). Not used by client applications.
         */
        private String initiatorName = "";
        /** Plain initiator password. Used only when {@code securityCredential} is empty. */
        private String initiatorPassword = "";
        /** Pre-encrypted Daraja SecurityCredential. Preferred over encrypting the password at runtime. */
        private String securityCredential = "";
        /** Path or classpath location of the Safaricom initiator .cer (production or sandbox). */
        private String initiatorCertificatePath = "";
        /**
         * Paybill account numbers that are recorded for stats only.
         * They never credit an organization wallet.
         */
        private List<String> collectionAccounts = new ArrayList<>();
        /** Phones notified when a collection account is paid. Not billed to any org. */
        private List<String> collectionNotifyPhones = new ArrayList<>();
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
        private String phone = "0711766223";
    }

    @Getter
    @Setter
    public static class OrganizationDefaults {
        /** How long EVENT accounts stay active after registration. */
        private int eventActiveDays = 7;
    }

    @Getter
    @Setter
    public static class Notifications {
        /** Platform SMS to org phone: welcome, top-up receipt, low-balance. Not billed to the org. */
        private boolean enabled = true;
        /** Alert when wallet balance crosses from above this amount to at-or-below it. */
        private BigDecimal lowBalanceThreshold = new BigDecimal("50.00");
        private String portalUrl = "https://novasms.novastack.co.ke";
        private Templates templates = new Templates();
    }

    @Getter
    @Setter
    public static class Templates {
        private String welcome =
                "Welcome to Nova SMS, {name}! Your organization account is ready. Top up your wallet via M-Pesa to start sending SMS. {portalUrl}";
        private String topup =
                "Nova SMS: KES {amount} credited to your wallet.{receipt} New balance: KES {balance}.";
        private String collection =
                "KES {amount} has been received from {payer} for {account}.{receipt}";
        private String lowBalance =
                "Nova SMS: Your wallet balance is low (KES {balance}). Top up via M-Pesa to keep sending SMS.";
        private String platformTopup =
                "Nova SMS: {name} ({account}) topped up KES {amount}.{receipt} Bal KES {balance}. {time}";
        private String providerLow =
                "Nova SMS: TalkSasa remaining {units} units. Threshold {threshold}. Top up the provider account.";
        private String providerExposure =
                "Nova SMS: Org wallets KES {wallets} exceed TalkSasa {units} units. Top up the provider account.";
    }
}
