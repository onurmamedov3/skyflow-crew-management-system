package az.azal.skyflow.flight.dto;

import az.azal.skyflow.flight.model.DelayReason;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record DelayRequest(
		@NotNull(message = "Delay reason is required")
		DelayReason reason,

		@Size(max = 500, message = "Reason detail must be at most 500 characters")
		String reasonDetail,

		@NotNull(message = "New departure time is required")
		@Future(message = "New departure time must be in the future")
		LocalDateTime newDepartureTime
		) {}