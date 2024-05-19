package com.example.ajouevent.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final CustomErrorCode customErrorCode;

    public CustomException(CustomErrorCode customErrorCode) {
        super(customErrorCode.getMessage());
        this.customErrorCode = customErrorCode;
    }

    public CustomErrorCode getErrorCode() {
        return customErrorCode;
    }
}
