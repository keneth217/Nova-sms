package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.ContactProviderUid;
import com.novastack.sms.domain.entity.ContactProviderUidId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContactProviderUidRepository extends JpaRepository<ContactProviderUid, ContactProviderUidId> {

    Optional<ContactProviderUid> findByContactIdAndGroupId(UUID contactId, UUID groupId);

    List<ContactProviderUid> findByContactId(UUID contactId);

    @Modifying
    void deleteByContactIdAndGroupId(UUID contactId, UUID groupId);

    @Modifying
    void deleteByGroupId(UUID groupId);
}
