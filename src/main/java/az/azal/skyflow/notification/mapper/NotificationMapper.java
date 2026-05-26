package az.azal.skyflow.notification.mapper;


import az.azal.skyflow.notification.dto.NotificationResponse;
import az.azal.skyflow.notification.model.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
	NotificationResponse toResponse(Notification entity);
}
