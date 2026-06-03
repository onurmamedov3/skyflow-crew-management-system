package az.azal.skyflow.crew.controller;

import az.azal.skyflow.crew.dto.CrewRequest;
import az.azal.skyflow.crew.dto.CrewResponse;
import az.azal.skyflow.crew.service.CrewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crew")
@RequiredArgsConstructor
public class CrewController {

	private final CrewService service;

	@GetMapping("/{employeeId}")
	public ResponseEntity<CrewResponse> getCrewByEmployeeId(@PathVariable String employeeId) {
		return ResponseEntity.ok(service.getCrewByEmployeeId(employeeId));
	}

	@GetMapping
	public ResponseEntity<Page<CrewResponse>> getAll(Pageable pageable) {
		return ResponseEntity.ok(service.getAll(pageable));
	}

	@PostMapping
	public ResponseEntity<CrewResponse> createCrew(@Valid @RequestBody CrewRequest request) {

		return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
	}

	@PutMapping("/{employeeId}")
	public ResponseEntity<CrewResponse> updateCrew(@PathVariable String employeeId, @Valid @RequestBody CrewRequest request) {
		return ResponseEntity.ok(service.update(employeeId, request));
	}

	@DeleteMapping("/{employeeId}")
	public ResponseEntity<Void> delete(@PathVariable String employeeId) {
		service.delete(employeeId);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}
}