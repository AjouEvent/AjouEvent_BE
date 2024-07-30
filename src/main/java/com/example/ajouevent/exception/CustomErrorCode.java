package com.example.ajouevent.exception;

import com.google.api.Http;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CustomErrorCode {
    UNAUTHORIZED( "인증에 실패하였습니다.", HttpStatus.UNAUTHORIZED.value()),
    INVALID_TOKEN( "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED.value()),
    EXPIRED_TOKEN("토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED.value()),
    UNSUPPORTED_TOKEN("지원되지 않는 토큰입니다.", HttpStatus.UNAUTHORIZED.value()),
    ILLEGAL_ARGUMENT_TOKEN("값 자체가 잘못 되었습니다.", HttpStatus.UNAUTHORIZED.value()),
    LOGIN_NEEDED("로그인이 필요합니다.", HttpStatus.UNAUTHORIZED.value()),
    LOGIN_FAILED("아이디 또는 비밀번호가 잘못되었습니다.", HttpStatus.BAD_REQUEST.value()),
    DUPLICATED_EMAIL("이미 존재하는 이메일입니다.", HttpStatus.BAD_REQUEST.value()),
    USER_NOT_FOUND( "사용자를 찾을 수 없습니다,", HttpStatus.NOT_FOUND.value()),
    EVENT_NOT_FOUND("이벤트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    TOPIC_NOT_FOUND("토픽을 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    FILE_NOT_FOUND("파일을 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    EVENT_NOT_LIKED("찜한 이벤트가 아닙니다.", HttpStatus.BAD_REQUEST.value()),
    FILE_CONVERT_FAILED("파일을 전환하던 도중 오류가 발생했습니다.",HttpStatus.BAD_REQUEST.value()),
    IMAGE_UPLOAD_FAILED("이미지를 업로드 하는 도중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    IMAGE_DELETE_FAILED("이미지를 삭제하는 도중 오류가 발생하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    SUBSCRIBE_FAILED("구독하는 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    ALREADY_SUBSCRIBED_TOPIC("이미 해당 토픽을 구독하고 있습니다.", HttpStatus.CONFLICT.value()),
    SUBSCRIBE_CANCEL_FAILED("구독을 취소하는 도중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    EMAIL_CHECK_FAILED("이메일을 인증하는 도중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    NO_SUCH_ALGORITHM("이메일 인증에서 해당하는 알고리즘을 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    CODE_FAILED("코드가 일치하지 않습니다.", HttpStatus.BAD_REQUEST.value()),
    TOPIC_NOTIFICATION_FAILED("토픽으로 FCM 메시지를 보내는 도중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    MAX_KEYWORD_LIMIT_EXCEEDED("최대 10개의 키워드를 구독할 수 있습니다.", HttpStatus.BAD_REQUEST.value()),
    KEYWORD_NOT_FOUND("키워드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value());

    private final String message;
    private final int statusCode;

}
