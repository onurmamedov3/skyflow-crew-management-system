package az.azal.skyflow.crew.service;

import az.azal.skyflow.crew.dto.CrewRequest;
import az.azal.skyflow.crew.dto.CrewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CrewService {
	CrewResponse getCrewByEmployeeId(String employeeId);

	Page<CrewResponse> getAll(Pageable pageable);

	CrewResponse create(CrewRequest request);

	CrewResponse update(String employeeId,CrewRequest request);

	CrewResponse delete(String employeeId);
}
