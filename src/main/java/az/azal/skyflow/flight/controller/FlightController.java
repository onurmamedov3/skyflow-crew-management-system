package az.azal.skyflow.flight.controller;

import az.azal.skyflow.flight.dto.FlightRequest;
import az.azal.skyflow.flight.dto.FlightResponse;
import az.azal.skyflow.flight.service.FlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/flight")
@RequiredArgsConstructor
public class FlightController {

	private final FlightService service;

	@GetMapping("/{flightNumber}")
	public ResponseEntity<FlightResponse> getByFlightNumber(@PathVariable String flightNumber){
		return ResponseEntity.ok(service.getByFlightNumber(flightNumber));
	}

	@GetMapping
	public ResponseEntity<List<FlightResponse>> getAll(){
		return ResponseEntity.ok(service.getAll());
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
	public ResponseEntity<FlightResponse> delete(@PathVariable String flightNumber) {
		return ResponseEntity.ok(service.delete(flightNumber));
	}
}
