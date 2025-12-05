package com.promptrungame.prompt_run.repository;

import com.promptrungame.prompt_run.domain.Member;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUsername(String username);

    boolean existsByUsername(@NotBlank(message = "아이디는 필수 입력 항목입니다.") @Size(min = 4, max = 20, message = "아이디는 4~20자여야 합니다.") String username);
}
