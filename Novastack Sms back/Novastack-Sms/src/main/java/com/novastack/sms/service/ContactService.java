package com.novastack.sms.service;

import com.novastack.sms.domain.entity.Contact;
import com.novastack.sms.domain.entity.ContactGroup;
import com.novastack.sms.domain.entity.ContactProviderUid;
import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.domain.repository.ContactGroupRepository;
import com.novastack.sms.domain.repository.ContactProviderUidRepository;
import com.novastack.sms.domain.repository.ContactRepository;
import com.novastack.sms.domain.repository.OrganizationRepository;
import com.novastack.sms.dto.request.AddContactsToGroupRequest;
import com.novastack.sms.dto.request.BulkContactImportRequest;
import com.novastack.sms.dto.request.ContactGroupRequest;
import com.novastack.sms.dto.request.ContactRequest;
import com.novastack.sms.dto.response.ContactGroupResponse;
import com.novastack.sms.dto.response.ContactResponse;
import com.novastack.sms.exception.ApiException;
import com.novastack.sms.provider.TalkSasaContactClient;
import com.novastack.sms.provider.TalkSasaContactGroupClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    private final ContactRepository contactRepository;
    private final ContactGroupRepository contactGroupRepository;
    private final ContactProviderUidRepository contactProviderUidRepository;
    private final OrganizationRepository organizationRepository;
    private final TalkSasaContactGroupClient talkSasaContactGroupClient;
    private final TalkSasaContactClient talkSasaContactClient;
    private final DataFormatter dataFormatter = new DataFormatter();

    @Transactional
    public ContactGroupResponse createGroup(UUID organizationId, ContactGroupRequest request) {
        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty()) {
            throw new ApiException("Group name is required", HttpStatus.BAD_REQUEST);
        }
        if (contactGroupRepository.existsByOrganizationIdAndNameIgnoreCase(organizationId, name)) {
            throw new ApiException("Group name already exists", HttpStatus.CONFLICT);
        }
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException("Organization not found", HttpStatus.NOT_FOUND));
        ContactGroup group = contactGroupRepository.save(ContactGroup.builder()
                .organization(org)
                .name(name)
                .description(blankToNull(request.getDescription()))
                .build());
        syncCreate(org, group);
        return toGroupResponse(group, 0);
    }

    @Transactional
    public ContactGroupResponse updateGroup(UUID organizationId, UUID groupId, ContactGroupRequest request) {
        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty()) {
            throw new ApiException("Group name is required", HttpStatus.BAD_REQUEST);
        }
        ContactGroup group = requireGroup(organizationId, groupId);
        if (contactGroupRepository.existsByOrganizationIdAndNameIgnoreCaseAndIdNot(organizationId, name, groupId)) {
            throw new ApiException("Group name already exists", HttpStatus.CONFLICT);
        }
        group.setName(name);
        if (request.getDescription() != null) {
            group.setDescription(blankToNull(request.getDescription()));
        }
        contactGroupRepository.save(group);
        syncUpdate(group);
        return toGroupResponse(
                group,
                contactRepository.countByOrganizationIdAndGroupsId(organizationId, groupId));
    }

    @Transactional
    public void deleteGroup(UUID organizationId, UUID groupId) {
        ContactGroup group = requireGroup(organizationId, groupId);
        String providerUid = group.getProviderGroupUid();
        List<Contact> members = contactRepository.findByOrganizationIdAndGroupsId(organizationId, groupId);
        for (Contact contact : members) {
            contact.getGroups().remove(group);
            contactRepository.save(contact);
        }
        contactProviderUidRepository.deleteByGroupId(groupId);
        contactGroupRepository.delete(group);
        syncDelete(providerUid);
    }

    @Transactional(readOnly = true)
    public List<ContactGroupResponse> listGroups(UUID organizationId) {
        return contactGroupRepository.findByOrganizationId(organizationId).stream()
                .map(group -> toGroupResponse(
                        group,
                        contactRepository.countByOrganizationIdAndGroupsId(organizationId, group.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ContactGroupResponse getGroup(UUID organizationId, UUID groupId) {
        ContactGroup group = requireGroup(organizationId, groupId);
        return toGroupResponse(
                group,
                contactRepository.countByOrganizationIdAndGroupsId(organizationId, groupId));
    }

    @Transactional
    public ContactResponse createContact(UUID organizationId, ContactRequest request) {
        String phone = normalize(request.getPhone());
        if (contactRepository.existsByOrganizationIdAndPhone(organizationId, phone)) {
            throw new ApiException("Contact already exists", HttpStatus.CONFLICT);
        }

        Contact contact = Contact.builder()
                .organization(organizationRepository.getReferenceById(organizationId))
                .phone(phone)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .build();

        ContactGroup group = request.getGroupId() != null
                ? requireGroup(organizationId, request.getGroupId())
                : null;
        if (group != null) {
            contact.getGroups().add(group);
        }

        Contact saved = contactRepository.save(contact);
        if (group != null) {
            syncStore(saved, group);
        }
        return toContactResponse(saved);
    }

    @Transactional
    public ContactResponse updateContact(UUID organizationId, UUID contactId, ContactRequest request) {
        Contact contact = contactRepository.findByIdAndOrganizationIdWithGroups(contactId, organizationId)
                .orElseThrow(() -> new ApiException("Contact not found", HttpStatus.NOT_FOUND));
        String phone = normalize(request.getPhone());
        if (contactRepository.existsByOrganizationIdAndPhoneAndIdNot(organizationId, phone, contactId)) {
            throw new ApiException("Contact already exists", HttpStatus.CONFLICT);
        }
        contact.setPhone(phone);
        contact.setFirstName(blankToNull(request.getFirstName()));
        contact.setLastName(blankToNull(request.getLastName()));
        if (request.getEmail() != null) {
            contact.setEmail(blankToNull(request.getEmail()));
        }
        Contact saved = contactRepository.save(contact);
        syncUpdateContact(saved);
        return toContactResponse(saved);
    }

    @Transactional
    public void deleteContact(UUID organizationId, UUID contactId) {
        Contact contact = contactRepository.findByIdAndOrganizationIdWithGroups(contactId, organizationId)
                .orElseThrow(() -> new ApiException("Contact not found", HttpStatus.NOT_FOUND));
        List<ContactProviderUid> mappings = contactProviderUidRepository.findByContactId(contact.getId());
        for (ContactProviderUid mapping : mappings) {
            ContactGroup group = contact.getGroups().stream()
                    .filter(item -> item.getId().equals(mapping.getGroupId()))
                    .findFirst()
                    .orElse(null);
            String groupUid = group != null ? group.getProviderGroupUid() : null;
            syncDeleteMember(groupUid, mapping.getProviderContactUid());
        }
        contact.getGroups().clear();
        contactRepository.delete(contact);
    }

    @Transactional
    public Map<String, Object> importContacts(UUID organizationId, BulkContactImportRequest request) {
        ContactGroup group = request.getGroupId() != null
                ? requireGroup(organizationId, request.getGroupId())
                : null;

        int created = 0;
        int skipped = 0;
        for (ContactRequest item : request.getContacts()) {
            ImportOutcome outcome = upsertContact(
                    organizationId,
                    item.getPhone(),
                    item.getFirstName(),
                    item.getLastName(),
                    item.getEmail(),
                    item.getGroupId() != null ? item.getGroupId() : (group != null ? group.getId() : null));
            if (outcome == ImportOutcome.CREATED || outcome == ImportOutcome.UPDATED) {
                created++;
            } else {
                skipped++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        return result;
    }

    @Transactional
    public Map<String, Object> importContactsFromExcel(UUID organizationId, MultipartFile file, UUID groupId) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Excel file is required", HttpStatus.BAD_REQUEST);
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
            throw new ApiException("Only Excel files (.xlsx, .xls) are supported", HttpStatus.BAD_REQUEST);
        }

        if (groupId != null) {
            requireGroup(organizationId, groupId);
        }

        int created = 0;
        int skipped = 0;
        int invalid = 0;
        List<String> errors = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new ApiException("Excel file has no sheets", HttpStatus.BAD_REQUEST);
            }

            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                throw new ApiException("Excel file is empty", HttpStatus.BAD_REQUEST);
            }

            Map<String, Integer> columns = mapHeaderColumns(header);
            if (!columns.containsKey("phone")) {
                throw new ApiException(
                        "Excel must include a 'phone' column. Optional: firstName, lastName, email",
                        HttpStatus.BAD_REQUEST);
            }

            int firstDataRow = sheet.getFirstRowNum() + 1;
            int lastRow = sheet.getLastRowNum();
            for (int i = firstDataRow; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row, columns)) {
                    continue;
                }

                String phone = cellValue(row, columns.get("phone"));
                String firstName = cellValue(row, columns.get("firstname"));
                String lastName = cellValue(row, columns.get("lastname"));
                String email = cellValue(row, columns.get("email"));

                if (phone == null || phone.isBlank()) {
                    invalid++;
                    errors.add("Row " + (i + 1) + ": phone is required");
                    continue;
                }

                try {
                    ImportOutcome outcome = upsertContact(organizationId, phone, firstName, lastName, email, groupId);
                    if (outcome == ImportOutcome.CREATED || outcome == ImportOutcome.UPDATED) {
                        created++;
                    } else {
                        skipped++;
                    }
                } catch (ApiException ex) {
                    invalid++;
                    errors.add("Row " + (i + 1) + ": " + ex.getMessage());
                }
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException("Failed to read Excel file: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("invalid", invalid);
        result.put("errors", errors.size() > 20 ? errors.subList(0, 20) : errors);
        return result;
    }

    public byte[] buildExcelTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Contacts");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("phone");
            header.createCell(1).setCellValue("firstName");
            header.createCell(2).setCellValue("lastName");
            header.createCell(3).setCellValue("email");

            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("254712345678");
            sample.createCell(1).setCellValue("Jane");
            sample.createCell(2).setCellValue("Doe");
            sample.createCell(3).setCellValue("jane@example.com");

            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new ApiException("Failed to generate Excel template", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public Map<String, Object> addContactsToGroup(UUID organizationId, UUID groupId, AddContactsToGroupRequest request) {
        ContactGroup group = requireGroup(organizationId, groupId);
        List<Contact> contacts = contactRepository.findByOrganizationIdAndIdIn(organizationId, request.getContactIds());
        if (contacts.size() != request.getContactIds().size()) {
            throw new ApiException("One or more contacts were not found in this organization", HttpStatus.NOT_FOUND);
        }

        int added = 0;
        for (Contact contact : contacts) {
            boolean newlyAdded = contact.getGroups().add(group);
            if (newlyAdded) {
                added++;
            }
            contactRepository.save(contact);
            if (newlyAdded
                    || contactProviderUidRepository.findByContactIdAndGroupId(contact.getId(), group.getId()).isEmpty()) {
                syncStore(contact, group);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("groupId", groupId);
        result.put("added", added);
        result.put("totalInGroup", contactRepository.countByOrganizationIdAndGroupsId(organizationId, groupId));
        return result;
    }

    @Transactional
    public void removeContactFromGroup(UUID organizationId, UUID groupId, UUID contactId) {
        ContactGroup group = requireGroup(organizationId, groupId);
        Contact contact = contactRepository.findByIdAndOrganizationIdWithGroups(contactId, organizationId)
                .orElseThrow(() -> new ApiException("Contact not found", HttpStatus.NOT_FOUND));
        if (!contact.getGroups().remove(group)) {
            throw new ApiException("Contact is not in this group", HttpStatus.BAD_REQUEST);
        }
        contactRepository.save(contact);
        syncDeleteMembership(contact.getId(), group);
    }

    @Transactional(readOnly = true)
    public Page<ContactResponse> listContacts(UUID organizationId, UUID groupId, Pageable pageable) {
        Page<Contact> page = groupId != null
                ? contactRepository.findByOrganizationIdAndGroupsId(organizationId, groupId, pageable)
                : contactRepository.findByOrganizationId(organizationId, pageable);
        return page.map(this::toContactResponse);
    }

    private ImportOutcome upsertContact(
            UUID organizationId,
            String rawPhone,
            String firstName,
            String lastName,
            String email,
            UUID groupId) {

        String phone = normalize(rawPhone);
        if (phone.length() < 9 || phone.length() > 15 || !phone.matches("\\d+")) {
            throw new ApiException("Invalid phone number: " + rawPhone, HttpStatus.BAD_REQUEST);
        }

        ContactGroup group = groupId != null ? requireGroup(organizationId, groupId) : null;
        var existing = contactRepository.findByOrganizationIdAndPhone(organizationId, phone);

        if (existing.isPresent()) {
            Contact contact = existing.get();
            if (group != null && contact.getGroups().add(group)) {
                contactRepository.save(contact);
                syncStore(contact, group);
                return ImportOutcome.UPDATED;
            }
            return ImportOutcome.SKIPPED;
        }

        Contact contact = Contact.builder()
                .organization(organizationRepository.getReferenceById(organizationId))
                .phone(phone)
                .firstName(blankToNull(firstName))
                .lastName(blankToNull(lastName))
                .email(blankToNull(email))
                .build();
        if (group != null) {
            contact.getGroups().add(group);
        }
        Contact saved = contactRepository.save(contact);
        if (group != null) {
            syncStore(saved, group);
        }
        return ImportOutcome.CREATED;
    }

    private ContactGroup requireGroup(UUID organizationId, UUID groupId) {
        return contactGroupRepository.findByIdAndOrganizationId(groupId, organizationId)
                .orElseThrow(() -> new ApiException("Group not found", HttpStatus.NOT_FOUND));
    }

    private void syncCreate(Organization org, ContactGroup group) {
        try {
            if (!talkSasaContactGroupClient.isEnabled()) {
                return;
            }
            talkSasaContactGroupClient.create(providerGroupName(org, group.getName()))
                    .ifPresentOrElse(
                            remote -> {
                                group.setProviderGroupUid(remote.uid());
                                contactGroupRepository.save(group);
                                log.info("Synced contact group {} to TalkSasa uidSuffix={}",
                                        group.getId(), suffix(remote.uid()));
                            },
                            () -> log.warn("TalkSasa contact group create did not return a uid for {}", group.getId()));
        } catch (Exception ex) {
            log.warn("TalkSasa contact group create sync failed for {}: {}", group.getId(), ex.getMessage());
        }
    }

    private void syncUpdate(ContactGroup group) {
        try {
            if (!talkSasaContactGroupClient.isEnabled()) {
                return;
            }
            String remoteName = providerGroupName(group.getOrganization(), group.getName());
            if (group.getProviderGroupUid() == null || group.getProviderGroupUid().isBlank()) {
                syncCreate(group.getOrganization(), group);
                return;
            }
            talkSasaContactGroupClient.update(group.getProviderGroupUid(), remoteName)
                    .ifPresentOrElse(
                            remote -> {
                                if (remote.uid() != null && !remote.uid().equals(group.getProviderGroupUid())) {
                                    group.setProviderGroupUid(remote.uid());
                                    contactGroupRepository.save(group);
                                }
                            },
                            () -> log.warn("TalkSasa contact group update failed for {}", group.getId()));
        } catch (Exception ex) {
            log.warn("TalkSasa contact group update sync failed for {}: {}", group.getId(), ex.getMessage());
        }
    }

    private void syncDelete(String providerUid) {
        try {
            if (providerUid == null || providerUid.isBlank() || !talkSasaContactGroupClient.isEnabled()) {
                return;
            }
            if (!talkSasaContactGroupClient.delete(providerUid)) {
                log.warn("TalkSasa contact group delete failed uidSuffix={}", suffix(providerUid));
            }
        } catch (Exception ex) {
            log.warn("TalkSasa contact group delete sync failed uidSuffix={}: {}", suffix(providerUid), ex.getMessage());
        }
    }

    private void syncStore(Contact contact, ContactGroup group) {
        try {
            if (!talkSasaContactClient.isEnabled() || contact == null || group == null) {
                return;
            }
            if (contactProviderUidRepository.findByContactIdAndGroupId(contact.getId(), group.getId()).isPresent()) {
                return;
            }
            ensureProviderGroup(group);
            String groupUid = group.getProviderGroupUid();
            if (groupUid == null || groupUid.isBlank()) {
                return;
            }
            talkSasaContactClient.store(groupUid, contact.getPhone(), contact.getFirstName(), contact.getLastName())
                    .ifPresentOrElse(
                            remote -> {
                                contactProviderUidRepository.save(ContactProviderUid.builder()
                                        .contactId(contact.getId())
                                        .groupId(group.getId())
                                        .providerContactUid(remote.uid())
                                        .build());
                                log.info("Synced contact {} to TalkSasa group uidSuffix={} contactUidSuffix={}",
                                        contact.getId(), suffix(groupUid), suffix(remote.uid()));
                            },
                            () -> log.warn("TalkSasa contact store did not return a uid for {} in group {}",
                                    contact.getId(), group.getId()));
        } catch (Exception ex) {
            log.warn("TalkSasa contact store sync failed for {}: {}", contact.getId(), ex.getMessage());
        }
    }

    private void syncUpdateContact(Contact contact) {
        try {
            if (!talkSasaContactClient.isEnabled() || contact == null) {
                return;
            }
            for (ContactGroup group : contact.getGroups()) {
                var mapping = contactProviderUidRepository.findByContactIdAndGroupId(contact.getId(), group.getId());
                if (mapping.isEmpty()) {
                    syncStore(contact, group);
                    continue;
                }
                ensureProviderGroup(group);
                String groupUid = group.getProviderGroupUid();
                if (groupUid == null || groupUid.isBlank()) {
                    continue;
                }
                talkSasaContactClient.update(
                                groupUid,
                                mapping.get().getProviderContactUid(),
                                contact.getPhone(),
                                contact.getFirstName(),
                                contact.getLastName())
                        .ifPresentOrElse(
                                remote -> {
                                    if (remote.uid() != null
                                            && !remote.uid().equals(mapping.get().getProviderContactUid())) {
                                        mapping.get().setProviderContactUid(remote.uid());
                                        contactProviderUidRepository.save(mapping.get());
                                    }
                                },
                                () -> log.warn("TalkSasa contact update failed for {} in group {}",
                                        contact.getId(), group.getId()));
            }
        } catch (Exception ex) {
            log.warn("TalkSasa contact update sync failed for {}: {}", contact.getId(), ex.getMessage());
        }
    }

    private void syncDeleteMembership(UUID contactId, ContactGroup group) {
        try {
            var mapping = contactProviderUidRepository.findByContactIdAndGroupId(contactId, group.getId());
            if (mapping.isEmpty()) {
                return;
            }
            syncDeleteMember(group.getProviderGroupUid(), mapping.get().getProviderContactUid());
            contactProviderUidRepository.deleteByContactIdAndGroupId(contactId, group.getId());
        } catch (Exception ex) {
            log.warn("TalkSasa contact membership delete sync failed for {}: {}", contactId, ex.getMessage());
        }
    }

    private void syncDeleteMember(String groupUid, String contactUid) {
        try {
            if (groupUid == null || groupUid.isBlank() || contactUid == null || contactUid.isBlank()
                    || !talkSasaContactClient.isEnabled()) {
                return;
            }
            if (!talkSasaContactClient.delete(groupUid, contactUid)) {
                log.warn("TalkSasa contact delete failed groupUidSuffix={} contactUidSuffix={}",
                        suffix(groupUid), suffix(contactUid));
            }
        } catch (Exception ex) {
            log.warn("TalkSasa contact delete sync failed contactUidSuffix={}: {}", suffix(contactUid), ex.getMessage());
        }
    }

    private void ensureProviderGroup(ContactGroup group) {
        if (group.getProviderGroupUid() != null && !group.getProviderGroupUid().isBlank()) {
            return;
        }
        syncCreate(group.getOrganization(), group);
    }

    private String providerGroupName(Organization org, String groupName) {
        String orgName = org != null && org.getName() != null && !org.getName().isBlank()
                ? org.getName().trim()
                : "Nova";
        String combined = orgName + " - " + groupName;
        return combined.length() <= 100 ? combined : combined.substring(0, 100);
    }

    private static String suffix(String value) {
        if (value == null || value.length() < 6) {
            return value;
        }
        return "..." + value.substring(value.length() - 6);
    }

    private Map<String, Integer> mapHeaderColumns(Row header) {
        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : header) {
            String raw = dataFormatter.formatCellValue(cell);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String key = raw.trim().toLowerCase(Locale.ROOT)
                    .replace(" ", "")
                    .replace("_", "");
            switch (key) {
                case "phone", "phonenumber", "mobile", "msisdn" -> columns.put("phone", cell.getColumnIndex());
                case "firstname", "first" -> columns.put("firstname", cell.getColumnIndex());
                case "lastname", "last" -> columns.put("lastname", cell.getColumnIndex());
                case "email", "mail" -> columns.put("email", cell.getColumnIndex());
                default -> {
                    // ignore unknown columns
                }
            }
        }
        return columns;
    }

    private boolean isBlankRow(Row row, Map<String, Integer> columns) {
        return columns.values().stream()
                .map(index -> cellValue(row, index))
                .allMatch(value -> value == null || value.isBlank());
    }

    private String cellValue(Row row, Integer index) {
        if (index == null || row == null) {
            return null;
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        String value = dataFormatter.formatCellValue(cell);
        return value != null ? value.trim() : null;
    }

    private String normalize(String phone) {
        String cleaned = phone.replaceAll("[\\s\\-()]", "");
        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.startsWith("0") && cleaned.length() == 10) {
            cleaned = "254" + cleaned.substring(1);
        }
        // Excel may store as 254712345678.0
        if (cleaned.contains(".")) {
            cleaned = cleaned.substring(0, cleaned.indexOf('.'));
        }
        return cleaned;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ContactGroupResponse toGroupResponse(ContactGroup group, long contactCount) {
        return ContactGroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .contactCount(contactCount)
                .createdAt(group.getCreatedAt())
                .build();
    }

    private ContactResponse toContactResponse(Contact contact) {
        return ContactResponse.builder()
                .id(contact.getId())
                .phone(contact.getPhone())
                .firstName(contact.getFirstName())
                .lastName(contact.getLastName())
                .email(contact.getEmail())
                .groupIds(contact.getGroups().stream().map(ContactGroup::getId).toList())
                .groupNames(contact.getGroups().stream().map(ContactGroup::getName).toList())
                .createdAt(contact.getCreatedAt())
                .build();
    }

    private enum ImportOutcome {
        CREATED,
        UPDATED,
        SKIPPED
    }
}
