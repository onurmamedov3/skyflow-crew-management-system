package az.azal.skyflow.flight.service;

import az.azal.skyflow.flight.model.Flight;
import az.azal.skyflow.flight.model.FlightStatus;

public interface FlightStatusService {
	void changeFlightStatus(Flight flight, FlightStatus newStatus, String changedBy, String reason);
}
