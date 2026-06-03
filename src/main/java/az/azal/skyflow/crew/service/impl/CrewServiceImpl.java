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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CrewServiceImpl implements CrewService {

	private final CrewMemberRepository repository;

	private final CrewMapper crewMapper;

	@Override
	@Transactional(readOnly = true)
	public CrewResponse getCrewByEmployeeId(String employeeId) {
		CrewMember crewMember = repository.findByEmployeeId(employeeId)
				.orElseThrow(() -> ResourceNotFoundException.byField("CrewMember", "employeeId", employeeId));
		return crewMapper.toResponse(crewMember);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<CrewResponse> getAll(Pageable pageable) {
		return repository.findAll(pageable)
				.map(crewMapper::toResponse);
	}

	@Override
	@Transactional
	public CrewResponse create(CrewRequest request) {

		if(repository.existsByEmployeeId(request.employeeId())){
			throw DuplicateResourceException.byField("CrewMember", "employeeId", request.employeeId());
		}

		CrewMember crewMember = crewMapper.toEntity(request);
		crewMember.setStatus(CrewStatus.AVAILABLE);

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
	@Transactional
	public CrewResponse delete(String employeeId) {
		CrewMember crewMember = repository.findByEmployeeId(employeeId)
				.orElseThrow(() -> ResourceNotFoundException.byField("CrewMember", "employeeId", employeeId));

		crewMember.setStatus(CrewStatus.INACTIVE);

		repository.save(crewMember);

		return crewMapper.toResponse(crewMember);
	}
}
