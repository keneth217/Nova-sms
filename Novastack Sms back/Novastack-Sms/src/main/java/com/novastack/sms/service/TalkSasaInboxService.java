package com.novastack.sms.service;

import com.novastack.sms.domain.entity.SmsMessage;
import com.novastack.sms.domain.repository.SmsMessageRepository;
import com.novastack.sms.dto.response.SmsMessageResponse;
import com.novastack.sms.dto.response.TalkSasaSmsItemResponse;
import com.novastack.sms.dto.response.TalkSasaSmsListResponse;
import com.novastack.sms.dto.response.TalkSasaSmsViewResponse;
import com.novastack.sms.provider.TalkSasaSmsInboxClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TalkSasaInboxService {

    private final TalkSasaSmsInboxClient talkSasaSmsInboxClient;
    private final SmsMessageRepository smsMessageRepository;
    private final SmsService smsService;

    @Transactional(readOnly = true)
    public TalkSasaSmsListResponse list(int page, int perPage) {
        TalkSasaSmsListResponse response = talkSasaSmsInboxClient.list(page, perPage);
        attachNovaMessages(response.getItems());
        return response;
    }

    @Transactional(readOnly = true)
    public TalkSasaSmsViewResponse get(String uid) {
        TalkSasaSmsViewResponse response = talkSasaSmsInboxClient.get(uid);
        if (response.getItem() != null) {
            attachNovaMessages(List.of(response.getItem()));
        } else {
            SmsMessageResponse nova = findNova(uid);
            if (nova != null) {
                response.setItem(TalkSasaSmsItemResponse.builder()
                        .uid(uid == null ? null : uid.trim())
                        .novaMessage(nova)
                        .build());
            }
        }
        return response;
    }

    private void attachNovaMessages(List<TalkSasaSmsItemResponse> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<String> uids = items.stream()
                .map(TalkSasaSmsItemResponse::getUid)
                .filter(uid -> uid != null && !uid.isBlank())
                .distinct()
                .toList();
        if (uids.isEmpty()) {
            return;
        }
        Map<String, SmsMessage> byUid = new LinkedHashMap<>();
        for (SmsMessage message : smsMessageRepository.findByProviderMessageIdIn(uids)) {
            if (message.getProviderMessageId() != null) {
                byUid.putIfAbsent(message.getProviderMessageId(), message);
            }
        }
        for (TalkSasaSmsItemResponse item : items) {
            SmsMessage message = byUid.get(item.getUid());
            if (message != null) {
                item.setNovaMessage(smsService.toResponse(message));
            }
        }
    }

    private SmsMessageResponse findNova(String uid) {
        if (uid == null || uid.isBlank()) {
            return null;
        }
        return smsMessageRepository.findByProviderMessageId(uid.trim())
                .map(smsService::toResponse)
                .orElse(null);
    }
}
