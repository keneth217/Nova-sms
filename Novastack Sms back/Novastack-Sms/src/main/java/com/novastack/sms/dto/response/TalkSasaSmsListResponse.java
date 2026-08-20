package com.novastack.sms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TalkSasaSmsListResponse {

    private boolean configured;
    private boolean reachable;
    private String errorMessage;
    private Integer page;
    private Integer perPage;
    private Long total;
    private Integer lastPage;
    private List<TalkSasaSmsItemResponse> items;
}
