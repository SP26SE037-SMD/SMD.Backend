package com.example.smd.dto.response.pdf;

import lombok.*;
import lombok.experimental.FieldDefaults;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProgramInfoDTO  {

    private String programName;
    private String programCode;

}
