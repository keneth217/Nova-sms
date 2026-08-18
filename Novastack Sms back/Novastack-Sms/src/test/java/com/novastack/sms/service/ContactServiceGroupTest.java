package com.novastack.sms.service;

import com.novastack.sms.domain.entity.Contact;
import com.novastack.sms.domain.entity.ContactGroup;
import com.novastack.sms.domain.entity.Organization;
import com.novastack.sms.domain.repository.ContactGroupRepository;
import com.novastack.sms.domain.repository.ContactProviderUidRepository;
import com.novastack.sms.domain.repository.ContactRepository;
import com.novastack.sms.domain.repository.OrganizationRepository;
import com.novastack.sms.dto.request.ContactGroupRequest;
import com.novastack.sms.dto.response.ContactGroupResponse;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceGroupTest {

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
        organization = Organization.builder().id(orgId).name("Acme").build();
    }

    @Test
    void createGroupStoresTalkSasaUidAndNamespacedName() {
        ContactGroupRequest request = new ContactGroupRequest();
        request.setName("Customers");
        when(contactGroupRepository.existsByOrganizationIdAndNameIgnoreCase(orgId, "Customers")).thenReturn(false);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(contactGroupRepository.save(any(ContactGroup.class))).thenAnswer(invocation -> {
            ContactGroup group = invocation.getArgument(0);
            if (group.getId() == null) {
                group.setId(UUID.randomUUID());
            }
            return group;
        });
        when(talkSasaContactGroupClient.isEnabled()).thenReturn(true);
        when(talkSasaContactGroupClient.create("Acme - Customers"))
                .thenReturn(Optional.of(new TalkSasaContactGroupClient.TalkSasaGroup("6065ecdc9184a", "Acme - Customers", null)));

        ContactGroupResponse response = contactService.createGroup(orgId, request);

        ArgumentCaptor<ContactGroup> captor = ArgumentCaptor.forClass(ContactGroup.class);
        verify(contactGroupRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        ContactGroup saved = captor.getAllValues().getLast();
        assertEquals("6065ecdc9184a", saved.getProviderGroupUid());
        assertEquals("Customers", response.getName());
    }

    @Test
    void createGroupSucceedsWhenTalkSasaIsDown() {
        ContactGroupRequest request = new ContactGroupRequest();
        request.setName("Customers");
        when(contactGroupRepository.existsByOrganizationIdAndNameIgnoreCase(orgId, "Customers")).thenReturn(false);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(contactGroupRepository.save(any(ContactGroup.class))).thenAnswer(invocation -> {
            ContactGroup group = invocation.getArgument(0);
            group.setId(UUID.randomUUID());
            return group;
        });
        when(talkSasaContactGroupClient.isEnabled()).thenReturn(true);
        when(talkSasaContactGroupClient.create(any())).thenReturn(Optional.empty());

        ContactGroupResponse response = contactService.createGroup(orgId, request);

        assertEquals("Customers", response.getName());
        verify(talkSasaContactGroupClient).create("Acme - Customers");
    }

    @Test
    void updateGroupIsTenantScoped() {
        UUID groupId = UUID.randomUUID();
        ContactGroupRequest request = new ContactGroupRequest();
        request.setName("Renamed");
        when(contactGroupRepository.findByIdAndOrganizationId(groupId, orgId)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> contactService.updateGroup(orgId, groupId, request));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(talkSasaContactGroupClient, never()).update(any(), any());
    }

    @Test
    void deleteGroupRemovesMembershipsThenMirrorsDelete() {
        UUID groupId = UUID.randomUUID();
        ContactGroup group = ContactGroup.builder()
                .id(groupId)
                .organization(organization)
                .name("Customers")
                .providerGroupUid("6065ecdc9184a")
                .build();
        Contact member = Contact.builder().id(UUID.randomUUID()).groups(new HashSet<>(List.of(group))).build();
        when(contactGroupRepository.findByIdAndOrganizationId(groupId, orgId)).thenReturn(Optional.of(group));
        when(contactRepository.findByOrganizationIdAndGroupsId(orgId, groupId)).thenReturn(List.of(member));
        when(talkSasaContactGroupClient.isEnabled()).thenReturn(true);
        when(talkSasaContactGroupClient.delete("6065ecdc9184a")).thenReturn(true);

        contactService.deleteGroup(orgId, groupId);

        assertFalse(member.getGroups().contains(group));
        verify(contactGroupRepository).delete(group);
        verify(talkSasaContactGroupClient).delete("6065ecdc9184a");
    }

    @Test
    void deleteGroupDoesNotFailWhenTalkSasaDeleteFails() {
        UUID groupId = UUID.randomUUID();
        ContactGroup group = ContactGroup.builder()
                .id(groupId)
                .organization(organization)
                .name("Customers")
                .providerGroupUid("6065ecdc9184a")
                .build();
        when(contactGroupRepository.findByIdAndOrganizationId(groupId, orgId)).thenReturn(Optional.of(group));
        when(contactRepository.findByOrganizationIdAndGroupsId(orgId, groupId)).thenReturn(List.of());
        when(talkSasaContactGroupClient.isEnabled()).thenReturn(true);
        when(talkSasaContactGroupClient.delete(eq("6065ecdc9184a"))).thenReturn(false);

        contactService.deleteGroup(orgId, groupId);

        verify(contactGroupRepository).delete(group);
    }
}
