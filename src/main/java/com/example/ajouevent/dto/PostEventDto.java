package com.example.ajouevent.dto;

import com.example.ajouevent.domain.Type;
import com.google.api.client.util.DateTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;

@Getter
@Setter
@Builder
public class PostEventDto {

    @NotNull(message = "제목은 Null 일 수 없습니다!")
    private String title;

    private String content;

    private String writer;

    private DateTime date;

    private String subject;

    private String major;

    @NotNull(message = "type은 Null일 수 없습니다")
    private Type type;

}
