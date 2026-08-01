package com.novastack.sms.databundle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novastack.sms.config.AppProperties;
import com.novastack.sms.dto.response.DataBundleOfferResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SafaricomDynamicOffersParseTest {

    @Mock
    private SafaricomAuthService authService;
    @Mock
    private WebClient.Builder webClientBuilder;

    private SafaricomDynamicOffersClient client;

    @BeforeEach
    void setUp() {
        client = new SafaricomDynamicOffersClient(
                new AppProperties(),
                authService,
                webClientBuilder,
                new ObjectMapper());
    }

    @Test
    void parsesOfficialSafaricomFetchSample() throws Exception {
        String sample = """
                {
                  "id": "mock-response-id",
                  "desc": "Mock offers retrieved successfully",
                  "status": "200",
                  "relatedSusbscription": [
                    {
                      "desc": "254708374149",
                      "name": "mock-msisdn"
                    }
                  ],
                  "lineItem": {
                    "characteristicsValue": [
                      {
                        "offerName": "Weekly 2GB",
                        "uniqueOfferingId": "50512026",
                        "offerValidity": 7,
                        "resourceAccId": 1001,
                        "resourceValue": 2048,
                        "offerPrice": 99,
                        "offerUssdName": "Weekly 2GB",
                        "offeringId": 20001,
                        "offerSource": "MOCK",
                        "locationId": 1,
                        "subscribed": 0,
                        "childOffers": [
                          {
                            "offerName": "Daily Booster 500MB",
                            "offerValidity": 1,
                            "resourceAccId": 1101,
                            "resourceValue": 500,
                            "offerPrice": 20,
                            "offerUssdName": "*544*20#",
                            "offeringId": 110199,
                            "parentOfferId": 1001
                          }
                        ]
                      }
                    ]
                  }
                }
                """;

        List<DataBundleOfferResponse> offers = client.parseOffersResponse(sample);
        assertEquals(2, offers.size());

        DataBundleOfferResponse weekly = offers.get(0);
        assertEquals("20001", weekly.getOfferId());
        assertEquals("50512026", weekly.getUniqueOfferingId());
        assertEquals("Weekly 2GB", weekly.getOfferName());
        assertEquals(new BigDecimal("99"), weekly.getAmount());
        assertEquals("1001", weekly.getAccountId());
        assertEquals("2048", weekly.getResourceAmount());
        assertEquals("7 Days", weekly.getValidity());
        assertEquals("WEEKLY", weekly.getCategory());

        DataBundleOfferResponse child = offers.get(1);
        assertEquals("110199", child.getOfferId());
        assertEquals("Daily Booster 500MB", child.getOfferName());
        assertEquals(new BigDecimal("20"), child.getAmount());
        assertEquals("1101", child.getAccountId());
        assertEquals("500", child.getResourceAmount());
        assertEquals("1 Day", child.getValidity());
        assertTrue(child.getCategory().equals("DAILY") || child.getCategory().equals("OTHER"));
    }

    @Test
    void purchaseMsisdnUsesInternationalFormat() {
        assertEquals("254708374149", SafaricomDynamicOffersClient.toPurchaseMsisdn("254708374149"));
        assertEquals("254795898572", SafaricomDynamicOffersClient.toPurchaseMsisdn("795898572"));
        assertEquals("254708374149", SafaricomDynamicOffersClient.toPurchaseMsisdn("0708374149"));
    }
}
