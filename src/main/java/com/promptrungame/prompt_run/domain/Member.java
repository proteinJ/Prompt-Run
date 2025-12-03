package com.promptrungame.prompt_run.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    private String password;
    private String nickname;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private boolean isDeleted;
    private String membership;
    private String role;
}
