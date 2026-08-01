package com.novastack.sms.dto.request;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Africa's Talking Delivery Report form fields
 * (application/x-www-form-urlencoded — not JSON).
 */
@Data
public class AfricasTalkingDlrCallback {

    private String id;
    private String status;
    private String phoneNumber;
    private String networkCode;
    private String failureReason;
    private String retryCount;

    public static AfricasTalkingDlrCallback from(Map<String, String> params) {
        AfricasTalkingDlrCallback callback = new AfricasTalkingDlrCallback();
        if (params == null || params.isEmpty()) {
            return callback;
        }
        callback.setId(first(params, "id", "messageId"));
        callback.setStatus(first(params, "status"));
        callback.setPhoneNumber(first(params, "phoneNumber", "number"));
        callback.setNetworkCode(first(params, "networkCode"));
        callback.setFailureReason(first(params, "failureReason"));
        callback.setRetryCount(first(params, "retryCount"));
        return callback;
    }

    public Map<String, String> asMap() {
        Map<String, String> map = new LinkedHashMap<>();
        if (id != null) map.put("id", id);
        if (status != null) map.put("status", status);
        if (phoneNumber != null) map.put("phoneNumber", phoneNumber);
        if (networkCode != null) map.put("networkCode", networkCode);
        if (failureReason != null) map.put("failureReason", failureReason);
        if (retryCount != null) map.put("retryCount", retryCount);
        return map;
    }

    private static String first(Map<String, String> params, String... keys) {
        for (String key : keys) {
            String value = params.get(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        for (String key : keys) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey() != null
                        && entry.getKey().equalsIgnoreCase(key)
                        && entry.getValue() != null
                        && !entry.getValue().isBlank()) {
                    return entry.getValue().trim();
                }
            }
        }
        return null;
    }

    public boolean hasId() {
        return id != null && !id.isBlank();
    }

    public String normalizedStatus() {
        return status == null ? null : status.trim().toLowerCase(Locale.ROOT);
    }
}
