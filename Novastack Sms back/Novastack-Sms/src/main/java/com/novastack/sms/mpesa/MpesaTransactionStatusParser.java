package com.novastack.sms.mpesa;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Daraja Transaction Status result callbacks. Not a client API.
 */
public final class MpesaTransactionStatusParser {

    private static final Pattern MSISDN = Pattern.compile("(254\\d{9}|0\\d{9})");

    private MpesaTransactionStatusParser() {
    }

    public static ParsedResult parse(JsonNode payload, String shortcode) {
        JsonNode result = unwrap(payload);
        Map<String, String> params = resultParameters(result);
        String resultCode = first(text(result, "ResultCode"), params.get("ResultCode"));
        String resultDesc = first(text(result, "ResultDesc"), params.get("ResultDesc"));
        String receipt = first(
                text(result, "TransactionID"),
                params.get("ReceiptNo"),
                params.get("TransactionID"),
                params.get("TransID"));
        String amountStr = first(params.get("Amount"), params.get("TransAmount"));
        String billRef = first(
                params.get("BillReferenceNumber"),
                params.get("BillRefNumber"),
                params.get("AccountReference"),
                params.get("AccountNumber"),
                billRefFromCreditParty(params.get("CreditPartyName"), shortcode));
        String phone = first(
                params.get("MSISDN"),
                params.get("PhoneNumber"),
                msisdnFrom(params.get("DebitPartyName")),
                msisdnFrom(params.get("CreditPartyName")));
        String transTime = first(
                params.get("FinalisedTime"),
                params.get("TransCompletedTime"),
                params.get("InitiatedTime"),
                params.get("TransactionDate"));
        String transactionStatus = params.get("TransactionStatus");
        return new ParsedResult(
                text(result, "OriginatorConversationID"),
                text(result, "ConversationID"),
                resultCode,
                resultDesc,
                receipt == null ? null : receipt.trim().toUpperCase(Locale.ROOT),
                parseAmount(amountStr),
                billRef == null ? null : billRef.trim(),
                phone,
                transTime,
                transactionStatus,
                isCompleted(resultCode, transactionStatus),
                payload == null ? null : payload.toString());
    }

    public static JsonNode unwrap(JsonNode payload) {
        if (payload == null || payload.isMissingNode() || payload.isNull()) {
            return payload;
        }
        if (payload.has("Result") && payload.get("Result").isObject()) {
            return payload.get("Result");
        }
        return payload;
    }

    static Map<String, String> resultParameters(JsonNode result) {
        Map<String, String> out = new LinkedHashMap<>();
        if (result == null) {
            return out;
        }
        JsonNode container = result.path("ResultParameters").path("ResultParameter");
        if (container.isMissingNode() || container.isNull()) {
            container = result.path("ReferenceData").path("ReferenceItem");
        }
        if (container.isArray()) {
            for (JsonNode item : container) {
                putParam(out, item);
            }
        } else if (container.isObject()) {
            putParam(out, container);
        }
        return out;
    }

    private static void putParam(Map<String, String> out, JsonNode item) {
        String key = text(item, "Key");
        String value = text(item, "Value");
        if (key != null && value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) {
            out.put(key, value.trim());
        }
    }

    static boolean isCompleted(String resultCode, String transactionStatus) {
        if (resultCode != null && !"0".equals(resultCode.trim())) {
            return false;
        }
        if (transactionStatus == null || transactionStatus.isBlank()) {
            return "0".equals(resultCode);
        }
        String status = transactionStatus.trim().toLowerCase(Locale.ROOT);
        return "completed".equals(status) || "success".equals(status) || "0".equals(status);
    }

    static String billRefFromCreditParty(String creditPartyName, String shortcode) {
        if (creditPartyName == null || creditPartyName.isBlank()) {
            return null;
        }
        String value = creditPartyName.trim();
        int dash = value.lastIndexOf('-');
        if (dash >= 0 && dash < value.length() - 1) {
            String tail = value.substring(dash + 1).trim();
            if (!tail.isBlank() && (shortcode == null || !tail.equals(shortcode))) {
                return tail;
            }
        }
        if (shortcode != null && !shortcode.isBlank()) {
            String prefix = shortcode.trim();
            if (value.startsWith(prefix) && value.length() > prefix.length()) {
                String tail = value.substring(prefix.length()).replaceFirst("^\\s*[-:]\\s*", "").trim();
                return tail.isBlank() ? null : tail;
            }
        }
        return null;
    }

    static String msisdnFrom(String partyName) {
        if (partyName == null) {
            return null;
        }
        Matcher matcher = MSISDN.matcher(partyName.replace(" ", ""));
        return matcher.find() ? matcher.group(1) : null;
    }

    private static BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(amountStr.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String first(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || field == null) {
            return null;
        }
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText(null);
        return text == null || text.isBlank() || "null".equalsIgnoreCase(text) ? null : text.trim();
    }

    public record ParsedResult(
            String originatorConversationId,
            String conversationId,
            String resultCode,
            String resultDesc,
            String receipt,
            BigDecimal amount,
            String billRef,
            String phone,
            String transactionDate,
            String transactionStatus,
            boolean completed,
            String rawPayload
    ) {
        public boolean canCredit() {
            return completed && receipt != null && amount != null
                    && amount.compareTo(BigDecimal.ZERO) > 0
                    && billRef != null && !billRef.isBlank();
        }
    }
}
