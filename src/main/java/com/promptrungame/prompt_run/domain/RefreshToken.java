package com.promptrungame.prompt_run.domain;

import com.fasterxml.jackson.core.JsonToken;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken implements Serializable {
    @Id
    private Long id;
    private String tokenValue;
    private Long memberId;
    private Date expiredAt;

}
