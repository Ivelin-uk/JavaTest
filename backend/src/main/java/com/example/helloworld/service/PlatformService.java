package com.example.helloworld.service;

import com.example.helloworld.dto.platform.AddGroupMemberRequest;
import com.example.helloworld.dto.platform.AssignToGroupRequest;
import com.example.helloworld.dto.platform.AssignToStudentRequest;
import com.example.helloworld.dto.platform.CreateGroupRequest;
import com.example.helloworld.dto.platform.CreateManualQuestionRequest;
import com.example.helloworld.dto.platform.CreateSubjectRequest;
import com.example.helloworld.dto.platform.CreateTestRequest;
import com.example.helloworld.dto.platform.GenerateAiQuestionsRequest;
import com.example.helloworld.dto.platform.QuestionOptionInput;
import com.example.helloworld.dto.platform.SubmitAnswerRequest;
import com.example.helloworld.dto.platform.UpdateQuestionRequest;
import com.example.helloworld.dto.platform.UpdateTestRequest;
import com.example.helloworld.dto.platform.ViolationRequest;
import com.example.helloworld.exception.UserNotFoundException;
import com.example.helloworld.model.User;
import com.example.helloworld.repository.PlatformRepository;
import com.example.helloworld.repository.UserRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PlatformService {

    private final UserRepository userRepository;
    private final PlatformRepository platformRepository;
    private final AiQuestionGeneratorService aiQuestionGeneratorService;

    public PlatformService(
            UserRepository userRepository,
            PlatformRepository platformRepository,
            AiQuestionGeneratorService aiQuestionGeneratorService
    ) {
        this.userRepository = userRepository;
        this.platformRepository = platformRepository;
        this.aiQuestionGeneratorService = aiQuestionGeneratorService;
    }

    public List<Map<String, Object>> listSubjects(String login) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);
        return platformRepository.findSubjects(current.getId(), isAdmin(current));
    }

    @Transactional
    public Map<String, Object> createSubject(String login, CreateSubjectRequest request) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);

        String name = normalizeText(request.name(), 2, 120, "Името на предмета");
        String description = nullableTrim(request.description(), 255);

        Long id = platformRepository.createSubject(name, description, current.getId());
        return platformRepository.findSubjectById(id)
                .orElseThrow(() -> new IllegalStateException("Създаденият предмет не беше намерен."));
    }

    @Transactional
    public Map<String, Object> updateSubject(String login, Long subjectId, CreateSubjectRequest request) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);

        String name = normalizeText(request.name(), 2, 120, "Името на предмета");
        String description = nullableTrim(request.description(), 255);

        boolean updated = platformRepository.updateSubject(subjectId, name, description, current.getId(), isAdmin(current));
        if (!updated) {
            throw new IllegalArgumentException("Предметът не е намерен или нямаш права да го редактираш.");
        }

        return platformRepository.findSubjectById(subjectId)
                .orElseThrow(() -> new IllegalStateException("Обновеният предмет не беше намерен."));
    }

    @Transactional
    public void deleteSubject(String login, Long subjectId) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);

        boolean deleted = platformRepository.deleteSubject(subjectId, current.getId(), isAdmin(current));
        if (!deleted) {
            throw new IllegalArgumentException("Предметът не е намерен или нямаш права да го изтриеш.");
        }
    }

    public List<Map<String, Object>> listTests(String login) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);
        return platformRepository.findTests(current.getId(), isAdmin(current));
    }

    public Map<String, Object> getTestDetails(String login, Long testId) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);

        Map<String, Object> header = platformRepository.findTestHeader(testId, current.getId(), isAdmin(current))
                .orElseThrow(() -> new IllegalArgumentException("Тестът не е намерен или нямаш достъп."));

        List<Map<String, Object>> questions = platformRepository.findQuestionsByTest(testId);
        List<Map<String, Object>> options = platformRepository.findOptionsByTest(testId);

        Map<Long, List<Map<String, Object>>> optionsByQuestion = new HashMap<>();
        for (Map<String, Object> option : options) {
            Long questionId = longValue(option.get("questionId"));
            optionsByQuestion.computeIfAbsent(questionId, k -> new ArrayList<>()).add(option);
        }

        for (Map<String, Object> question : questions) {
            Long questionId = longValue(question.get("id"));
            List<Map<String, Object>> questionOptions = optionsByQuestion.getOrDefault(questionId, List.of());
            question.put("options", questionOptions);
        }

        Map<String, Object> response = new LinkedHashMap<>(header);
        response.put("questions", questions);
        response.put("questionsCount", questions.size());
        return response;
    }

    @Transactional
    public Map<String, Object> createTest(String login, CreateTestRequest request) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);

        String title = normalizeText(request.title(), 3, 160, "Заглавието на теста");
        String description = nullableLongText(request.description());
        Long subjectId = requireId(request.subjectId(), "Subject ID");
        int timeLimit = normalizeTimeLimit(request.timeLimitMinutes());

        if (!platformRepository.subjectBelongsToTeacher(subjectId, current.getId(), isAdmin(current))) {
            throw new IllegalArgumentException("Предметът не съществува или нямаш достъп до него.");
        }

        Long testId = platformRepository.createTest(title, description, subjectId, current.getId(), timeLimit);
        return getTestDetails(login, testId);
    }

    @Transactional
    public Map<String, Object> updateTest(String login, Long testId, UpdateTestRequest request) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);

        String title = normalizeText(request.title(), 3, 160, "Заглавието на теста");
        String description = nullableLongText(request.description());
        Long subjectId = requireId(request.subjectId(), "Subject ID");
        int timeLimit = normalizeTimeLimit(request.timeLimitMinutes());

        if (!platformRepository.subjectBelongsToTeacher(subjectId, current.getId(), isAdmin(current))) {
            throw new IllegalArgumentException("Предметът не съществува или нямаш достъп до него.");
        }

        boolean updated = platformRepository.updateTest(
                testId,
                title,
                description,
                subjectId,
                timeLimit,
                Boolean.TRUE.equals(request.active()),
                current.getId(),
                isAdmin(current)
        );

        if (!updated) {
            throw new IllegalArgumentException("Тестът не е намерен или нямаш достъп.");
        }

        return getTestDetails(login, testId);
    }

    @Transactional
    public void deleteTest(String login, Long testId) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);

        boolean deleted = platformRepository.deleteTest(testId, current.getId(), isAdmin(current));
        if (!deleted) {
            throw new IllegalArgumentException("Тестът не е намерен или нямаш достъп.");
        }
    }

    @Transactional
    public Map<String, Object> addManualQuestion(String login, Long testId, CreateManualQuestionRequest request) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);

        if (!platformRepository.testBelongsToTeacher(testId, current.getId(), isAdmin(current))) {
            throw new IllegalArgumentException("Тестът не е намерен или нямаш достъп.");
        }

        String questionText = normalizeText(request.questionText(), 5, 5000, "Текстът на въпроса");
        BigDecimal points = BigDecimal.valueOf(request.points() == null ? 1.0 : request.points());
        int timeLimitSeconds = normalizeQuestionTimeLimitSeconds(request.timeLimitSeconds());
        List<QuestionOptionInput> options = request.options() == null ? List.of() : request.options();

        if (options.size() < 2) {
            throw new IllegalArgumentException("Трябва да има поне 2 отговора.");
        }

        long correctCount = options.stream().filter(option -> Boolean.TRUE.equals(option.correct())).count();
        if (correctCount == 0) {
            throw new IllegalArgumentException("Поне един отговор трябва да е правилен.");
        }

        int position = platformRepository.nextQuestionPosition(testId);
        Long questionId = platformRepository.createQuestion(testId, questionText, "MANUAL", points, timeLimitSeconds, position);

        int optionPosition = 1;
        for (QuestionOptionInput option : options) {
            String optionText = normalizeText(option.text(), 1, 500, "Текстът на отговор");
            platformRepository.createQuestionOption(questionId, optionText, Boolean.TRUE.equals(option.correct()), optionPosition);
            optionPosition++;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Въпросът е добавен успешно.");
        response.put("questionId", questionId);
        response.put("test", getTestDetails(login, testId));
        return response;
    }

    @Transactional
    public Map<String, Object> updateQuestion(String login, Long questionId, UpdateQuestionRequest request) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);

        Map<String, Object> existingQuestion = platformRepository.findQuestionById(questionId, current.getId(), isAdmin(current))
                .orElseThrow(() -> new IllegalArgumentException("Въпросът не е намерен или нямаш достъп."));

        String questionText = normalizeText(request.questionText(), 5, 5000, "Текстът на въпроса");
        BigDecimal points = BigDecimal.valueOf(request.points() == null ? 1.0 : request.points());
        int timeLimitSeconds = normalizeQuestionTimeLimitSeconds(request.timeLimitSeconds());
        List<QuestionOptionInput> options = request.options() == null ? List.of() : request.options();

        if (options.size() < 2) {
            throw new IllegalArgumentException("Трябва да има поне 2 отговора.");
        }

        long correctCount = options.stream().filter(option -> Boolean.TRUE.equals(option.correct())).count();
        if (correctCount == 0) {
            throw new IllegalArgumentException("Поне един отговор трябва да е правилен.");
        }

        boolean updated = platformRepository.updateQuestion(
                questionId,
                questionText,
                points,
                timeLimitSeconds,
                current.getId(),
                isAdmin(current)
        );

        if (!updated) {
            throw new IllegalArgumentException("Въпросът не е намерен или нямаш достъп.");
        }

        platformRepository.deleteQuestionOptions(questionId);
        int optionPosition = 1;
        for (QuestionOptionInput option : options) {
            String optionText = normalizeText(option.text(), 1, 500, "Текстът на отговор");
            platformRepository.createQuestionOption(questionId, optionText, Boolean.TRUE.equals(option.correct()), optionPosition);
            optionPosition++;
        }

        Long testId = longValue(existingQuestion.get("testId"));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Въпросът е обновен успешно.");
        response.put("questionId", questionId);
        response.put("test", getTestDetails(login, testId));
        return response;
    }

    @Transactional
    public Map<String, Object> generateAiQuestions(String login, Long testId, GenerateAiQuestionsRequest request) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);

        if (!platformRepository.testBelongsToTeacher(testId, current.getId(), isAdmin(current))) {
            throw new IllegalArgumentException("Тестът не е намерен или нямаш достъп.");
        }

        String topic = normalizeText(request.topic(), 2, 180, "Темата");
        String difficulty = nullableTrim(request.difficulty(), 30);
        int count = request.count() == null ? 3 : Math.max(1, Math.min(20, request.count()));
        int timeLimitSeconds = normalizeQuestionTimeLimitSeconds(request.timeLimitSeconds());

        List<AiQuestionGeneratorService.GeneratedQuestion> generatedQuestions =
                aiQuestionGeneratorService.generate(topic, difficulty, count);

        int position = platformRepository.nextQuestionPosition(testId);
        int created = 0;

        for (AiQuestionGeneratorService.GeneratedQuestion generatedQuestion : generatedQuestions) {
            Long questionId = platformRepository.createQuestion(
                    testId,
                    generatedQuestion.questionText(),
                    "AI",
                    BigDecimal.valueOf(generatedQuestion.points()),
                    timeLimitSeconds,
                    position
            );

            int optionPosition = 1;
            for (int i = 0; i < generatedQuestion.options().size(); i++) {
                boolean correct = i == generatedQuestion.correctIndex();
                platformRepository.createQuestionOption(
                        questionId,
                        generatedQuestion.options().get(i),
                        correct,
                        optionPosition
                );
                optionPosition++;
            }

            position++;
            created++;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "AI генерира " + created + " въпроса.");
        response.put("created", created);
        response.put("test", getTestDetails(login, testId));
        return response;
    }

    @Transactional
    public void deleteQuestion(String login, Long questionId) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);

        boolean deleted = platformRepository.deleteQuestion(questionId, current.getId(), isAdmin(current));
        if (!deleted) {
            throw new IllegalArgumentException("Въпросът не е намерен или нямаш достъп.");
        }
    }

    public List<Map<String, Object>> listGroups(String login) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);

        List<Map<String, Object>> groups = platformRepository.findGroups(current.getId(), isAdmin(current));
        for (Map<String, Object> group : groups) {
            Long groupId = longValue(group.get("id"));
            group.put("members", platformRepository.findGroupMembers(groupId));
        }
        return groups;
    }

    @Transactional
    public Map<String, Object> createGroup(String login, CreateGroupRequest request) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);

        String groupName = normalizeText(request.name(), 2, 120, "Името на групата");
        Long groupId = platformRepository.createGroup(groupName, current.getId());

        Map<String, Object> group = platformRepository.findGroupHeader(groupId, current.getId(), isAdmin(current))
                .orElseThrow(() -> new IllegalStateException("Създадената група не беше намерена."));
        group.put("members", List.of());
        return group;
    }

    @Transactional
    public Map<String, Object> addGroupMember(String login, Long groupId, AddGroupMemberRequest request) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);

        platformRepository.findGroupHeader(groupId, current.getId(), isAdmin(current))
                .orElseThrow(() -> new IllegalArgumentException("Групата не е намерена или нямаш достъп."));

        Long studentId = requireId(request.studentId(), "Student ID");
        if (!platformRepository.userHasRole(studentId, "STUDENT")) {
            throw new IllegalArgumentException("Потребителят не е ученик.");
        }

        platformRepository.addGroupMember(groupId, studentId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("groupId", groupId);
        response.put("members", platformRepository.findGroupMembers(groupId));
        return response;
    }

    @Transactional
    public Map<String, Object> removeGroupMember(String login, Long groupId, Long studentId) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);

        platformRepository.findGroupHeader(groupId, current.getId(), isAdmin(current))
                .orElseThrow(() -> new IllegalArgumentException("Групата не е намерена или нямаш достъп."));

        platformRepository.removeGroupMember(groupId, studentId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("groupId", groupId);
        response.put("members", platformRepository.findGroupMembers(groupId));
        return response;
    }

    public List<Map<String, Object>> listStudentsForTeacher(String login) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);

        List<Map<String, Object>> rows = platformRepository.findUsersByRole("STUDENT");
        rows.sort(Comparator.comparing(row -> String.valueOf(row.get("name")), String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    @Transactional
    public Map<String, Object> assignTestToStudent(String login, AssignToStudentRequest request) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);

        Long testId = requireId(request.testId(), "Test ID");
        Long studentId = requireId(request.studentId(), "Student ID");

        if (!platformRepository.testBelongsToTeacher(testId, current.getId(), isAdmin(current))) {
            throw new IllegalArgumentException("Тестът не е намерен или нямаш достъп.");
        }
        if (!platformRepository.userHasRole(studentId, "STUDENT")) {
            throw new IllegalArgumentException("Избраният потребител не е ученик.");
        }

        Timestamp dueAt = platformRepository.parseDueAt(request.dueAt());
        Long assignmentId = platformRepository.assignTestToStudent(testId, studentId, current.getId(), null, dueAt);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Тестът е зададен успешно.");
        response.put("assignmentId", assignmentId);
        return response;
    }

    @Transactional
    public Map<String, Object> assignTestToGroup(String login, AssignToGroupRequest request) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);

        Long testId = requireId(request.testId(), "Test ID");
        Long groupId = requireId(request.groupId(), "Group ID");

        if (!platformRepository.testBelongsToTeacher(testId, current.getId(), isAdmin(current))) {
            throw new IllegalArgumentException("Тестът не е намерен или нямаш достъп.");
        }

        platformRepository.findGroupHeader(groupId, current.getId(), isAdmin(current))
                .orElseThrow(() -> new IllegalArgumentException("Групата не е намерена или нямаш достъп."));

        List<Long> studentIds = platformRepository.findStudentIdsByGroup(groupId, current.getId(), isAdmin(current));
        if (studentIds.isEmpty()) {
            throw new IllegalArgumentException("Групата няма ученици.");
        }

        Timestamp dueAt = platformRepository.parseDueAt(request.dueAt());
        int created = 0;
        for (Long studentId : studentIds) {
            platformRepository.assignTestToStudent(testId, studentId, current.getId(), groupId, dueAt);
            created++;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Тестът е зададен към групата.");
        response.put("createdAssignments", created);
        return response;
    }

    public List<Map<String, Object>> listTeacherAssignments(String login) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);
        return platformRepository.findAssignmentsForTeacher(current.getId(), isAdmin(current));
    }

    public Map<String, Object> teacherOverview(String login) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);

        Map<String, Object> summary = new LinkedHashMap<>(platformRepository.findTeacherOverview(current.getId(), isAdmin(current)));
        summary.put("generatedAt", Instant.now().toString());
        return summary;
    }

    public List<Map<String, Object>> reportByTest(String login, Long testId) {
        User current = requireUser(login);
        ensureTeacherOrAdmin(current);

        if (!platformRepository.testBelongsToTeacher(testId, current.getId(), isAdmin(current))) {
            throw new IllegalArgumentException("Тестът не е намерен или нямаш достъп.");
        }
        return platformRepository.findScoresByTest(testId, current.getId(), isAdmin(current));
    }

    public List<Map<String, Object>> listStudentAssignments(String login) {
        User current = requireUser(login);
        ensureStudent(current);
        return platformRepository.findAssignmentsForStudent(current.getId());
    }

    @Transactional
    public Map<String, Object> startAttempt(String login, Long assignmentId) {
        User current = requireUser(login);
        ensureStudent(current);

        Map<String, Object> assignment = platformRepository.findAssignmentForStudent(assignmentId, current.getId())
                .orElseThrow(() -> new IllegalArgumentException("Заданието не е намерено."));

        if (!Boolean.TRUE.equals(assignment.get("testActive"))) {
            throw new IllegalArgumentException("Тестът е неактивен.");
        }

        Long testId = longValue(assignment.get("testId"));
        int totalQuestions = platformRepository.countQuestions(testId);
        if (totalQuestions == 0) {
            throw new IllegalArgumentException("Тестът няма въпроси.");
        }

        Optional<Map<String, Object>> existingAttempt = platformRepository.findAttemptByAssignmentAndStudent(assignmentId, current.getId());
        Long attemptId;
        String status;

        if (existingAttempt.isPresent()) {
            attemptId = longValue(existingAttempt.get().get("id"));
            status = String.valueOf(existingAttempt.get().get("status"));
        } else {
            BigDecimal totalPoints = platformRepository.sumTestPoints(testId);
            attemptId = platformRepository.createAttempt(assignmentId, current.getId(), totalPoints);
            status = "IN_PROGRESS";
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("attemptId", attemptId);
        response.put("status", status);
        response.put("current", getCurrentQuestion(login, attemptId));
        return response;
    }

    public Map<String, Object> getCurrentQuestion(String login, Long attemptId) {
        User current = requireUser(login);
        ensureStudent(current);

        int safetyCounter = 0;
        while (safetyCounter < 100) {
            safetyCounter++;
            Map<String, Object> attempt = platformRepository.findAttemptByIdForStudent(attemptId, current.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Опитът не е намерен."));

            String status = String.valueOf(attempt.get("status"));
            if ("COMPLETED".equalsIgnoreCase(status)) {
                Map<String, Object> completed = new LinkedHashMap<>();
                completed.put("completed", true);
                completed.put("result", buildAttemptResult(attemptId, current.getId()));
                return completed;
            }

            Long testId = longValue(attempt.get("testId"));
            int currentPosition = intValue(attempt.get("currentPosition"));
            int totalQuestions = platformRepository.countQuestions(testId);

            Optional<Map<String, Object>> currentQuestionOpt = platformRepository.findCurrentQuestion(testId, currentPosition);
            if (currentQuestionOpt.isEmpty()) {
                platformRepository.completeAttempt(attemptId);
                Map<String, Object> completed = new LinkedHashMap<>();
                completed.put("completed", true);
                completed.put("result", buildAttemptResult(attemptId, current.getId()));
                return completed;
            }

            Map<String, Object> question = new LinkedHashMap<>(currentQuestionOpt.get());
            Long questionId = longValue(question.get("id"));
            int remainingSeconds = calculateRemainingSeconds(attempt, question);

            if (remainingSeconds <= 0 && !platformRepository.hasAnswerForQuestion(attemptId, questionId)) {
                Long answerId = platformRepository.insertAttemptAnswer(
                        attemptId,
                        questionId,
                        null,
                        false,
                        BigDecimal.ZERO,
                        "TIME_EXPIRED"
                );
                platformRepository.insertAttemptAnswerOptions(answerId, List.of());
                platformRepository.updateAttemptProgress(attemptId, currentPosition + 1, BigDecimal.ZERO, 1);

                if (currentPosition >= totalQuestions) {
                    platformRepository.completeAttempt(attemptId);
                    Map<String, Object> completed = new LinkedHashMap<>();
                    completed.put("completed", true);
                    completed.put("result", buildAttemptResult(attemptId, current.getId()));
                    return completed;
                }
                continue;
            }

            List<Map<String, Object>> options = platformRepository.findOptionsByQuestion(questionId);
            List<Map<String, Object>> safeOptions = new ArrayList<>();
            for (Map<String, Object> option : options) {
                Map<String, Object> safe = new LinkedHashMap<>();
                safe.put("id", option.get("id"));
                safe.put("questionId", option.get("questionId"));
                safe.put("optionText", option.get("optionText"));
                safe.put("positionIndex", option.get("positionIndex"));
                safeOptions.add(safe);
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("completed", false);
            payload.put("attemptId", attemptId);
            payload.put("testId", testId);
            payload.put("testTitle", attempt.get("testTitle"));
            payload.put("currentPosition", currentPosition);
            payload.put("totalQuestions", totalQuestions);
            payload.put("question", question);
            payload.put("options", safeOptions);
            payload.put("remainingSeconds", remainingSeconds);
            payload.put("violationsCount", attempt.get("violationsCount"));
            return payload;
        }

        throw new IllegalStateException("Неуспешно извличане на текущ въпрос.");
    }

    @Transactional
    public Map<String, Object> submitAnswer(String login, Long attemptId, SubmitAnswerRequest request) {
        User current = requireUser(login);
        ensureStudent(current);

        Map<String, Object> attempt = platformRepository.findAttemptByIdForStudent(attemptId, current.getId())
                .orElseThrow(() -> new IllegalArgumentException("Опитът не е намерен."));

        if ("COMPLETED".equalsIgnoreCase(String.valueOf(attempt.get("status")))) {
            return buildAttemptResult(attemptId, current.getId());
        }

        Long testId = longValue(attempt.get("testId"));
        int currentPosition = intValue(attempt.get("currentPosition"));

        Map<String, Object> question = platformRepository.findCurrentQuestion(testId, currentPosition)
                .orElseThrow(() -> new IllegalArgumentException("Няма активен въпрос."));

        Long questionId = longValue(question.get("id"));
        if (platformRepository.hasAnswerForQuestion(attemptId, questionId)) {
            throw new IllegalArgumentException("На този въпрос вече е отговорено.");
        }

        int remainingSeconds = calculateRemainingSeconds(attempt, question);
        if (remainingSeconds <= 0) {
            Long answerId = platformRepository.insertAttemptAnswer(
                    attemptId,
                    questionId,
                    null,
                    false,
                    BigDecimal.ZERO,
                    "TIME_EXPIRED"
            );
            platformRepository.insertAttemptAnswerOptions(answerId, List.of());
            platformRepository.updateAttemptProgress(attemptId, currentPosition + 1, BigDecimal.ZERO, 1);

            if (currentPosition >= platformRepository.countQuestions(testId)) {
                platformRepository.completeAttempt(attemptId);
                return buildAttemptResult(attemptId, current.getId());
            }
            return getCurrentQuestion(login, attemptId);
        }

        List<Map<String, Object>> questionOptions = platformRepository.findOptionsByQuestion(questionId);
        Set<Long> allOptionIds = new LinkedHashSet<>();
        Set<Long> correctOptionIds = new LinkedHashSet<>();
        for (Map<String, Object> option : questionOptions) {
            Long id = longValue(option.get("id"));
            allOptionIds.add(id);
            if (Boolean.TRUE.equals(option.get("correct"))) {
                correctOptionIds.add(id);
            }
        }

        List<Long> selectedOptionIds = normalizeSelectedOptionIds(request);
        for (Long selectedOptionId : selectedOptionIds) {
            if (!allOptionIds.contains(selectedOptionId)) {
                throw new IllegalArgumentException("Невалиден отговор за текущия въпрос.");
            }
        }

        Set<Long> selectedSet = new LinkedHashSet<>(selectedOptionIds);
        boolean correct = !selectedSet.isEmpty() && selectedSet.equals(correctOptionIds);
        BigDecimal earned = correct ? decimalValue(question.get("points")) : BigDecimal.ZERO;
        Long legacySelectedOptionId = selectedSet.size() == 1 ? selectedSet.iterator().next() : null;

        Long answerId = platformRepository.insertAttemptAnswer(
                attemptId,
                questionId,
                legacySelectedOptionId,
                correct,
                earned,
                null
        );
        platformRepository.insertAttemptAnswerOptions(answerId, selectedOptionIds);
        platformRepository.updateAttemptProgress(attemptId, currentPosition + 1, earned, 0);

        if (currentPosition >= platformRepository.countQuestions(testId)) {
            platformRepository.completeAttempt(attemptId);
            return buildAttemptResult(attemptId, current.getId());
        }

        return getCurrentQuestion(login, attemptId);
    }

    @Transactional
    public Map<String, Object> reportViolation(String login, Long attemptId, ViolationRequest request) {
        User current = requireUser(login);
        ensureStudent(current);

        Map<String, Object> attempt = platformRepository.findAttemptByIdForStudent(attemptId, current.getId())
                .orElseThrow(() -> new IllegalArgumentException("Опитът не е намерен."));

        if ("COMPLETED".equalsIgnoreCase(String.valueOf(attempt.get("status")))) {
            return buildAttemptResult(attemptId, current.getId());
        }

        Long testId = longValue(attempt.get("testId"));
        int currentPosition = intValue(attempt.get("currentPosition"));
        Map<String, Object> question = platformRepository.findCurrentQuestion(testId, currentPosition)
                .orElseThrow(() -> new IllegalArgumentException("Няма активен въпрос."));

        Long questionId = longValue(question.get("id"));
        if (!platformRepository.hasAnswerForQuestion(attemptId, questionId)) {
            String violationReason = normalizeViolationReason(request == null ? null : request.reason());
            Long answerId = platformRepository.insertAttemptAnswer(
                    attemptId,
                    questionId,
                    null,
                    false,
                    BigDecimal.ZERO,
                    violationReason
            );
            platformRepository.insertAttemptAnswerOptions(answerId, List.of());
            platformRepository.updateAttemptProgress(attemptId, currentPosition + 1, BigDecimal.ZERO, 1);
        }

        if (currentPosition >= platformRepository.countQuestions(testId)) {
            platformRepository.completeAttempt(attemptId);
            return buildAttemptResult(attemptId, current.getId());
        }

        return getCurrentQuestion(login, attemptId);
    }

    public Map<String, Object> getAttemptResult(String login, Long attemptId) {
        User current = requireUser(login);
        ensureStudent(current);
        return buildAttemptResult(attemptId, current.getId());
    }

    public List<Map<String, Object>> listStudentResults(String login) {
        User current = requireUser(login);
        ensureStudent(current);

        List<Map<String, Object>> assignments = platformRepository.findAssignmentsForStudent(current.getId());
        List<Map<String, Object>> completed = new ArrayList<>();
        for (Map<String, Object> assignment : assignments) {
            Object status = assignment.get("attemptStatus");
            if (status != null && "COMPLETED".equalsIgnoreCase(String.valueOf(status))) {
                completed.add(assignment);
            }
        }
        return completed;
    }

    private Map<String, Object> buildAttemptResult(Long attemptId, Long studentId) {
        Map<String, Object> attempt = platformRepository.findAttemptByIdForStudent(attemptId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("Опитът не е намерен."));

        List<Map<String, Object>> answers = platformRepository.findAttemptAnswers(attemptId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attemptId", attempt.get("id"));
        result.put("assignmentId", attempt.get("assignmentId"));
        result.put("testId", attempt.get("testId"));
        result.put("testTitle", attempt.get("testTitle"));
        result.put("status", attempt.get("status"));
        result.put("earnedPoints", attempt.get("earnedPoints"));
        result.put("totalPoints", attempt.get("totalPoints"));
        result.put("scorePercent", attempt.get("scorePercent"));
        result.put("violationsCount", attempt.get("violationsCount"));
        result.put("startTime", attempt.get("startTime"));
        result.put("endTime", attempt.get("endTime"));
        result.put("answers", answers);
        return result;
    }

    private User requireUser(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new UserNotFoundException("Потребителят не е намерен."));
    }

    private void ensureTeacherOrAdmin(User user) {
        String role = normalizeRole(user.getRole());
        if (!"TEACHER".equals(role) && !"ADMIN".equals(role)) {
            throw new IllegalArgumentException("Достъпът е разрешен само за преподавател или администратор.");
        }
    }

    private void ensureStudent(User user) {
        String role = normalizeRole(user.getRole());
        if (!"STUDENT".equals(role)) {
            throw new IllegalArgumentException("Достъпът е разрешен само за ученик.");
        }
    }

    private boolean isAdmin(User user) {
        return "ADMIN".equals(normalizeRole(user.getRole()));
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "";
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value, int minLen, int maxLen, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " е задължително.");
        }
        String normalized = value.trim();
        if (normalized.length() < minLen || normalized.length() > maxLen) {
            throw new IllegalArgumentException(fieldName + " трябва да е между " + minLen + " и " + maxLen + " символа.");
        }
        return normalized;
    }

    private String nullableTrim(String value, int maxLen) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLen) {
            throw new IllegalArgumentException("Стойността е твърде дълга.");
        }
        return normalized;
    }

    private String nullableLongText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > 10000) {
            throw new IllegalArgumentException("Описанието е твърде дълго.");
        }
        return normalized;
    }

    private int normalizeTimeLimit(Integer value) {
        int normalized = value == null ? 30 : value;
        if (normalized < 1 || normalized > 300) {
            throw new IllegalArgumentException("Времето за тест трябва да е между 1 и 300 минути.");
        }
        return normalized;
    }

    private int normalizeQuestionTimeLimitSeconds(Integer value) {
        int normalized = value == null ? 60 : value;
        if (normalized < 5 || normalized > 3600) {
            throw new IllegalArgumentException("Времето за въпрос трябва да е между 5 и 3600 секунди.");
        }
        return normalized;
    }

    private String normalizeViolationReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return "TAB_SWITCH";
        }
        String normalized = reason.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > 120) {
            normalized = normalized.substring(0, 120);
        }
        return normalized;
    }

    private Long requireId(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " е невалиден.");
        }
        return value;
    }

    private List<Long> normalizeSelectedOptionIds(SubmitAnswerRequest request) {
        if (request == null) {
            return List.of();
        }

        Set<Long> unique = new LinkedHashSet<>();
        if (request.optionId() != null && request.optionId() > 0) {
            unique.add(request.optionId());
        }
        if (request.optionIds() != null) {
            for (Long optionId : request.optionIds()) {
                if (optionId != null && optionId > 0) {
                    unique.add(optionId);
                }
            }
        }
        return new ArrayList<>(unique);
    }

    private int calculateRemainingSeconds(Map<String, Object> attempt, Map<String, Object> question) {
        int timeLimitSeconds = intValue(question.get("timeLimitSeconds"));
        if (timeLimitSeconds <= 0) {
            return 0;
        }

        Timestamp startedAt = timestampValue(attempt.get("currentQuestionStartedAt"));
        if (startedAt == null) {
            return timeLimitSeconds;
        }

        long elapsed = Duration.between(startedAt.toInstant(), Instant.now()).getSeconds();
        int remaining = timeLimitSeconds - (int) elapsed;
        return Math.max(0, remaining);
    }

    private Timestamp timestampValue(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp;
        }
        return null;
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(String.valueOf(value));
    }
}
