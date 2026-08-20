package com.novastack.sms.service;

import com.novastack.sms.domain.entity.PlatformAnnouncement;
import com.novastack.sms.domain.repository.PlatformAnnouncementRepository;
import com.novastack.sms.dto.request.UpdateAnnouncementRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private PlatformAnnouncementRepository repository;

    private AnnouncementService service;
    private PlatformAnnouncement stored;

    @BeforeEach
    void setUp() {
        service = new AnnouncementService(repository);
        stored = new PlatformAnnouncement();
        stored.setId(PlatformAnnouncement.SINGLETON_ID);
        stored.setEnabled(false);
        stored.setLabel("Announcement");
        stored.setTitle("Service Notice");
        stored.setBody("Network delay on Safaricom.");
        stored.setTone("INFO");
        when(repository.findById(PlatformAnnouncement.SINGLETON_ID)).thenReturn(Optional.of(stored));
        when(repository.save(any(PlatformAnnouncement.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void publicCurrentHidesDisabledBanner() {
        var response = service.publicCurrent();
        assertFalse(response.isEnabled());
        assertEquals("", response.getBody());
    }

    @Test
    void updateEnablesBannerForDashboard() {
        UpdateAnnouncementRequest request = new UpdateAnnouncementRequest();
        request.setEnabled(true);
        request.setLabel("Announcement");
        request.setTitle("Service Notice");
        request.setBody("Safaricom delay.");
        request.setTone("WARNING");

        var saved = service.update(request);

        assertTrue(saved.isEnabled());
        assertEquals("WARNING", saved.getTone());
        assertEquals("Safaricom delay.", saved.getBody());
        assertTrue(service.publicCurrent().isEnabled());
    }
}
