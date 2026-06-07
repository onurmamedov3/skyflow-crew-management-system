package az.azal.skyflow.flight.service.impl;

import az.azal.skyflow.common.exception.custom.BusinessRuleViolationException;
import az.azal.skyflow.common.exception.custom.ResourceNotFoundException;
import az.azal.skyflow.flight.dto.DelayRequest;
import az.azal.skyflow.flight.dto.DelayResponse;
import az.azal.skyflow.flight.mapper.DelayMapper;
import az.azal.skyflow.flight.model.Flight;
import az.azal.skyflow.flight.model.FlightDelay;
import az.azal.skyflow.flight.model.FlightStatus;
import az.azal.skyflow.flight.repository.FlightDelayRepository;
import az.azal.skyflow.flight.repository.FlightRepository;
import az.azal.skyflow.flight.service.FlightDelayService;
import az.azal.skyflow.flight.service.FlightStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FlightDelayServiceImpl implements FlightDelayService {

	private final FlightRepository flightRepository;
	private final FlightDelayRepository flightDelayRepository;
	private final FlightStatusService flightStatusService;
	private final DelayMapper delayMapper;

	@Transactional
	public DelayResponse delayFlight(UUID flightId, DelayRequest request, String delayedBy) {

		Flight flight = flightRepository.findById(flightId)
				.orElseThrow(() -> ResourceNotFoundException.byId(Flight.class.toString(), flightId));

		if(flight.getStatus() != FlightStatus.SCHEDULED && flight.getStatus() != FlightStatus.DELAYED) {
			throw BusinessRuleViolationException.flightStatusViolation(flight.getStatus());
		}

		if(!request.newDepartureTime().isAfter(flight.getDepartureTime())) {
			throw BusinessRuleViolationException.invalidDelayTime(request.newDepartureTime(), flight.getDepartureTime());
		}

		long delayMinutes = Duration.between(flight.getDepartureTime(), request.newDepartureTime()).toMinutes();

		Duration flightDuration = Duration.between(flight.getDepartureTime(), flight.getArrivalTime());

		LocalDateTime newArrivalTime = request.newDepartureTime().plus(flightDuration);

		FlightDelay delay = new FlightDelay();
		delay.setFlight(flight);
		delay.setDelayReason(request.reason());
		delay.setDelayReasonDetail(request.reasonDetail());
		delay.setDelayMinutes((int) delayMinutes);
		delay.setOriginalDepartureTime(flight.getDepartureTime());
		delay.setNewDepartureTime(request.newDepartureTime());
		delay.setNewArrivalTime(newArrivalTime);
		delay.setHighRisk(delayMinutes >= 120);
		// delay.setReportedBy(delayedBy); // this will be handled after the security is written
		flightDelayRepository.save(delay);

		flight.setDepartureTime(request.newDepartureTime());
		flight.setArrivalTime(newArrivalTime);

		flightStatusService.changeFlightStatus(flight, FlightStatus.DELAYED, delayedBy, "Delay reason: " + request.reason());

		flightRepository.save(flight);

		return delayMapper.toResponse(delay);
	}


}
