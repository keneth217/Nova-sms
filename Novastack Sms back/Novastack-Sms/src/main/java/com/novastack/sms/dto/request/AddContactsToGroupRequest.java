package com.novastack.sms.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AddContactsToGroupRequest {

    @NotEmpty
    private List<UUID> contactIds;
}
