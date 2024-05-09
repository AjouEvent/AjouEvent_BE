package com.example.ajouevent.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.ajouevent.domain.Type;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PostEventDto {

    @NotNull(message = "제목은 Null 일 수 없습니다!")
    private String title;

    private String content;

    private String url;

    private String writer;

    private String subject;

    private String major;

    private String postImage;

    private LocalDateTime eventDateTime;

    @NotNull(message = "type은 Null일 수 없습니다")
    private Type type;

    // 이미지 URL 리스트 추가
    private List<String> imageUrls;

}
