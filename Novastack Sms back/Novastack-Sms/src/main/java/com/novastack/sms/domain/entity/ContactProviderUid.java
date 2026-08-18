package com.novastack.sms.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contact_provider_uids")
@IdClass(ContactProviderUidId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactProviderUid {

    @Id
    @Column(name = "contact_id", nullable = false)
    private UUID contactId;

    @Id
    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "provider_contact_uid", nullable = false, length = 64, unique = true)
    private String providerContactUid;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
