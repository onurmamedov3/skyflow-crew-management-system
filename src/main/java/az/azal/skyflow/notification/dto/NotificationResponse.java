package az.azal.skyflow.notification.dto;

import az.azal.skyflow.notification.model.NotificationSeverity;
import az.azal.skyflow.notification.model.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
		UUID id,
		NotificationType type,
		NotificationSeverity severity,
		String message,
		boolean isRead,
		LocalDateTime createdAt,
		String title
) {
}
