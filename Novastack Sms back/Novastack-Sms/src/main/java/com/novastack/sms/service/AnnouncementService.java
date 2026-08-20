package com.novastack.sms.service;

import com.novastack.sms.domain.entity.PlatformAnnouncement;
import com.novastack.sms.domain.repository.PlatformAnnouncementRepository;
import com.novastack.sms.dto.request.UpdateAnnouncementRequest;
import com.novastack.sms.dto.response.AnnouncementResponse;
import com.novastack.sms.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private static final Set<String> TONES = Set.of("INFO", "WARNING", "DANGER");

    private final PlatformAnnouncementRepository repository;

    @Transactional
    public AnnouncementResponse current() {
        return toResponse(entity());
    }

    @Transactional
    public AnnouncementResponse publicCurrent() {
        PlatformAnnouncement announcement = entity();
        if (!announcement.isEnabled() || blank(announcement.getBody())) {
            return AnnouncementResponse.builder()
                    .enabled(false)
                    .label(announcement.getLabel())
                    .title(announcement.getTitle())
                    .body("")
                    .tone(readTone(announcement.getTone()))
                    .updatedAt(announcement.getUpdatedAt())
                    .build();
        }
        return toResponse(announcement);
    }

    @Transactional
    public AnnouncementResponse update(UpdateAnnouncementRequest request) {
        if (request == null) {
            throw new ApiException("Announcement is required", HttpStatus.BAD_REQUEST);
        }
        PlatformAnnouncement announcement = entity();
        announcement.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        announcement.setLabel(request.getLabel().trim());
        announcement.setTitle(request.getTitle().trim());
        announcement.setBody(request.getBody().trim());
        announcement.setTone(requireTone(request.getTone()));
        return toResponse(repository.save(announcement));
    }

    private PlatformAnnouncement entity() {
        return repository.findById(PlatformAnnouncement.SINGLETON_ID).orElseGet(this::seed);
    }

    private PlatformAnnouncement seed() {
        PlatformAnnouncement announcement = new PlatformAnnouncement();
        announcement.setId(PlatformAnnouncement.SINGLETON_ID);
        announcement.setEnabled(false);
        announcement.setLabel("Announcement");
        announcement.setTitle("Service Notice");
        announcement.setBody("");
        announcement.setTone("INFO");
        return repository.save(announcement);
    }

    private static AnnouncementResponse toResponse(PlatformAnnouncement announcement) {
        return AnnouncementResponse.builder()
                .enabled(announcement.isEnabled())
                .label(announcement.getLabel())
                .title(announcement.getTitle())
                .body(announcement.getBody())
                .tone(readTone(announcement.getTone()))
                .updatedAt(announcement.getUpdatedAt())
                .build();
    }

    private static String requireTone(String tone) {
        if (tone == null || tone.isBlank()) {
            return "INFO";
        }
        String value = tone.trim().toUpperCase();
        if (!TONES.contains(value)) {
            throw new ApiException("Tone must be INFO, WARNING, or DANGER", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private static String readTone(String tone) {
        if (tone == null || tone.isBlank()) {
            return "INFO";
        }
        String value = tone.trim().toUpperCase();
        return TONES.contains(value) ? value : "INFO";
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
