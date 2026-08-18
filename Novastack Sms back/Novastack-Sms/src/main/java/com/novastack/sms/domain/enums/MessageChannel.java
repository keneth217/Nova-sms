package com.novastack.sms.domain.enums;

public enum MessageChannel {
    SMS,
    WHATSAPP;

    public String talkSasaType() {
        return this == WHATSAPP ? "whatsapp" : "plain";
    }

    public String displayName() {
        return this == WHATSAPP ? "WhatsApp" : "SMS";
    }
}
