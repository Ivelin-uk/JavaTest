package com.example.helloworld.controller;

import com.example.helloworld.dto.platform.AddGroupMemberRequest;
import com.example.helloworld.dto.platform.AssignToGroupRequest;
import com.example.helloworld.dto.platform.AssignToStudentRequest;
import com.example.helloworld.dto.platform.CreateGroupRequest;
import com.example.helloworld.dto.platform.CreateManualQuestionRequest;
import com.example.helloworld.dto.platform.CreateSubjectRequest;
import com.example.helloworld.dto.platform.CreateTestRequest;
import com.example.helloworld.dto.platform.GenerateAiQuestionsRequest;
import com.example.helloworld.dto.platform.UpdateQuestionRequest;
import com.example.helloworld.dto.platform.UpdateTestRequest;
import com.example.helloworld.service.PlatformService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
@CrossOrigin(origins = "*")
public class TeacherController {

    private final PlatformService platformService;

    public TeacherController(PlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping("/subjects")
    public List<Map<String, Object>> listSubjects(Authentication authentication) {
        return platformService.listSubjects(authentication.getName());
    }

    @PostMapping("/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createSubject(Authentication authentication,
                                             @Valid @RequestBody CreateSubjectRequest request) {
        return platformService.createSubject(authentication.getName(), request);
    }

    @PutMapping("/subjects/{subjectId}")
    public Map<String, Object> updateSubject(Authentication authentication,
                                             @PathVariable Long subjectId,
                                             @Valid @RequestBody CreateSubjectRequest request) {
        return platformService.updateSubject(authentication.getName(), subjectId, request);
    }

    @DeleteMapping("/subjects/{subjectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubject(Authentication authentication, @PathVariable Long subjectId) {
        platformService.deleteSubject(authentication.getName(), subjectId);
    }

    @GetMapping("/tests")
    public List<Map<String, Object>> listTests(Authentication authentication) {
        return platformService.listTests(authentication.getName());
    }

    @GetMapping("/tests/{testId}")
    public Map<String, Object> getTest(Authentication authentication, @PathVariable Long testId) {
        return platformService.getTestDetails(authentication.getName(), testId);
    }

    @PostMapping("/tests")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createTest(Authentication authentication,
                                          @Valid @RequestBody CreateTestRequest request) {
        return platformService.createTest(authentication.getName(), request);
    }

    @PutMapping("/tests/{testId}")
    public Map<String, Object> updateTest(Authentication authentication,
                                          @PathVariable Long testId,
                                          @Valid @RequestBody UpdateTestRequest request) {
        return platformService.updateTest(authentication.getName(), testId, request);
    }

    @DeleteMapping("/tests/{testId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTest(Authentication authentication, @PathVariable Long testId) {
        platformService.deleteTest(authentication.getName(), testId);
    }

    @PostMapping("/tests/{testId}/questions/manual")
    public Map<String, Object> addManualQuestion(Authentication authentication,
                                                 @PathVariable Long testId,
                                                 @Valid @RequestBody CreateManualQuestionRequest request) {
        return platformService.addManualQuestion(authentication.getName(), testId, request);
    }

    @PostMapping("/tests/{testId}/questions/ai")
    public Map<String, Object> addAiQuestions(Authentication authentication,
                                              @PathVariable Long testId,
                                              @Valid @RequestBody GenerateAiQuestionsRequest request) {
        return platformService.generateAiQuestions(authentication.getName(), testId, request);
    }

    @DeleteMapping("/questions/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(Authentication authentication, @PathVariable Long questionId) {
        platformService.deleteQuestion(authentication.getName(), questionId);
    }

    @PutMapping("/questions/{questionId}")
    public Map<String, Object> updateQuestion(Authentication authentication,
                                              @PathVariable Long questionId,
                                              @Valid @RequestBody UpdateQuestionRequest request) {
        return platformService.updateQuestion(authentication.getName(), questionId, request);
    }

    @GetMapping("/groups")
    public List<Map<String, Object>> listGroups(Authentication authentication) {
        return platformService.listGroups(authentication.getName());
    }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createGroup(Authentication authentication,
                                           @Valid @RequestBody CreateGroupRequest request) {
        return platformService.createGroup(authentication.getName(), request);
    }

    @PostMapping("/groups/{groupId}/members")
    public Map<String, Object> addGroupMember(Authentication authentication,
                                              @PathVariable Long groupId,
                                              @Valid @RequestBody AddGroupMemberRequest request) {
        return platformService.addGroupMember(authentication.getName(), groupId, request);
    }

    @DeleteMapping("/groups/{groupId}/members/{studentId}")
    public Map<String, Object> removeGroupMember(Authentication authentication,
                                                 @PathVariable Long groupId,
                                                 @PathVariable Long studentId) {
        return platformService.removeGroupMember(authentication.getName(), groupId, studentId);
    }

    @GetMapping("/students")
    public List<Map<String, Object>> listStudents(Authentication authentication) {
        return platformService.listStudentsForTeacher(authentication.getName());
    }

    @PostMapping("/assignments/student")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> assignToStudent(Authentication authentication,
                                               @Valid @RequestBody AssignToStudentRequest request) {
        return platformService.assignTestToStudent(authentication.getName(), request);
    }

    @PostMapping("/assignments/group")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> assignToGroup(Authentication authentication,
                                             @Valid @RequestBody AssignToGroupRequest request) {
        return platformService.assignTestToGroup(authentication.getName(), request);
    }

    @GetMapping("/assignments")
    public List<Map<String, Object>> listAssignments(Authentication authentication) {
        return platformService.listTeacherAssignments(authentication.getName());
    }

    @GetMapping("/reports/overview")
    public Map<String, Object> reportOverview(Authentication authentication) {
        return platformService.teacherOverview(authentication.getName());
    }

    @GetMapping("/reports/tests/{testId}")
    public List<Map<String, Object>> reportByTest(Authentication authentication, @PathVariable Long testId) {
        return platformService.reportByTest(authentication.getName(), testId);
    }
}
