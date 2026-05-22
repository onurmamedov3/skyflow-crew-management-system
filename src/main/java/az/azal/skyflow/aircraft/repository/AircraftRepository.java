package az.azal.skyflow.aircraft.repository;

import az.azal.skyflow.aircraft.model.Aircraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AircraftRepository extends JpaRepository<Aircraft, UUID> {
}