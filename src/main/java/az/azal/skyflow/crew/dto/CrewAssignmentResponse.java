package az.azal.skyflow.crew.dto;

import az.azal.skyflow.crew.model.AssignmentStatus;
import az.azal.skyflow.crew.model.CrewRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record CrewAssignmentResponse(
		UUID id,
		UUID flightId,
		UUID crewMemberId,
		String employeeId,
		String firstName,
		String lastName,
		String email,
		CrewRole roleOnFlight,
		AssignmentStatus assignmentStatus,
		LocalDateTime assignedAt
) {
}
