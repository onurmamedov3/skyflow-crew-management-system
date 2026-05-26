package az.azal.skyflow.flight.mapper;


import az.azal.skyflow.flight.dto.FlightRequest;
import az.azal.skyflow.flight.dto.FlightResponse;
import az.azal.skyflow.flight.model.Flight;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FlightMapper {

	FlightResponse toResponse(Flight entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "lastFlightEnd", ignore = true)
	@Mapping(target = "totalFlightHours", ignore = true)
	Flight toEntity(FlightRequest request);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "lastFlightEnd", ignore = true)
	@Mapping(target = "totalFlightHours", ignore = true)
	void updateEntity(FlightRequest request, @MappingTarget Flight entity);

}
