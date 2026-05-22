package az.azal.skyflow.flight.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "flight_status_history")
public class FlightStatusHistory {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Flight flight;

    @Enumerated(EnumType.STRING)
    private FlightStatus oldStatus;

    @Enumerated(EnumType.STRING)
    private FlightStatus newStatus;

    private String changeReason;

    private LocalDateTime changeTime;

    private String changedBy;
}