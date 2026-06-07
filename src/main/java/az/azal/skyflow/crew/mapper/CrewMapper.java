package az.azal.skyflow.crew.mapper;


import az.azal.skyflow.crew.dto.CrewAssignmentResponse;
import az.azal.skyflow.crew.dto.CrewRequest;
import az.azal.skyflow.crew.dto.CrewResponse;
import az.azal.skyflow.crew.model.CrewMember;
import az.azal.skyflow.crew.model.FlightCrewAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CrewMapper {

	CrewResponse toResponse(CrewMember entity);

	@Mapping(source = "flight.id", target = "flightId")
	@Mapping(source = "crewMember.id", target = "crewMemberId")
	@Mapping(source = "crewMember.employeeId", target = "employeeId")
	@Mapping(source = "crewMember.firstName", target = "firstName")
	@Mapping(source = "crewMember.lastName", target = "lastName")
	@Mapping(source = "crewMember.email", target = "email")
	CrewAssignmentResponse toAssignmentResponse(FlightCrewAssignment assignment);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "lastFlightEnd", ignore = true)
	@Mapping(target = "totalFlightMinutes", ignore = true)
	@Mapping(target = "status", ignore = true)
	CrewMember toEntity(CrewRequest request);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "lastFlightEnd", ignore = true)
	@Mapping(target = "totalFlightMinutes", ignore = true)
	@Mapping(target = "status", ignore = true)
	void updateEntity(CrewRequest request,@MappingTarget CrewMember entity);

}
