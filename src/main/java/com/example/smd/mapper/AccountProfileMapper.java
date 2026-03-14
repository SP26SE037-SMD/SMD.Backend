package com.example.smd.mapper;

import com.example.smd.dto.response.account.AccountProfileResponse;
import com.example.smd.entities.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountProfileMapper {

    @Mapping(source = "accountId", target = "accountId")
    @Mapping(source = "avatarUrl", target = "avatarUrl")
    @Mapping(source = "phoneNumber", target = "phoneNumber")
    AccountProfileResponse toResponse(Account account);
}

