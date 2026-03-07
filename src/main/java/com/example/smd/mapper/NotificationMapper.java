package com.example.smd.mapper;

import com.example.smd.dto.request.NotificationRequest;
import com.example.smd.dto.response.NotificationResponse;
import com.example.smd.entities.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    // Ánh xạ từ Entity sang Response
    @Mapping(source = "account.accountId", target = "accountId")
    @Mapping(source = "account.email", target = "accountEmail")
    NotificationResponse toNotificationResponse(Notification notification);

    // Ánh xạ từ Request sang Entity (không map account, sẽ set trong service)
    @Mapping(target = "notificationId", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "isRead", ignore = true)
    Notification toNotification(NotificationRequest request);
}
