package az.azal.skyflow.flight.service.impl;

import az.azal.skyflow.common.exception.custom.BusinessRuleViolationException;
import az.azal.skyflow.flight.model.Flight;
import az.azal.skyflow.flight.model.FlightStatus;
import az.azal.skyflow.flight.model.FlightStatusHistory;
import az.azal.skyflow.flight.repository.FlightStatusHistoryRepository;
import az.azal.skyflow.flight.service.FlightStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FlightStatusServiceImpl implements FlightStatusService {

	private static final Map<FlightStatus, Set<FlightStatus>> ALLOWED_TRANSITIONS = Map.of(
			FlightStatus.SCHEDULED, Set.of(FlightStatus.BOARDING, FlightStatus.DELAYED, FlightStatus.CANCELLED),
			FlightStatus.DELAYED, Set.of(FlightStatus.BOARDING, FlightStatus.CANCELLED),
			FlightStatus.BOARDING, Set.of(FlightStatus.IN_FLIGHT),
			FlightStatus.IN_FLIGHT, Set.of(FlightStatus.ARRIVED)
	);

	private final FlightStatusHistoryRepository flightStatusHistoryRepository;

	@Override
	@Transactional
	public void changeFlightStatus(Flight flight, FlightStatus newStatus, String changedBy, String reason) {

		Set<FlightStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(flight.getStatus(), Set.of());
		if (!allowed.contains(newStatus)) {
			throw BusinessRuleViolationException.invalidStatusTransition(
					flight.getStatus().name(), newStatus.name());
		}

		FlightStatusHistory history = new FlightStatusHistory();

		history.setFlight(flight);
		history.setOldStatus(flight.getStatus());
		history.setNewStatus(newStatus);
		history.setChangedBy(changedBy);
		history.setChangeReason(reason);
		history.setChangeTime(LocalDateTime.now());
		flight.setStatus(newStatus);

		flightStatusHistoryRepository.save(history);
	}
}
