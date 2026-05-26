package az.azal.skyflow.crew.mapper;


import az.azal.skyflow.crew.dto.CrewRequest;
import az.azal.skyflow.crew.dto.CrewResponse;
import az.azal.skyflow.crew.model.CrewMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CrewMapper {

	CrewResponse toResponse(CrewMember entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "lastFlightEnd", ignore = true)
	@Mapping(target = "totalFlightHours", ignore = true)
	CrewMember toEntity(CrewRequest request);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "lastFlightEnd", ignore = true)
	@Mapping(target = "totalFlightHours", ignore = true)
	void updateEntity(CrewRequest request,@MappingTarget CrewMember entity);

}
