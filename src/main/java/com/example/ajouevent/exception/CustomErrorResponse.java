package com.example.ajouevent.exception;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomErrorResponse {
    private final int status;
    private final String statusMessage;
    private final String message;
}
