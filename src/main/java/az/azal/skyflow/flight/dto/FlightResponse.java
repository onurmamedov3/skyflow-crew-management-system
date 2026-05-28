package az.azal.skyflow.flight.dto;

import az.azal.skyflow.flight.model.FlightStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record FlightResponse(
		UUID id,
		String flightNumber,
		UUID aircraftId,
		String departureAirport,
		String destinationAirport,
		FlightStatus status,
		LocalDateTime departureTime,
		LocalDateTime arrivalTime,
		String gateNumber
) {
}
