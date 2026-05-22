package az.azal.skyflow.common.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID entityId;

    private String entityType;

    private String action;

    private String oldValue;

    private String newValue;

    private String performedBy;

    private String ipAddress;

    private String requestId;

    private LocalDateTime performAt;

    private String details;
}