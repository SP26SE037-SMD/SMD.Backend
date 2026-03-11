package com.example.smd.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
//@JsonInclude(JsonInclude.Include.USE_DEFAULTS)
public class AccountProfileResponse {
    
    String profileId;
    
    String accountId;
    
    String avatarUrl;
    
    String phoneNumber;
    
    String createdAt;
}
