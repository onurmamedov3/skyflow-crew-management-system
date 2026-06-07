package az.azal.skyflow.flight.controller;

import az.azal.skyflow.flight.dto.FlightRequest;
import az.azal.skyflow.flight.dto.FlightResponse;
import az.azal.skyflow.flight.model.FlightStatus;
import az.azal.skyflow.flight.service.FlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
public class FlightController {

	private final FlightService service;

	@GetMapping("/{flightNumber}")
	public ResponseEntity<FlightResponse> getByFlightNumber(@PathVariable String flightNumber){
		return ResponseEntity.ok(service.getByFlightNumber(flightNumber));
	}

	@GetMapping
	public ResponseEntity<Page<FlightResponse>> getAll(Pageable pageable){
		return ResponseEntity.ok(service.getAll(pageable));
	}

	@PostMapping
	public ResponseEntity<FlightResponse> create(@Valid @RequestBody FlightRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
	}

	@PutMapping("/{flightNumber}")
	public ResponseEntity<FlightResponse> update(@PathVariable String flightNumber, @Valid @RequestBody FlightRequest request) {
		return ResponseEntity.ok(service.update(flightNumber, request));
	}

	@DeleteMapping("/{flightNumber}")
	public ResponseEntity<Void> delete(@PathVariable String flightNumber) {
		service.delete(flightNumber);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{flightNumber}/status")
	public ResponseEntity<FlightResponse> changeStatus(@PathVariable String flightNumber, @RequestParam FlightStatus newStatus, @RequestParam String changeReason) {
		FlightResponse response = service.changeStatus(flightNumber, newStatus, changeReason);
		return ResponseEntity.ok(response);
	}
}