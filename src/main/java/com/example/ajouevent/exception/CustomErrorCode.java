package com.example.ajouevent.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CustomErrorCode {
    UNAUTHORIZED( "인증에 실패하였습니다.", HttpStatus.UNAUTHORIZED.value()),
    LOGIN_FAILED("아이디 또는 비밀번호가 잘못되었습니다.", HttpStatus.BAD_REQUEST.value());


    private final String message;
    private final int statusCode;

}
