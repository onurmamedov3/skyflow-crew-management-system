package az.azal.skyflow.flight.mapper;


import az.azal.skyflow.flight.dto.FlightRequest;
import az.azal.skyflow.flight.dto.FlightResponse;
import az.azal.skyflow.flight.model.Flight;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FlightMapper {

	@Mapping(source = "aircraft.id", target = "aircraftId")
	FlightResponse toResponse(Flight entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "aircraft", ignore = true)
	@Mapping(target = "parentFlight", ignore = true)
	@Mapping(target = "crewAssignments", ignore = true)
	@Mapping(target = "delays", ignore = true)
	@Mapping(target = "statusHistory", ignore = true)
	@Mapping(target = "actualDepartureTime", ignore = true)
	@Mapping(target = "actualArrivalTime", ignore = true)
	@Mapping(target = "sequenceOrder", ignore = true)
	Flight toEntity(FlightRequest request);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "aircraft", ignore = true)
	@Mapping(target = "parentFlight", ignore = true)
	@Mapping(target = "crewAssignments", ignore = true)
	@Mapping(target = "delays", ignore = true)
	@Mapping(target = "statusHistory", ignore = true)
	@Mapping(target = "actualDepartureTime", ignore = true)
	@Mapping(target = "actualArrivalTime", ignore = true)
	@Mapping(target = "sequenceOrder", ignore = true)
	void updateEntity(FlightRequest request, @MappingTarget Flight entity);

}
