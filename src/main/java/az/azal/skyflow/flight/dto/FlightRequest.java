package az.azal.skyflow.flight.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.UUID;

public record FlightRequest(
		@NotBlank(message = "Flight number is required")
		@Size(max = 10, message = "Flight number must be at most 10 characters")
		@Pattern(regexp = "^[A-Z0-9]+$", message = "Flight number must be uppercase letters and numbers only")
		String flightNumber,

		@NotBlank(message = "Departure airport is required")
		@Size(min = 3, max = 3, message = "Departure airport must be a 3-letter IATA code")
		@Pattern(regexp = "^[A-Z]{3}$", message = "Departure airport must be uppercase letters only")
		String departureAirport,

		@NotBlank(message = "Destination airport is required")
		@Size(min = 3, max = 3, message = "Destination airport must be a 3-letter IATA code")
		@Pattern(regexp = "^[A-Z]{3}$", message = "Destination airport must be uppercase letters only")
		String destinationAirport,

		@NotNull(message = "Departure time is required")
		@Future(message = "Departure time must be in the future")
		LocalDateTime departureTime,

		@NotNull(message = "Arrival time is required")
		@Future(message = "Arrival time must be in the future")
		LocalDateTime arrivalTime,

		@Size(max = 10, message = "Gate number must be at most 10 characters")
		String gateNumber,

		@NotNull(message = "Aircraft is required")
		UUID aircraftId
) {}
