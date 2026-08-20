package com.novastack.sms.domain.repository;

import com.novastack.sms.domain.entity.PlatformAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformAnnouncementRepository extends JpaRepository<PlatformAnnouncement, Byte> {
}
