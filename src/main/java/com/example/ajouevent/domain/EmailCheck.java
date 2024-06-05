package com.example.ajouevent.domain;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.time.LocalDateTime;

@Getter
@Setter
@RedisHash(value = "EmailCheck", timeToLive = 300)
public class EmailCheck {
    @Id
    private String id;
    @Indexed
    private final String email;
    @Setter
    private String code;

    public EmailCheck(String email, String code) {
        this.email = email;
        this.code = code;
    }

}
