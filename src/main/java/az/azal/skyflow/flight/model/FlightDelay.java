package az.azal.skyflow.flight.model;

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
@Table(name = "flight_delay")
public class FlightDelay {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Flight flight;

    @Enumerated(EnumType.STRING)
    private DelayReason delayReason;

    private String delayReasonDetail;

    private LocalDateTime originalDepartureTime;

    private LocalDateTime newDepartureTime;

    private LocalDateTime newArrivalTime;

    private Long delayMinutes;

    private boolean isHighRisk;

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private AppUser reportedBy ;
}