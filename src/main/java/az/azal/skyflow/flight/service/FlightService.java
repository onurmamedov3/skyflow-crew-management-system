package az.azal.skyflow.flight.service;

import az.azal.skyflow.flight.dto.FlightRequest;
import az.azal.skyflow.flight.dto.FlightResponse;
import az.azal.skyflow.flight.model.FlightStatus;

import java.util.List;

public interface FlightService {
	FlightResponse getByFlightNumber(String flightNumber);

	List<FlightResponse> getAll();

	FlightResponse create(FlightRequest request);

	FlightResponse update(String flightNumber, FlightRequest request);

	FlightResponse delete(String flightNumber);

	FlightResponse changeStatus(String flightNumber, FlightStatus newStatus, String changeReason);
}
