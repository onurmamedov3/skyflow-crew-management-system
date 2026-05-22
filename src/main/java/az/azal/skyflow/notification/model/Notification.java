package az.azal.skyflow.notification.model;

import az.azal.skyflow.auth.model.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    private NotificationSeverity severity;

    private String title;

    private String message;

    private String targetType;

    private UUID targetId;

    private boolean isRead;

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private AppUser user;
}