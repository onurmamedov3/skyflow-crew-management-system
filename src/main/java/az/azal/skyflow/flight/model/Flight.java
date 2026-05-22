package az.azal.skyflow.flight.model;

import az.azal.skyflow.aircraft.model.Aircraft;
import az.azal.skyflow.common.model.BaseEntity;
import az.azal.skyflow.crew.model.FlightCrewAssignment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "flight")
public class Flight extends BaseEntity {

    @Column(nullable = false, length = 10)
    private String flightNumber;

    @Column(nullable = false, length = 3)
    private String departureAirport;

    @Column(nullable = false, length = 3)
    private String destinationAirport;

    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    private LocalDateTime actualDepartureTime;

    private LocalDateTime actualArrivalTime;

    private Integer sequenceOrder; //bu teyare aktarmada necendir?

    private String gateNumber; //teyareye minmek qapisi

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlightStatus status; //ucunsun hazirki statusu

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aircraft_id")
    private Aircraft aircraft; //hansi teyyare teyin olunub?

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_flight_id")
    private Flight parentFlight; //bu hansisa ucunsun davamidirmi?

    @OneToMany(mappedBy = "flight")
    private List<FlightCrewAssignment> crewAssignments; //kimler idare edir

    @OneToMany(mappedBy = "flight")
    private List<FlightDelay> delays; //ucus nece defe gecikib ve niye?

    @OneToMany(mappedBy = "flight")
    private List<FlightStatusHistory> statusHistory; //ucusun statusunu kim nece deyisib?

    @Version
    private Long version;
}