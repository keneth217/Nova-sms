package com.novastack.sms.provider;

import com.novastack.sms.domain.enums.MessageStatus;

public final class TalkSasaStatusMapper {

    private TalkSasaStatusMapper() {
    }

    public static MessageStatus toInternal(String providerStatus) {
        String normalized = normalize(providerStatus);
        if (normalized.isEmpty()) {
            return MessageStatus.PENDING;
        }

        return switch (normalized) {
            case "new", "queued", "queue", "pending", "scheduled", "waiting" -> MessageStatus.PENDING;
            case "processing" -> MessageStatus.PROCESSING;
            case "accepted", "processed", "submitted" -> MessageStatus.ACCEPTED;
            case "sent", "sending" -> MessageStatus.SENT;
            case "delivered", "delivery success", "success", "completed", "complete", "done", "finished"
                    -> MessageStatus.DELIVERED;
            case "rejected", "invalid", "undeliverable" -> MessageStatus.REJECTED;
            case "cancelled", "canceled", "stopped" -> MessageStatus.CANCELLED;
            case "failed", "failure", "undelivered", "expired", "error" -> MessageStatus.FAILED;
            default -> {
                if (normalized.contains("deliver") || normalized.contains("complete")) {
                    yield MessageStatus.DELIVERED;
                }
                if (normalized.contains("reject")) {
                    yield MessageStatus.REJECTED;
                }
                if (normalized.contains("cancel")) {
                    yield MessageStatus.CANCELLED;
                }
                if (normalized.contains("fail") || normalized.contains("error") || normalized.contains("expir")) {
                    yield MessageStatus.FAILED;
                }
                if (normalized.contains("sent")) {
                    yield MessageStatus.SENT;
                }
                if (normalized.contains("process")) {
                    yield MessageStatus.PROCESSING;
                }
                if (normalized.contains("accept") || normalized.contains("submit")) {
                    yield MessageStatus.ACCEPTED;
                }
                yield MessageStatus.PENDING;
            }
        };
    }

    /**
     * Queue lookup includes counts. {@code completed} with no failures is {@link MessageStatus#DELIVERED}.
     * Returns {@code null} when mixed bulk results would incorrectly mark failed recipients as delivered.
     */
    public static MessageStatus fromQueue(
            String providerStatus,
            Integer failedCount,
            Integer remaining,
            Integer processedCount,
            Integer recipientCount,
            String error) {
        if (hasError(error)) {
            return MessageStatus.FAILED;
        }
        MessageStatus mapped = toInternal(providerStatus);
        if (!isCompleted(providerStatus)) {
            return mapped;
        }

        int failed = failedCount == null ? 0 : Math.max(0, failedCount);
        int left = remaining == null ? 0 : Math.max(0, remaining);
        int processed = processedCount == null ? 0 : Math.max(0, processedCount);
        int recipients = recipientCount == null ? processed : Math.max(0, recipientCount);
        boolean single = recipients <= 1 || processed <= 1;

        if (failed > 0) {
            boolean allFailed = processed > 0 && processed == failed;
            if (single || allFailed) {
                return MessageStatus.FAILED;
            }
            return null;
        }
        if (left == 0) {
            return MessageStatus.DELIVERED;
        }
        return MessageStatus.PROCESSING;
    }

    public static String normalize(String providerStatus) {
        if (providerStatus == null) {
            return "";
        }
        return providerStatus.trim().toLowerCase().replace('_', ' ').replace('-', ' ');
    }

    private static boolean isCompleted(String providerStatus) {
        String normalized = normalize(providerStatus);
        return "completed".equals(normalized)
                || "complete".equals(normalized)
                || "done".equals(normalized)
                || "finished".equals(normalized)
                || normalized.contains("complete");
    }

    private static boolean hasError(String error) {
        return error != null && !error.isBlank() && !"null".equalsIgnoreCase(error.trim());
    }
}
