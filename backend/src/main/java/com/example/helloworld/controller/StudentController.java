package com.example.helloworld.controller;

import com.example.helloworld.dto.platform.SubmitAnswerRequest;
import com.example.helloworld.dto.platform.ViolationRequest;
import com.example.helloworld.service.PlatformService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = "*")
public class StudentController {

    private final PlatformService platformService;

    public StudentController(PlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping("/assignments")
    public List<Map<String, Object>> listAssignments(Authentication authentication) {
        return platformService.listStudentAssignments(authentication.getName());
    }

    @PostMapping("/attempts/{assignmentId}/start")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> startAttempt(Authentication authentication, @PathVariable Long assignmentId) {
        return platformService.startAttempt(authentication.getName(), assignmentId);
    }

    @GetMapping("/attempts/{attemptId}/current")
    public Map<String, Object> currentQuestion(Authentication authentication, @PathVariable Long attemptId) {
        return platformService.getCurrentQuestion(authentication.getName(), attemptId);
    }

    @PostMapping("/attempts/{attemptId}/answer")
    public Map<String, Object> submitAnswer(Authentication authentication,
                                            @PathVariable Long attemptId,
                                            @Valid @RequestBody SubmitAnswerRequest request) {
        return platformService.submitAnswer(authentication.getName(), attemptId, request);
    }

    @PostMapping("/attempts/{attemptId}/violation")
    public Map<String, Object> reportViolation(Authentication authentication,
                                               @PathVariable Long attemptId,
                                               @Valid @RequestBody ViolationRequest request) {
        return platformService.reportViolation(authentication.getName(), attemptId, request);
    }

    @GetMapping("/attempts/{attemptId}/result")
    public Map<String, Object> getAttemptResult(Authentication authentication, @PathVariable Long attemptId) {
        return platformService.getAttemptResult(authentication.getName(), attemptId);
    }

    @GetMapping("/results")
    public List<Map<String, Object>> listResults(Authentication authentication) {
        return platformService.listStudentResults(authentication.getName());
    }
}
