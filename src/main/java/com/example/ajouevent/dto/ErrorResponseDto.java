package com.example.ajouevent.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErrorResponseDto {
    private int status;
    private String message;
    private LocalDateTime timestamp;

    // 생성자, getter, setter 등 필요한 메서드 추가

    public ErrorResponseDto(int status, String message, LocalDateTime timestamp) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Getter, Setter 등 필요한 메서드
}