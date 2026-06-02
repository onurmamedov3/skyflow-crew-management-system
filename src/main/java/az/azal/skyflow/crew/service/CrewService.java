package az.azal.skyflow.crew.service;

import az.azal.skyflow.crew.dto.CrewRequest;
import az.azal.skyflow.crew.dto.CrewResponse;

import java.util.List;

public interface CrewService {
	CrewResponse getCrewByEmployeeId(String employeeId);

	List<CrewResponse> getAll();

	CrewResponse create(CrewRequest request);

	CrewResponse update(String employeeId,CrewRequest request);

	CrewResponse delete(String employeeId);
}
