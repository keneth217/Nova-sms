package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DataBundleOffersResponse {
    private boolean success;
    private String phoneNumber;
    private List<DataBundleOfferResponse> offers;
}
