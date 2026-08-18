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
import com.novastack.sms.dto.request.ContactRequest;
import com.novastack.sms.exception.ApiException;
import com.novastack.sms.provider.TalkSasaContactClient;
import com.novastack.sms.provider.TalkSasaContactGroupClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceContactSyncTest {

    @Mock
    private ContactRepository contactRepository;
    @Mock
    private ContactGroupRepository contactGroupRepository;
    @Mock
    private ContactProviderUidRepository contactProviderUidRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private TalkSasaContactGroupClient talkSasaContactGroupClient;
    @Mock
    private TalkSasaContactClient talkSasaContactClient;

    private ContactService contactService;
    private Organization organization;
    private UUID orgId;
    private UUID groupId;
    private ContactGroup group;

    @BeforeEach
    void setUp() {
        contactService = new ContactService(
                contactRepository,
                contactGroupRepository,
                contactProviderUidRepository,
                organizationRepository,
                talkSasaContactGroupClient,
                talkSasaContactClient);
        orgId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        organization = Organization.builder().id(orgId).name("Acme").build();
        group = ContactGroup.builder()
                .id(groupId)
                .organization(organization)
                .name("Customers")
                .providerGroupUid("6065ecdc9184a")
                .build();
    }

    @Test
    void createContactStoresTalkSasaUidWhenGrouped() {
        ContactRequest request = new ContactRequest();
        request.setPhone("0712345678");
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setGroupId(groupId);

        when(contactRepository.existsByOrganizationIdAndPhone(orgId, "254712345678")).thenReturn(false);
        when(organizationRepository.getReferenceById(orgId)).thenReturn(organization);
        when(contactGroupRepository.findByIdAndOrganizationId(groupId, orgId)).thenReturn(Optional.of(group));
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
            Contact contact = invocation.getArgument(0);
            if (contact.getId() == null) {
                contact.setId(UUID.randomUUID());
            }
            return contact;
        });
        when(talkSasaContactClient.isEnabled()).thenReturn(true);
        when(contactProviderUidRepository.findByContactIdAndGroupId(any(), eq(groupId))).thenReturn(Optional.empty());
        when(talkSasaContactClient.store("6065ecdc9184a", "254712345678", "Jane", "Doe"))
                .thenReturn(Optional.of(new TalkSasaContactClient.TalkSasaContact(
                        "606732aec8705", "254712345678", "Jane", "Doe")));

        contactService.createContact(orgId, request);

        ArgumentCaptor<ContactProviderUid> captor = ArgumentCaptor.forClass(ContactProviderUid.class);
        verify(contactProviderUidRepository).save(captor.capture());
        assertEquals("606732aec8705", captor.getValue().getProviderContactUid());
        assertEquals(groupId, captor.getValue().getGroupId());
    }

    @Test
    void createContactSucceedsWhenTalkSasaIsDown() {
        ContactRequest request = new ContactRequest();
        request.setPhone("0712345678");
        request.setGroupId(groupId);

        when(contactRepository.existsByOrganizationIdAndPhone(orgId, "254712345678")).thenReturn(false);
        when(organizationRepository.getReferenceById(orgId)).thenReturn(organization);
        when(contactGroupRepository.findByIdAndOrganizationId(groupId, orgId)).thenReturn(Optional.of(group));
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
            Contact contact = invocation.getArgument(0);
            contact.setId(UUID.randomUUID());
            return contact;
        });
        when(talkSasaContactClient.isEnabled()).thenReturn(true);
        when(contactProviderUidRepository.findByContactIdAndGroupId(any(), eq(groupId))).thenReturn(Optional.empty());
        when(talkSasaContactClient.store(eq("6065ecdc9184a"), eq("254712345678"), any(), any()))
                .thenReturn(Optional.empty());

        var response = contactService.createContact(orgId, request);

        assertEquals("254712345678", response.getPhone());
        verify(contactProviderUidRepository, never()).save(any());
    }

    @Test
    void addToGroupMirrorsStore() {
        UUID contactId = UUID.randomUUID();
        Contact contact = Contact.builder()
                .id(contactId)
                .phone("254712345678")
                .firstName("Jane")
                .groups(new HashSet<>())
                .build();
        AddContactsToGroupRequest request = new AddContactsToGroupRequest();
        request.setContactIds(List.of(contactId));

        when(contactGroupRepository.findByIdAndOrganizationId(groupId, orgId)).thenReturn(Optional.of(group));
        when(contactRepository.findByOrganizationIdAndIdIn(orgId, List.of(contactId))).thenReturn(List.of(contact));
        when(contactRepository.save(contact)).thenReturn(contact);
        when(contactRepository.countByOrganizationIdAndGroupsId(orgId, groupId)).thenReturn(1L);
        when(talkSasaContactClient.isEnabled()).thenReturn(true);
        when(contactProviderUidRepository.findByContactIdAndGroupId(contactId, groupId)).thenReturn(Optional.empty());
        when(talkSasaContactClient.store("6065ecdc9184a", "254712345678", "Jane", null))
                .thenReturn(Optional.of(new TalkSasaContactClient.TalkSasaContact(
                        "606732aec8705", "254712345678", "Jane", null)));

        contactService.addContactsToGroup(orgId, groupId, request);

        verify(talkSasaContactClient).store("6065ecdc9184a", "254712345678", "Jane", null);
        verify(contactProviderUidRepository).save(any(ContactProviderUid.class));
    }

    @Test
    void removeFromGroupMirrorsDelete() {
        UUID contactId = UUID.randomUUID();
        Contact contact = Contact.builder()
                .id(contactId)
                .phone("254712345678")
                .groups(new HashSet<>(List.of(group)))
                .build();
        ContactProviderUid mapping = ContactProviderUid.builder()
                .contactId(contactId)
                .groupId(groupId)
                .providerContactUid("606732aec8705")
                .build();

        when(contactGroupRepository.findByIdAndOrganizationId(groupId, orgId)).thenReturn(Optional.of(group));
        when(contactRepository.findByIdAndOrganizationIdWithGroups(contactId, orgId)).thenReturn(Optional.of(contact));
        when(contactRepository.save(contact)).thenReturn(contact);
        when(contactProviderUidRepository.findByContactIdAndGroupId(contactId, groupId)).thenReturn(Optional.of(mapping));
        when(talkSasaContactClient.isEnabled()).thenReturn(true);
        when(talkSasaContactClient.delete("6065ecdc9184a", "606732aec8705")).thenReturn(true);

        contactService.removeContactFromGroup(orgId, groupId, contactId);

        verify(talkSasaContactClient).delete("6065ecdc9184a", "606732aec8705");
        verify(contactProviderUidRepository).deleteByContactIdAndGroupId(contactId, groupId);
    }

    @Test
    void updateContactPatchesTalkSasaMemberships() {
        UUID contactId = UUID.randomUUID();
        Contact contact = Contact.builder()
                .id(contactId)
                .phone("254712345678")
                .firstName("Jane")
                .groups(new HashSet<>(List.of(group)))
                .build();
        ContactProviderUid mapping = ContactProviderUid.builder()
                .contactId(contactId)
                .groupId(groupId)
                .providerContactUid("606732aec8705")
                .build();
        ContactRequest request = new ContactRequest();
        request.setPhone("0722000111");
        request.setFirstName("Janet");

        when(contactRepository.findByIdAndOrganizationIdWithGroups(contactId, orgId)).thenReturn(Optional.of(contact));
        when(contactRepository.existsByOrganizationIdAndPhoneAndIdNot(orgId, "254722000111", contactId))
                .thenReturn(false);
        when(contactRepository.save(contact)).thenReturn(contact);
        when(talkSasaContactClient.isEnabled()).thenReturn(true);
        when(contactProviderUidRepository.findByContactIdAndGroupId(contactId, groupId)).thenReturn(Optional.of(mapping));
        when(talkSasaContactClient.update("6065ecdc9184a", "606732aec8705", "254722000111", "Janet", null))
                .thenReturn(Optional.of(new TalkSasaContactClient.TalkSasaContact(
                        "606732aec8705", "254722000111", "Janet", null)));

        var response = contactService.updateContact(orgId, contactId, request);

        assertEquals("254722000111", response.getPhone());
        verify(talkSasaContactClient).update("6065ecdc9184a", "606732aec8705", "254722000111", "Janet", null);
    }

    @Test
    void updateContactIsTenantScoped() {
        UUID contactId = UUID.randomUUID();
        ContactRequest request = new ContactRequest();
        request.setPhone("0712345678");
        when(contactRepository.findByIdAndOrganizationIdWithGroups(contactId, orgId)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> contactService.updateContact(orgId, contactId, request));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(talkSasaContactClient, never()).update(any(), any(), any(), any(), any());
    }

    @Test
    void deleteContactMirrorsTalkSasaThenDeletesNova() {
        UUID contactId = UUID.randomUUID();
        Contact contact = Contact.builder()
                .id(contactId)
                .phone("254712345678")
                .groups(new HashSet<>(List.of(group)))
                .build();
        ContactProviderUid mapping = ContactProviderUid.builder()
                .contactId(contactId)
                .groupId(groupId)
                .providerContactUid("606732aec8705")
                .build();

        when(contactRepository.findByIdAndOrganizationIdWithGroups(contactId, orgId)).thenReturn(Optional.of(contact));
        when(contactProviderUidRepository.findByContactId(contactId)).thenReturn(List.of(mapping));
        when(talkSasaContactClient.isEnabled()).thenReturn(true);
        when(talkSasaContactClient.delete("6065ecdc9184a", "606732aec8705")).thenReturn(true);

        contactService.deleteContact(orgId, contactId);

        verify(talkSasaContactClient).delete("6065ecdc9184a", "606732aec8705");
        verify(contactRepository).delete(contact);
    }
}
