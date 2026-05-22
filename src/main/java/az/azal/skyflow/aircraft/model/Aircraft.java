package az.azal.skyflow.aircraft.model;

import az.azal.skyflow.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "aircraft")
public class Aircraft extends BaseEntity {

    @Column(unique = true)
    private String registrationNumber;

    private String model;

    private Integer capacity;

    @Enumerated(EnumType.STRING)
    private AircraftStatus status;

    private LocalDateTime lastMaintenanceDate;

    private LocalDateTime nextMaintenanceDate;

    private Long totalFlightHours;

    @Version
    private Long version;

}