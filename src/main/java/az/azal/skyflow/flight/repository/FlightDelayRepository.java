package az.azal.skyflow.flight.repository;

import az.azal.skyflow.flight.model.FlightDelay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FlightDelayRepository extends JpaRepository<FlightDelay, UUID> {
}
