package az.azal.skyflow.crew.service.impl;


import az.azal.skyflow.common.exception.custom.DuplicateResourceException;
import az.azal.skyflow.common.exception.custom.ResourceNotFoundException;
import az.azal.skyflow.crew.dto.CrewRequest;
import az.azal.skyflow.crew.dto.CrewResponse;
import az.azal.skyflow.crew.mapper.CrewMapper;
import az.azal.skyflow.crew.model.CrewMember;
import az.azal.skyflow.crew.model.CrewStatus;
import az.azal.skyflow.crew.repository.CrewMemberRepository;
import az.azal.skyflow.crew.service.CrewService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CrewServiceImpl implements CrewService {

	private final CrewMemberRepository repository;

	private final CrewMapper crewMapper;

	@Override
	@Transactional
	public CrewResponse getCrewByEmployeeId(String employeeId) {
		CrewMember crewMember = repository.findByEmployeeId(employeeId)
				.orElseThrow(() -> ResourceNotFoundException.byField("CrewMember", "employeeId", employeeId));
		return crewMapper.toResponse(crewMember);
	}

	@Override
	@Transactional
	public List<CrewResponse> getAll() {

		return repository.findAll()
				.stream()
				.map(crewMapper::toResponse)
				.toList();
	}

	@Override
	@Transactional
	public CrewResponse create(CrewRequest request) {

		if(repository.existsByEmployeeId(request.employeeId())){
			throw DuplicateResourceException.byField("CrewMember", "employeeId", request.employeeId());
		}

		CrewMember crewMember = crewMapper.toEntity(request);

		repository.save(crewMember);

		return crewMapper.toResponse(crewMember);
	}

	@Override
	@Transactional
	public CrewResponse update(String employeeId, CrewRequest request) {
		CrewMember crewMember = repository.findByEmployeeId(employeeId)
				.orElseThrow(() -> ResourceNotFoundException.byField("CrewMember", "employeeId", employeeId));

		crewMapper.updateEntity(request, crewMember);

		repository.save(crewMember);

		return crewMapper.toResponse(crewMember);
	}

	@Override
	public CrewResponse delete(String employeeId) {
		CrewMember crewMember = repository.findByEmployeeId(employeeId)
				.orElseThrow(() -> ResourceNotFoundException.byField("CrewMember", "employeeId", employeeId));

		crewMember.setStatus(CrewStatus.INACTIVE);

		repository.save(crewMember);

		return crewMapper.toResponse(crewMember);
	}
}
