package az.azal.skyflow.crew.dto;

import az.azal.skyflow.crew.model.CrewRole;
import az.azal.skyflow.crew.model.CrewStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record CrewResponse(
		UUID id,
		String employeeId,
		String firstName,
		String lastName,
		CrewRole role,
		String email,
		String phoneNumber,
		CrewStatus status,
		Integer totalFlightMinutes,
		LocalDateTime lastFlightEnd
) {
}
