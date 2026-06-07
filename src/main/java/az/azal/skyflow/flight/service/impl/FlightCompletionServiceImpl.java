package az.azal.skyflow.flight.service.impl;

import az.azal.skyflow.aircraft.model.Aircraft;
import az.azal.skyflow.aircraft.model.AircraftStatus;
import az.azal.skyflow.aircraft.repository.AircraftRepository;
import az.azal.skyflow.common.exception.custom.BusinessRuleViolationException;
import az.azal.skyflow.common.exception.custom.ResourceNotFoundException;
import az.azal.skyflow.crew.model.AssignmentStatus;
import az.azal.skyflow.crew.model.CrewMember;
import az.azal.skyflow.crew.model.FlightCrewAssignment;
import az.azal.skyflow.crew.repository.FlightCrewAssignmentRepository;
import az.azal.skyflow.crew.service.CrewService;
import az.azal.skyflow.flight.dto.FlightResponse;
import az.azal.skyflow.flight.mapper.FlightMapper;
import az.azal.skyflow.flight.model.Flight;
import az.azal.skyflow.flight.model.FlightStatus;
import az.azal.skyflow.flight.repository.FlightRepository;
import az.azal.skyflow.flight.service.FlightCompletionService;
import az.azal.skyflow.flight.service.FlightStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FlightCompletionServiceImpl implements FlightCompletionService {

	private final FlightRepository flightRepository;
	private final FlightStatusService flightStatusService;
	private final AircraftRepository aircraftRepository;
	private final FlightCrewAssignmentRepository assignmentRepository;
	private final CrewService crewService;
	private final FlightMapper flightMapper;

	@Override
	@Transactional
	public FlightResponse completeFlight(UUID flightId, String completedBy) {

		Flight flight = flightRepository.findById(flightId)
				.orElseThrow(() -> ResourceNotFoundException.byId("Flight", flightId));

		flight.setActualArrivalTime(LocalDateTime.now());
		flightStatusService.changeFlightStatus(flight, FlightStatus.ARRIVED, completedBy, "Flight completed");

		Aircraft aircraft = flight.getAircraft();
		if (aircraft != null) {
			aircraft.setStatus(AircraftStatus.MAINTENANCE);
			aircraftRepository.save(aircraft);
		}

		List<FlightCrewAssignment> assignments = assignmentRepository.findByFlightAndAssignmentStatus(flight, AssignmentStatus.ASSIGNED);

		for (FlightCrewAssignment assignment : assignments) {
			CrewMember crewMember = assignment.getCrewMember();

			crewService.recordFlightCompletion(crewMember, flight);
		}

		//	notification will be written in the future after the notification service is implemented

		return flightMapper.toResponse(flight);
	}
}
