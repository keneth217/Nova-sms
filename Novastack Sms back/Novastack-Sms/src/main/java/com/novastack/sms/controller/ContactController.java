package com.novastack.sms.controller;

import com.novastack.sms.dto.request.AddContactsToGroupRequest;
import com.novastack.sms.dto.request.BulkContactImportRequest;
import com.novastack.sms.dto.request.ContactGroupRequest;
import com.novastack.sms.dto.request.ContactRequest;
import com.novastack.sms.dto.response.ApiResponse;
import com.novastack.sms.dto.response.ContactGroupResponse;
import com.novastack.sms.dto.response.ContactResponse;
import com.novastack.sms.security.SecurityUtils;
import com.novastack.sms.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
@Tag(name = "Contacts & Groups")
public class ContactController {

    private final ContactService contactService;

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a contact group for this organization")
    public ApiResponse<ContactGroupResponse> createGroup(@Valid @RequestBody ContactGroupRequest request) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(contactService.createGroup(orgId, request));
    }

    @GetMapping("/groups")
    @Operation(summary = "List organization contact groups")
    public ApiResponse<List<ContactGroupResponse>> listGroups() {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(contactService.listGroups(orgId));
    }

    @GetMapping("/groups/{groupId}")
    @Operation(summary = "Get group details")
    public ApiResponse<ContactGroupResponse> getGroup(@PathVariable UUID groupId) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(contactService.getGroup(orgId, groupId));
    }

    @PostMapping("/groups/{groupId}/members")
    @Operation(summary = "Add existing contacts to a group")
    public ApiResponse<Map<String, Object>> addToGroup(
            @PathVariable UUID groupId,
            @Valid @RequestBody AddContactsToGroupRequest request) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(contactService.addContactsToGroup(orgId, groupId, request));
    }

    @DeleteMapping("/groups/{groupId}/members/{contactId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a contact from a group")
    public void removeFromGroup(@PathVariable UUID groupId, @PathVariable UUID contactId) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        contactService.removeContactFromGroup(orgId, groupId, contactId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a contact (optionally assign to a group)")
    public ApiResponse<ContactResponse> create(@Valid @RequestBody ContactRequest request) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(contactService.createContact(orgId, request));
    }

    @PostMapping("/import")
    @Operation(summary = "Bulk import contacts from JSON")
    public ApiResponse<Map<String, Object>> importContacts(@Valid @RequestBody BulkContactImportRequest request) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(contactService.importContacts(orgId, request));
    }

    @PostMapping(value = "/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import contacts from Excel (.xlsx/.xls). Optional groupId assigns all rows to that group.")
    public ApiResponse<Map<String, Object>> importExcel(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) UUID groupId) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(contactService.importContactsFromExcel(orgId, file, groupId));
    }

    @GetMapping("/import/excel/template")
    @Operation(summary = "Download Excel import template")
    public ResponseEntity<ByteArrayResource> downloadTemplate() {
        byte[] bytes = contactService.buildExcelTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=contacts-import-template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }

    @GetMapping
    @Operation(summary = "List contacts. Pass groupId to list only members of that group.")
    public ApiResponse<Page<ContactResponse>> list(
            @RequestParam(required = false) UUID groupId,
            @PageableDefault(size = 50) Pageable pageable) {
        UUID orgId = SecurityUtils.requireOrganizationId();
        return ApiResponse.ok(contactService.listContacts(orgId, groupId, pageable));
    }
}
