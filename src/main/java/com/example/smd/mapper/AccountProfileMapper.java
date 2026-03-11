package com.example.smd.mapper;

import com.example.smd.dto.response.AccountProfileResponse;
import com.example.smd.entities.Account_Profile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountProfileMapper {
    
    @Mapping(source = "profileId", target = "profileId")
    @Mapping(source = "account.accountId", target = "accountId")
    @Mapping(source = "avatarUrl", target = "avatarUrl")
    @Mapping(source = "phoneNumber", target = "phoneNumber")
    @Mapping(source = "createdAt", target = "createdAt")
    AccountProfileResponse toResponse(Account_Profile accountProfile);
}
