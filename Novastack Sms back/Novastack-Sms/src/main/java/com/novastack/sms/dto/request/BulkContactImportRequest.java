package com.novastack.sms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BulkContactImportRequest {

    @NotEmpty
    @Valid
    private List<ContactRequest> contacts;

    private UUID groupId;
}
