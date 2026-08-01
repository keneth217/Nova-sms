package com.novastack.sms.databundle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafaricomApiErrorMapperTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapsInsufficientFunds() {
        ApiException ex = SafaricomApiErrorMapper.fromBody(
                "purchase",
                400,
                "ResponseMsg=The account balance is insufficient. CustomerMsg=Dear customer, we are experiencing a technical issue.",
                "400");
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().toLowerCase().contains("insufficient"));
    }

    @Test
    void prefersInsufficientFundsOverGenericTechnicalCustomerMsg() {
        String body = """
                {"header":{"responseCode":400,"responseMessage":"The account balance is insufficient.",
                "customerMessage":"Dear customer, we are experiencing a technical issue. Please try again later."}}
                """;
        ApiException ex = SafaricomApiErrorMapper.fromBody("purchase", 400, body, "400");
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().toLowerCase().contains("insufficient"));
    }

    @Test
    void mapsRequestHeaderInvalid() {
        ApiException ex = SafaricomApiErrorMapper.fromBody(
                "purchase", 400, "Request Header Invalid", "400");
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().toLowerCase().contains("header"));
    }

    @Test
    void mapsUnauthorizedToken_401() {
        ApiException ex = SafaricomApiErrorMapper.fromBody("offers", 401, "Invalid Access Token", "401");
        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
        assertTrue(ex.getMessage().toLowerCase().contains("dynamic offers"));
    }

    @Test
    void mapsUnauthorizedToken_401_001() {
        ApiException ex = SafaricomApiErrorMapper.fromBody("offers", 401, "Unauthorised", "401.001");
        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
        assertTrue(ex.getMessage().toLowerCase().contains("access token"));
    }

    @Test
    void mapsMaintenance_503() {
        ApiException ex = SafaricomApiErrorMapper.fromBody(
                "offers", 503, "Service is currently under maintenance. Please try again later", "503");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        assertTrue(ex.getMessage().toLowerCase().contains("unavailable"));
    }

    @Test
    void mapsInvalidMsisdn() {
        ApiException ex = SafaricomApiErrorMapper.fromBody(
                "offers", 400, "Operation Failed: invalid MSISDN", "400");
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().toLowerCase().contains("invalid"));
    }

    @Test
    void mapsInvalidParameter_500() {
        ApiException ex = SafaricomApiErrorMapper.fromBody(
                "purchase", 500, "Invalid parameter input", "500");
        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
        assertTrue(ex.getMessage().toLowerCase().contains("invalid parameter"));
    }

    @Test
    void mapsTechnicalError_400() {
        ApiException ex = SafaricomApiErrorMapper.fromBody(
                "purchase",
                400,
                "Dear customer, we are experiencing a technical issue. Please try again later",
                "400");
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().toLowerCase().contains("airtime")
                || ex.getMessage().toLowerCase().contains("daraja"));
    }

    @Test
    void mapsUnsuccessfulRequest_400() {
        ApiException ex = SafaricomApiErrorMapper.fromBody(
                "purchase", 400, "Unsuccessfully request", "400");
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().toLowerCase().contains("could not"));
    }

    @Test
    void mapsDarajaStyleErrorMessageOnly() throws Exception {
        var root = mapper.readTree("""
                {"errorCode":"400.002.02","errorMessage":"Request Header Invalid"}
                """);
        ApiException ex = assertThrows(ApiException.class,
                () -> SafaricomApiErrorMapper.assertPurchaseSuccess(root));
        assertTrue(ex.getMessage().toLowerCase().contains("header"));
    }

    @Test
    void mapsCancelledPayment() {
        ApiException ex = SafaricomApiErrorMapper.fromBody(
                "status", 400, "Request cancelled by user", "400");
        assertTrue(ex.getMessage().toLowerCase().contains("cancelled"));
    }

    @Test
    void mapsDuplicateInProgress() {
        ApiException ex = SafaricomApiErrorMapper.fromBody(
                "purchase", 400, "The transaction is being processed by another instance", "400");
        assertTrue(ex.getMessage().toLowerCase().contains("already in progress"));
    }

    @Test
    void recognizesSuccessCodes() {
        assertTrue(SafaricomApiErrorMapper.isSuccess("200"));
        assertTrue(SafaricomApiErrorMapper.isSuccess("1000"));
        assertTrue(SafaricomApiErrorMapper.isSuccess("0"));
        assertTrue(SafaricomApiErrorMapper.isSuccess("00"));
        assertTrue(SafaricomApiErrorMapper.isSuccess("SUCCESS"));
    }

    @Test
    void assertPurchaseSuccessPassesOn200() throws Exception {
        var root = mapper.readTree("""
                {"header":{"responseCode":200,"customerMessage":"Bundle purchase was successful"}}
                """);
        SafaricomApiErrorMapper.assertPurchaseSuccess(root);
    }
}
