package az.azal.skyflow.crew.model;

import az.azal.skyflow.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "crew_member")
public class CrewMember extends BaseEntity {

    @Column(unique = true)
    private String employeeId; //system id den ferqli olaraq oxunaqliq ve id kartdaki kodu

    private String firstName;

    private String lastName;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private CrewRole role;

    @Enumerated(EnumType.STRING)
    private CrewStatus status;

    private LocalDateTime lastFlightEnd;

    private Integer totalFlightMinutes;

    @Version
    private Long version;
}