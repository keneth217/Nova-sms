package com.novastack.sms.controller;

import com.novastack.sms.dto.response.AnnouncementResponse;
import com.novastack.sms.dto.response.ApiResponse;
import com.novastack.sms.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/announcement")
@RequiredArgsConstructor
@Tag(name = "Announcement")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    @Operation(summary = "Active dashboard announcement for signed-in users")
    public ApiResponse<AnnouncementResponse> current() {
        return ApiResponse.ok(announcementService.publicCurrent());
    }
}
