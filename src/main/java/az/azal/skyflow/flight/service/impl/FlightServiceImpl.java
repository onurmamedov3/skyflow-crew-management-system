package az.azal.skyflow.flight.service.impl;

import az.azal.skyflow.aircraft.model.Aircraft;
import az.azal.skyflow.aircraft.repository.AircraftRepository;
import az.azal.skyflow.common.exception.custom.BusinessRuleViolationException;
import az.azal.skyflow.common.exception.custom.DuplicateResourceException;
import az.azal.skyflow.common.exception.custom.ResourceNotFoundException;
import az.azal.skyflow.flight.dto.FlightRequest;
import az.azal.skyflow.flight.dto.FlightResponse;
import az.azal.skyflow.flight.mapper.FlightMapper;
import az.azal.skyflow.flight.model.Flight;
import az.azal.skyflow.flight.model.FlightStatus;
import az.azal.skyflow.flight.model.FlightStatusHistory;
import az.azal.skyflow.flight.repository.FlightRepository;
import az.azal.skyflow.flight.repository.FlightStatusHistoryRepository;
import az.azal.skyflow.flight.service.FlightService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FlightServiceImpl implements FlightService {

	private static final Map<FlightStatus, Set<FlightStatus>> ALLOWED_TRANSITIONS = Map.of(
			FlightStatus.SCHEDULED, Set.of(FlightStatus.BOARDING, FlightStatus.DELAYED, FlightStatus.CANCELLED),
			FlightStatus.DELAYED, Set.of(FlightStatus.BOARDING, FlightStatus.CANCELLED),
			FlightStatus.BOARDING, Set.of(FlightStatus.IN_FLIGHT),
			FlightStatus.IN_FLIGHT, Set.of(FlightStatus.ARRIVED)
	);

	private final FlightRepository flightRepository;
	private final AircraftRepository aircraftRepository;
	private final FlightMapper flightMapper;
	private final FlightStatusHistoryRepository statusHistoryRe;

	@Override
	@Transactional(readOnly = true)
	public FlightResponse getByFlightNumber(String flightNumber) {
		return flightRepository.findByFlightNumber(flightNumber)
				.map(flightMapper::toResponse)
				.orElseThrow(() -> ResourceNotFoundException.byField("Flight", "flightNumber", flightNumber));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<FlightResponse> getAll(Pageable pageable) {
		return flightRepository.findAll(pageable)
				.map(flightMapper::toResponse);

	}

	@Override
	@Transactional
	public FlightResponse create(FlightRequest request) {

		if(flightRepository.existsByFlightNumber(request.flightNumber())){
			throw DuplicateResourceException.byField("Flight", "flightNumber", request.flightNumber());
		}

		Aircraft aircraft = aircraftRepository.findById(request.aircraftId())
				.orElseThrow(() -> ResourceNotFoundException.byId("Aircraft", request.aircraftId()));

		Flight flight = flightMapper.toEntity(request);
		flight.setAircraft(aircraft);
		flight.setStatus(FlightStatus.SCHEDULED);

		flightRepository.save(flight);
		return flightMapper.toResponse(flight);
	}

	@Override
	@Transactional
	public FlightResponse update(String flightNumber, FlightRequest request) {
		Flight flight = flightRepository.findByFlightNumber(flightNumber)
				.orElseThrow(() -> ResourceNotFoundException.byField("Flight", "flightNumber", flightNumber));

		flightMapper.updateEntity(request, flight);

		flightRepository.save(flight);

		return flightMapper.toResponse(flight);
	}

	@Override
	@Transactional
	public FlightResponse delete(String flightNumber) {
		Flight flight = flightRepository.findByFlightNumber(flightNumber)
				.orElseThrow(() -> ResourceNotFoundException.byField("Flight", "flightNumber", flightNumber));

		flight.setStatus(FlightStatus.CANCELLED);
		flightRepository.save(flight);
		return flightMapper.toResponse(flight);
	}

	@Override
	@Transactional
	public FlightResponse changeStatus(String flightNumber, FlightStatus newStatus, String changeReason) {

		Flight flight = flightRepository.findByFlightNumber(flightNumber)
				.orElseThrow(() -> ResourceNotFoundException.byField("Flight", "flightNumber", flightNumber));

		Set<FlightStatus> allowedStatuses = ALLOWED_TRANSITIONS.get(flight.getStatus());

		if (allowedStatuses == null || !allowedStatuses.contains(newStatus)) {
			throw BusinessRuleViolationException.invalidStatusTransition(
					flight.getStatus().name(),
					newStatus.name()
			);
		}

		FlightStatusHistory statusHistory = new FlightStatusHistory();
		statusHistory.setFlight(flight);
		statusHistory.setOldStatus(flight.getStatus());
		statusHistory.setNewStatus(newStatus);
		statusHistory.setChangeTime(LocalDateTime.now());
		statusHistory.setChangeReason(changeReason);
		statusHistory.setChangedBy("SYSTEM"); //Temporary

		statusHistoryRe.save(statusHistory);

		flight.setStatus(newStatus);
		flightRepository.save(flight);
		return flightMapper.toResponse(flight);
	}
}
