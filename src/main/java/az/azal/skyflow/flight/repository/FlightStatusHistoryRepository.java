package az.azal.skyflow.flight.repository;

import az.azal.skyflow.flight.model.FlightStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FlightStatusHistoryRepository extends JpaRepository<FlightStatusHistory, UUID> {
}
