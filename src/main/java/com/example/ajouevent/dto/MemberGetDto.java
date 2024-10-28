package com.example.ajouevent.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberGetDto {
    private String name;
    private String email;
    private String major;
    private String phone;
}
