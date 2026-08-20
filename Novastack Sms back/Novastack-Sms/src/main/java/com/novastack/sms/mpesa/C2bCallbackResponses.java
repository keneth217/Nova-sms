package com.novastack.sms.mpesa;

import java.util.LinkedHashMap;
import java.util.Map;

public final class C2bCallbackResponses {

    private C2bCallbackResponses() {
    }

    /** Confirmation and validation accept. Payment already succeeded for confirmation. */
    public static Map<String, Object> accepted() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ResultCode", "0");
        body.put("ResultDesc", "Accepted");
        return body;
    }

    public static Map<String, Object> rejected(String resultCode, String resultDesc) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ResultCode", resultCode);
        body.put("ResultDesc", resultDesc);
        return body;
    }

    public static Map<String, Object> invalidAccount() {
        return rejected("C2B00012", "Rejected");
    }

    public static Map<String, Object> invalidAmount() {
        return rejected("C2B00013", "Rejected");
    }

    public static Map<String, Object> invalidMsisdn() {
        return rejected("C2B00011", "Rejected");
    }

    public static Map<String, Object> invalidShortcode() {
        return rejected("C2B00015", "Rejected");
    }

    public static Map<String, Object> otherError() {
        return rejected("C2B00016", "Rejected");
    }
}
