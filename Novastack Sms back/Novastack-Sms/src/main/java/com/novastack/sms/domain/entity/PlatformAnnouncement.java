package com.novastack.sms.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "platform_announcement")
@Getter
@Setter
@NoArgsConstructor
public class PlatformAnnouncement {

    public static final byte SINGLETON_ID = 1;

    @Id
    private Byte id = SINGLETON_ID;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(nullable = false, length = 40)
    private String label = "Announcement";

    @Column(nullable = false, length = 120)
    private String title = "Service Notice";

    @Column(nullable = false, length = 2000)
    private String body = "";

    @Column(nullable = false, length = 20)
    private String tone = "INFO";

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
