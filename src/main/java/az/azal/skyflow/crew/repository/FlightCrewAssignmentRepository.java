package az.azal.skyflow.crew.repository;

import az.azal.skyflow.crew.model.FlightCrewAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FlightCrewAssignmentRepository extends JpaRepository<FlightCrewAssignment, UUID> {
}
