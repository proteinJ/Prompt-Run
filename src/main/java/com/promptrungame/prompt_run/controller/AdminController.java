package com.promptrungame.prompt_run.controller;

import com.promptrungame.prompt_run.domain.GameConfig;
import com.promptrungame.prompt_run.domain.Member;
import com.promptrungame.prompt_run.domain.QuizEntity;
import com.promptrungame.prompt_run.repository.GameConfigRepository;
import com.promptrungame.prompt_run.repository.MemberRepository;
import com.promptrungame.prompt_run.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MemberRepository memberRepository;
    private final GameConfigRepository gameConfigRepository;
    private final QuizRepository quizRepository;

    // ###########################
    // #### === 회원 관리 ===  ####
    // ###########################
    @GetMapping("/members")
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }



    // ###########################
    // #### === 게임 설정 ===  ####
    // ###########################
    // 1. MaxTurn Limit 설정
    // 1) 조회
    @GetMapping("/config/turn")
    public String getMaxTurn() {
        return gameConfigRepository.findById("MAX_TURN")
                .map(GameConfig::getConfigValue)
                .orElse("11");
    }
    // 2) 업데이트
    @PostMapping("/config/turn")
    public ResponseEntity<?> updateMaxTurn(@RequestBody String newTurn) {
        GameConfig config = GameConfig.builder()
                .ConfigKey("MAX_TURN")
                .ConfigValue(newTurn.replaceAll("[^0-9]", ""))
                .build();
        gameConfigRepository.save(config);
        return ResponseEntity.ok("턴 수가 " + config.getConfigValue() + "회로 변경 되었습니다.");
    }



    // ###########################
    // #### === 퀴즈 관리 ===  ####
    // ###########################
    // 1. 전체 조회
    @GetMapping("/quizzes")
    public List<QuizEntity> getAllQuizzes() {
        return quizRepository.findAll();
    }
    // 2. 퀴즈 추가
    @PostMapping("/quizzes")
    public QuizEntity addQuiz(@RequestBody QuizEntity quiz) {
        return quizRepository.save(quiz);
    }
    // 3. 퀴즈 삭제
    @DeleteMapping("/quizzes/{id}")
    public ResponseEntity<?> deleteQuiz(@PathVariable Long id) {
        quizRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }


}
