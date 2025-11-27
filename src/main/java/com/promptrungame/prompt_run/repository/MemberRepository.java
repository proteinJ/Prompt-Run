package com.promptrungame.prompt_run.repository;

import com.promptrungame.prompt_run.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Integer> {
    Optional<Member> findByUsername(String username);
}
