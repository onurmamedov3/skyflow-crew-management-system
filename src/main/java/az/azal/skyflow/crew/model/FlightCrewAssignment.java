package az.azal.skyflow.crew.model;

import az.azal.skyflow.auth.model.AppUser;
import az.azal.skyflow.flight.model.Flight;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "flight_crew_assignment")
public class FlightCrewAssignment {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private CrewMember crewMember;

    @Enumerated(EnumType.STRING)
    private CrewRole roleOnFlight;

    @ManyToOne(fetch = FetchType.LAZY)
    private Flight flight;

    @Enumerated(EnumType.STRING)
    private AssignmentStatus assignmentStatus;

    private LocalDateTime assignedAt;

    private LocalDateTime removedAt;

    private String removalReason;

    @ManyToOne(fetch = FetchType.LAZY)
    private AppUser assignedBy;
}