package com.example.helloworld.repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PlatformRepository {

    private final JdbcTemplate jdbcTemplate;

    public PlatformRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> findSubjects(Long currentUserId, boolean admin) {
        String sql = """
                SELECT s.id,
                       s.name,
                       s.description,
                       s.teacher_id AS teacherId,
                       u.name AS teacherName,
                       s.created_at AS createdAt
                FROM subjects s
                JOIN users u ON u.id = s.teacher_id
                WHERE (? = 1 OR s.teacher_id = ?)
                ORDER BY s.created_at DESC
                """;
        return jdbcTemplate.queryForList(sql, admin ? 1 : 0, currentUserId);
    }

    public Optional<Map<String, Object>> findSubjectById(Long subjectId) {
        String sql = """
                SELECT s.id,
                       s.name,
                       s.description,
                       s.teacher_id AS teacherId,
                       u.name AS teacherName,
                       s.created_at AS createdAt
                FROM subjects s
                JOIN users u ON u.id = s.teacher_id
                WHERE s.id = ?
                """;
        return findOne(sql, subjectId);
    }

    public Long createSubject(String name, String description, Long teacherId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO subjects (name, description, teacher_id) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setLong(3, teacherId);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Неуспешно създаване на предмет.");
        }
        return key.longValue();
    }

    public boolean updateSubject(Long subjectId, String name, String description, Long currentUserId, boolean admin) {
        String sql = """
                UPDATE subjects
                SET name = ?, description = ?
                WHERE id = ?
                  AND (? = 1 OR teacher_id = ?)
                """;
        int rows = jdbcTemplate.update(sql, name, description, subjectId, admin ? 1 : 0, currentUserId);
        return rows > 0;
    }

    public boolean deleteSubject(Long subjectId, Long currentUserId, boolean admin) {
        String sql = """
                DELETE FROM subjects
                WHERE id = ?
                  AND (? = 1 OR teacher_id = ?)
                """;
        int rows = jdbcTemplate.update(sql, subjectId, admin ? 1 : 0, currentUserId);
        return rows > 0;
    }

    public List<Map<String, Object>> findTests(Long currentUserId, boolean admin) {
        String sql = """
                SELECT t.id,
                       t.title,
                       t.description,
                       t.subject_id AS subjectId,
                       s.name AS subjectName,
                       t.teacher_id AS teacherId,
                       u.name AS teacherName,
                       t.time_limit_minutes AS timeLimitMinutes,
                       t.is_active AS active,
                       t.created_at AS createdAt,
                       t.updated_at AS updatedAt,
                       (SELECT COUNT(*) FROM questions q WHERE q.test_id = t.id) AS questionsCount
                FROM tests t
                JOIN subjects s ON s.id = t.subject_id
                JOIN users u ON u.id = t.teacher_id
                WHERE (? = 1 OR t.teacher_id = ?)
                ORDER BY t.created_at DESC
                """;
        return jdbcTemplate.queryForList(sql, admin ? 1 : 0, currentUserId);
    }

    public Optional<Map<String, Object>> findTestHeader(Long testId, Long currentUserId, boolean admin) {
        String sql = """
                SELECT t.id,
                       t.title,
                       t.description,
                       t.subject_id AS subjectId,
                       s.name AS subjectName,
                       t.teacher_id AS teacherId,
                       u.name AS teacherName,
                       t.time_limit_minutes AS timeLimitMinutes,
                       t.is_active AS active,
                       t.created_at AS createdAt,
                       t.updated_at AS updatedAt
                FROM tests t
                JOIN subjects s ON s.id = t.subject_id
                JOIN users u ON u.id = t.teacher_id
                WHERE t.id = ?
                  AND (? = 1 OR t.teacher_id = ?)
                """;
        return findOne(sql, testId, admin ? 1 : 0, currentUserId);
    }

    public Long createTest(String title, String description, Long subjectId, Long teacherId, int timeLimitMinutes) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO tests (title, description, subject_id, teacher_id, time_limit_minutes, is_active) VALUES (?, ?, ?, ?, ?, 1)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, title);
            ps.setString(2, description);
            ps.setLong(3, subjectId);
            ps.setLong(4, teacherId);
            ps.setInt(5, timeLimitMinutes);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Неуспешно създаване на тест.");
        }
        return key.longValue();
    }

    public boolean updateTest(Long testId, String title, String description, Long subjectId, int timeLimitMinutes,
                              boolean active, Long currentUserId, boolean admin) {
        String sql = """
                UPDATE tests
                SET title = ?, description = ?, subject_id = ?, time_limit_minutes = ?, is_active = ?
                WHERE id = ?
                  AND (? = 1 OR teacher_id = ?)
                """;
        int rows = jdbcTemplate.update(
                sql,
                title,
                description,
                subjectId,
                timeLimitMinutes,
                active,
                testId,
                admin ? 1 : 0,
                currentUserId
        );
        return rows > 0;
    }

    public boolean deleteTest(Long testId, Long currentUserId, boolean admin) {
        String sql = """
                DELETE FROM tests
                WHERE id = ?
                  AND (? = 1 OR teacher_id = ?)
                """;
        int rows = jdbcTemplate.update(sql, testId, admin ? 1 : 0, currentUserId);
        return rows > 0;
    }

    public int nextQuestionPosition(Long testId) {
        Integer maxPosition = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(position_index), 0) FROM questions WHERE test_id = ?",
                Integer.class,
                testId
        );
        return (maxPosition == null ? 0 : maxPosition) + 1;
    }

    public Long createQuestion(Long testId, String questionText, String sourceType, BigDecimal points, int timeLimitSeconds, int positionIndex) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO questions (test_id, question_text, source_type, points, time_limit_seconds, position_index) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, testId);
            ps.setString(2, questionText);
            ps.setString(3, sourceType);
            ps.setBigDecimal(4, points);
            ps.setInt(5, timeLimitSeconds);
            ps.setInt(6, positionIndex);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Неуспешно създаване на въпрос.");
        }
        return key.longValue();
    }

    public void createQuestionOption(Long questionId, String optionText, boolean correct, int positionIndex) {
        jdbcTemplate.update(
                "INSERT INTO question_options (question_id, option_text, is_correct, position_index) VALUES (?, ?, ?, ?)",
                questionId,
                optionText,
                correct,
                positionIndex
        );
    }

    public List<Map<String, Object>> findQuestionsByTest(Long testId) {
        String sql = """
                SELECT q.id,
                       q.test_id AS testId,
                       q.question_text AS questionText,
                       q.source_type AS sourceType,
                       q.points,
                       q.time_limit_seconds AS timeLimitSeconds,
                       q.position_index AS positionIndex
                FROM questions q
                WHERE q.test_id = ?
                ORDER BY q.position_index
                """;
        return jdbcTemplate.queryForList(sql, testId);
    }

    public List<Map<String, Object>> findOptionsByTest(Long testId) {
        String sql = """
                SELECT o.id,
                       o.question_id AS questionId,
                       o.option_text AS optionText,
                       o.is_correct AS correct,
                       o.position_index AS positionIndex
                FROM question_options o
                JOIN questions q ON q.id = o.question_id
                WHERE q.test_id = ?
                ORDER BY o.question_id, o.position_index
                """;
        return jdbcTemplate.queryForList(sql, testId);
    }

    public boolean deleteQuestion(Long questionId, Long currentUserId, boolean admin) {
        String sql = """
                DELETE q
                FROM questions q
                JOIN tests t ON t.id = q.test_id
                WHERE q.id = ?
                  AND (? = 1 OR t.teacher_id = ?)
                """;
        int rows = jdbcTemplate.update(sql, questionId, admin ? 1 : 0, currentUserId);
        return rows > 0;
    }

    public Optional<Map<String, Object>> findQuestionById(Long questionId, Long currentUserId, boolean admin) {
        String sql = """
                SELECT q.id,
                       q.test_id AS testId,
                       q.question_text AS questionText,
                       q.source_type AS sourceType,
                       q.points,
                       q.time_limit_seconds AS timeLimitSeconds,
                       q.position_index AS positionIndex
                FROM questions q
                JOIN tests t ON t.id = q.test_id
                WHERE q.id = ?
                  AND (? = 1 OR t.teacher_id = ?)
                """;
        return findOne(sql, questionId, admin ? 1 : 0, currentUserId);
    }

    public boolean updateQuestion(Long questionId, String questionText, BigDecimal points, int timeLimitSeconds,
                                  Long currentUserId, boolean admin) {
        String sql = """
                UPDATE questions q
                JOIN tests t ON t.id = q.test_id
                SET q.question_text = ?,
                    q.points = ?,
                    q.time_limit_seconds = ?
                WHERE q.id = ?
                  AND (? = 1 OR t.teacher_id = ?)
                """;
        int rows = jdbcTemplate.update(
                sql,
                questionText,
                points,
                timeLimitSeconds,
                questionId,
                admin ? 1 : 0,
                currentUserId
        );
        return rows > 0;
    }

    public void deleteQuestionOptions(Long questionId) {
        jdbcTemplate.update("DELETE FROM question_options WHERE question_id = ?", questionId);
    }

    public List<Map<String, Object>> findGroups(Long currentUserId, boolean admin) {
        String sql = """
                SELECT g.id,
                       g.group_name AS groupName,
                       g.teacher_id AS teacherId,
                       u.name AS teacherName,
                       g.created_at AS createdAt,
                       (SELECT COUNT(*) FROM group_members gm WHERE gm.group_id = g.id) AS membersCount
                FROM student_groups g
                JOIN users u ON u.id = g.teacher_id
                WHERE (? = 1 OR g.teacher_id = ?)
                ORDER BY g.created_at DESC
                """;
        return jdbcTemplate.queryForList(sql, admin ? 1 : 0, currentUserId);
    }

    public Optional<Map<String, Object>> findGroupHeader(Long groupId, Long currentUserId, boolean admin) {
        String sql = """
                SELECT g.id,
                       g.group_name AS groupName,
                       g.teacher_id AS teacherId,
                       u.name AS teacherName,
                       g.created_at AS createdAt
                FROM student_groups g
                JOIN users u ON u.id = g.teacher_id
                WHERE g.id = ?
                  AND (? = 1 OR g.teacher_id = ?)
                """;
        return findOne(sql, groupId, admin ? 1 : 0, currentUserId);
    }

    public Long createGroup(String groupName, Long teacherId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO student_groups (group_name, teacher_id) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, groupName);
            ps.setLong(2, teacherId);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Неуспешно създаване на група.");
        }
        return key.longValue();
    }

    public void addGroupMember(Long groupId, Long studentId) {
        jdbcTemplate.update(
                "INSERT IGNORE INTO group_members (group_id, student_id) VALUES (?, ?)",
                groupId,
                studentId
        );
    }

    public boolean removeGroupMember(Long groupId, Long studentId) {
        int rows = jdbcTemplate.update(
                "DELETE FROM group_members WHERE group_id = ? AND student_id = ?",
                groupId,
                studentId
        );
        return rows > 0;
    }

    public List<Map<String, Object>> findGroupMembers(Long groupId) {
        String sql = """
                SELECT u.id,
                       u.username,
                       u.name,
                       u.email,
                       u.role,
                       COALESCE(u.is_active, 1) AS active
                FROM group_members gm
                JOIN users u ON u.id = gm.student_id
                WHERE gm.group_id = ?
                ORDER BY u.name
                """;
        return jdbcTemplate.queryForList(sql, groupId);
    }

    public List<Map<String, Object>> findUsersByRole(String role) {
        String sql = """
                SELECT id,
                       username,
                       name,
                       email,
                       role,
                       COALESCE(is_active, 1) AS active
                FROM users
                WHERE role = ?
                ORDER BY name
                """;
        return jdbcTemplate.queryForList(sql, role);
    }

    public boolean userHasRole(Long userId, String role) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ? AND role = ?",
                Integer.class,
                userId,
                role
        );
        return count != null && count > 0;
    }

    public Long assignTestToStudent(Long testId, Long studentId, Long assignedBy, Long groupId, Timestamp dueAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO test_assignments (test_id, student_id, group_id, assigned_by, due_at) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, testId);
            ps.setLong(2, studentId);
            if (groupId == null) {
                ps.setNull(3, java.sql.Types.BIGINT);
            } else {
                ps.setLong(3, groupId);
            }
            ps.setLong(4, assignedBy);
            if (dueAt == null) {
                ps.setNull(5, java.sql.Types.TIMESTAMP);
            } else {
                ps.setTimestamp(5, dueAt);
            }
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Неуспешно задаване на тест.");
        }
        return key.longValue();
    }

    public List<Long> findStudentIdsByGroup(Long groupId, Long currentUserId, boolean admin) {
        String sql = """
                SELECT gm.student_id
                FROM group_members gm
                JOIN student_groups g ON g.id = gm.group_id
                WHERE gm.group_id = ?
                  AND (? = 1 OR g.teacher_id = ?)
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, groupId, admin ? 1 : 0, currentUserId);
        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object value = row.get("student_id");
            if (value instanceof Number number) {
                ids.add(number.longValue());
            }
        }
        return ids;
    }

    public List<Map<String, Object>> findAssignmentsForTeacher(Long currentUserId, boolean admin) {
        String sql = """
                SELECT a.id,
                       a.test_id AS testId,
                       t.title AS testTitle,
                       a.student_id AS studentId,
                       su.name AS studentName,
                       su.username AS studentUsername,
                       a.group_id AS groupId,
                       g.group_name AS groupName,
                       a.assigned_by AS assignedBy,
                       au.name AS assignedByName,
                       a.due_at AS dueAt,
                       a.created_at AS createdAt,
                       at.status AS latestStatus,
                       at.score_percent AS latestScorePercent
                FROM test_assignments a
                JOIN tests t ON t.id = a.test_id
                JOIN users su ON su.id = a.student_id
                JOIN users au ON au.id = a.assigned_by
                LEFT JOIN student_groups g ON g.id = a.group_id
                LEFT JOIN test_attempts at ON at.assignment_id = a.id AND at.student_id = a.student_id
                WHERE (? = 1 OR t.teacher_id = ?)
                ORDER BY a.created_at DESC
                """;
        return jdbcTemplate.queryForList(sql, admin ? 1 : 0, currentUserId);
    }

    public List<Map<String, Object>> findAssignmentsForStudent(Long studentId) {
        String sql = """
                SELECT a.id,
                       a.test_id AS testId,
                       t.title AS testTitle,
                       t.description AS testDescription,
                       t.time_limit_minutes AS timeLimitMinutes,
                       s.name AS subjectName,
                       a.due_at AS dueAt,
                       a.created_at AS createdAt,
                       at.id AS attemptId,
                       at.status AS attemptStatus,
                       at.score_percent AS scorePercent
                FROM test_assignments a
                JOIN tests t ON t.id = a.test_id
                JOIN subjects s ON s.id = t.subject_id
                LEFT JOIN test_attempts at ON at.assignment_id = a.id AND at.student_id = a.student_id
                WHERE a.student_id = ?
                ORDER BY a.created_at DESC
                """;
        return jdbcTemplate.queryForList(sql, studentId);
    }

    public Optional<Map<String, Object>> findAssignmentForStudent(Long assignmentId, Long studentId) {
        String sql = """
                SELECT a.id,
                       a.test_id AS testId,
                       a.student_id AS studentId,
                       a.due_at AS dueAt,
                       t.time_limit_minutes AS timeLimitMinutes,
                       t.title AS testTitle,
                       t.is_active AS testActive
                FROM test_assignments a
                JOIN tests t ON t.id = a.test_id
                WHERE a.id = ?
                  AND a.student_id = ?
                """;
        return findOne(sql, assignmentId, studentId);
    }

    public Optional<Map<String, Object>> findAttemptByAssignmentAndStudent(Long assignmentId, Long studentId) {
        String sql = """
                SELECT id,
                       assignment_id AS assignmentId,
                       student_id AS studentId,
                       status,
                       current_position AS currentPosition,
                       current_question_started_at AS currentQuestionStartedAt,
                       start_time AS startTime,
                       end_time AS endTime,
                       earned_points AS earnedPoints,
                       total_points AS totalPoints,
                       score_percent AS scorePercent,
                       violations_count AS violationsCount
                FROM test_attempts
                WHERE assignment_id = ?
                  AND student_id = ?
                """;
        return findOne(sql, assignmentId, studentId);
    }

    public Long createAttempt(Long assignmentId, Long studentId, BigDecimal totalPoints) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO test_attempts (assignment_id, student_id, status, current_position, current_question_started_at, total_points) VALUES (?, ?, 'IN_PROGRESS', 1, CURRENT_TIMESTAMP, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, assignmentId);
            ps.setLong(2, studentId);
            ps.setBigDecimal(3, totalPoints);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Неуспешно стартиране на тестов опит.");
        }
        return key.longValue();
    }

    public Optional<Map<String, Object>> findAttemptByIdForStudent(Long attemptId, Long studentId) {
        String sql = """
                SELECT at.id,
                       at.assignment_id AS assignmentId,
                       at.student_id AS studentId,
                       at.status,
                       at.current_position AS currentPosition,
                       at.current_question_started_at AS currentQuestionStartedAt,
                       at.start_time AS startTime,
                       at.end_time AS endTime,
                       at.earned_points AS earnedPoints,
                       at.total_points AS totalPoints,
                       at.score_percent AS scorePercent,
                       at.violations_count AS violationsCount,
                       a.test_id AS testId,
                       t.title AS testTitle
                FROM test_attempts at
                JOIN test_assignments a ON a.id = at.assignment_id
                JOIN tests t ON t.id = a.test_id
                WHERE at.id = ?
                  AND at.student_id = ?
                """;
        return findOne(sql, attemptId, studentId);
    }

    public Optional<Map<String, Object>> findCurrentQuestion(Long testId, int currentPosition) {
        String sql = """
                SELECT id,
                       test_id AS testId,
                       question_text AS questionText,
                       source_type AS sourceType,
                       points,
                       time_limit_seconds AS timeLimitSeconds,
                       position_index AS positionIndex
                FROM questions
                WHERE test_id = ?
                  AND position_index = ?
                """;
        return findOne(sql, testId, currentPosition);
    }

    public List<Map<String, Object>> findOptionsByQuestion(Long questionId) {
        String sql = """
                SELECT id,
                       question_id AS questionId,
                       option_text AS optionText,
                       is_correct AS correct,
                       position_index AS positionIndex
                FROM question_options
                WHERE question_id = ?
                ORDER BY position_index
                """;
        return jdbcTemplate.queryForList(sql, questionId);
    }

    public Optional<Map<String, Object>> findOptionById(Long optionId) {
        String sql = """
                SELECT id,
                       question_id AS questionId,
                       option_text AS optionText,
                       is_correct AS correct,
                       position_index AS positionIndex
                FROM question_options
                WHERE id = ?
                """;
        return findOne(sql, optionId);
    }

    public boolean hasAnswerForQuestion(Long attemptId, Long questionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attempt_answers WHERE attempt_id = ? AND question_id = ?",
                Integer.class,
                attemptId,
                questionId
        );
        return count != null && count > 0;
    }

    public Long insertAttemptAnswer(Long attemptId, Long questionId, Long selectedOptionId,
                                    boolean correct, BigDecimal earnedPoints, String violationReason) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO attempt_answers
                        (attempt_id, question_id, selected_option_id, is_correct, earned_points, violation_reason)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, attemptId);
            ps.setLong(2, questionId);
            if (selectedOptionId == null) {
                ps.setNull(3, java.sql.Types.BIGINT);
            } else {
                ps.setLong(3, selectedOptionId);
            }
            ps.setBoolean(4, correct);
            ps.setBigDecimal(5, earnedPoints);
            ps.setString(6, violationReason);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Неуспешен запис на отговор.");
        }
        return key.longValue();
    }

    public void insertAttemptAnswerOptions(Long attemptAnswerId, List<Long> optionIds) {
        if (optionIds == null || optionIds.isEmpty()) {
            return;
        }

        for (Long optionId : optionIds) {
            jdbcTemplate.update(
                    "INSERT IGNORE INTO attempt_answer_options (attempt_answer_id, option_id) VALUES (?, ?)",
                    attemptAnswerId,
                    optionId
            );
        }
    }

    public void updateAttemptProgress(Long attemptId, int nextPosition, BigDecimal earnedDelta, int violationsDelta) {
        jdbcTemplate.update(
                """
                UPDATE test_attempts
                SET current_position = ?,
                    current_question_started_at = CURRENT_TIMESTAMP,
                    earned_points = earned_points + ?,
                    violations_count = violations_count + ?
                WHERE id = ?
                """,
                nextPosition,
                earnedDelta,
                violationsDelta,
                attemptId
        );
    }

    public void completeAttempt(Long attemptId) {
        jdbcTemplate.update(
                """
                UPDATE test_attempts
                SET status = 'COMPLETED',
                    end_time = CURRENT_TIMESTAMP,
                    score_percent = CASE
                        WHEN total_points <= 0 THEN 0
                        ELSE ROUND((earned_points / total_points) * 100, 2)
                    END
                WHERE id = ?
                """,
                attemptId
        );
    }

    public int countQuestions(Long testId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM questions WHERE test_id = ?",
                Integer.class,
                testId
        );
        return count == null ? 0 : count;
    }

    public BigDecimal sumTestPoints(Long testId) {
        BigDecimal points = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(points), 0) FROM questions WHERE test_id = ?",
                BigDecimal.class,
                testId
        );
        return points == null ? BigDecimal.ZERO : points;
    }

    public List<Map<String, Object>> findAttemptAnswers(Long attemptId) {
        String sql = """
                SELECT a.id,
                       a.attempt_id AS attemptId,
                       a.question_id AS questionId,
                       q.question_text AS questionText,
                       a.selected_option_id AS selectedOptionId,
                       COALESCE(
                           NULLIF(GROUP_CONCAT(DISTINCT so.option_text ORDER BY so.position_index SEPARATOR ', '), ''),
                           o.option_text
                       ) AS selectedOptionText,
                       a.is_correct AS correct,
                       a.earned_points AS earnedPoints,
                       a.violation_reason AS violationReason,
                       a.answered_at AS answeredAt
                FROM attempt_answers a
                JOIN questions q ON q.id = a.question_id
                LEFT JOIN question_options o ON o.id = a.selected_option_id
                LEFT JOIN attempt_answer_options aao ON aao.attempt_answer_id = a.id
                LEFT JOIN question_options so ON so.id = aao.option_id
                WHERE a.attempt_id = ?
                GROUP BY a.id,
                         a.attempt_id,
                         a.question_id,
                         q.question_text,
                         a.selected_option_id,
                         o.option_text,
                         a.is_correct,
                         a.earned_points,
                         a.violation_reason,
                         a.answered_at,
                         q.position_index
                ORDER BY q.position_index
                """;
        return jdbcTemplate.queryForList(sql, attemptId);
    }

    public Map<String, Object> findTeacherOverview(Long currentUserId, boolean admin) {
        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM tests t WHERE (? = 1 OR t.teacher_id = ?)) AS totalTests,
                  (SELECT COUNT(*) FROM test_assignments a
                     JOIN tests t ON t.id = a.test_id
                    WHERE (? = 1 OR t.teacher_id = ?)) AS totalAssignments,
                  (SELECT COUNT(*) FROM test_attempts ta
                     JOIN test_assignments a ON a.id = ta.assignment_id
                     JOIN tests t ON t.id = a.test_id
                    WHERE ta.status = 'COMPLETED'
                      AND (? = 1 OR t.teacher_id = ?)) AS completedAttempts,
                  (SELECT COALESCE(ROUND(AVG(ta.score_percent), 2), 0)
                     FROM test_attempts ta
                     JOIN test_assignments a ON a.id = ta.assignment_id
                     JOIN tests t ON t.id = a.test_id
                    WHERE ta.status = 'COMPLETED'
                      AND (? = 1 OR t.teacher_id = ?)) AS averageScore
                """;
        return jdbcTemplate.queryForMap(
                sql,
                admin ? 1 : 0,
                currentUserId,
                admin ? 1 : 0,
                currentUserId,
                admin ? 1 : 0,
                currentUserId,
                admin ? 1 : 0,
                currentUserId
        );
    }

    public List<Map<String, Object>> findScoresByTest(Long testId, Long currentUserId, boolean admin) {
        String sql = """
                SELECT ta.id AS attemptId,
                       ta.status,
                       ta.score_percent AS scorePercent,
                       ta.earned_points AS earnedPoints,
                       ta.total_points AS totalPoints,
                       ta.violations_count AS violationsCount,
                       ta.start_time AS startTime,
                       ta.end_time AS endTime,
                       u.id AS studentId,
                       u.name AS studentName,
                       u.username AS studentUsername
                FROM test_attempts ta
                JOIN test_assignments a ON a.id = ta.assignment_id
                JOIN tests t ON t.id = a.test_id
                JOIN users u ON u.id = ta.student_id
                WHERE t.id = ?
                  AND (? = 1 OR t.teacher_id = ?)
                ORDER BY ta.start_time DESC
                """;
        return jdbcTemplate.queryForList(sql, testId, admin ? 1 : 0, currentUserId);
    }

    public boolean testBelongsToTeacher(Long testId, Long currentUserId, boolean admin) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tests WHERE id = ? AND (? = 1 OR teacher_id = ?)",
                Integer.class,
                testId,
                admin ? 1 : 0,
                currentUserId
        );
        return count != null && count > 0;
    }

    public boolean subjectBelongsToTeacher(Long subjectId, Long currentUserId, boolean admin) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM subjects WHERE id = ? AND (? = 1 OR teacher_id = ?)",
                Integer.class,
                subjectId,
                admin ? 1 : 0,
                currentUserId
        );
        return count != null && count > 0;
    }

    public Timestamp parseDueAt(String dueAt) {
        if (dueAt == null || dueAt.isBlank()) {
            return null;
        }
        LocalDateTime value = LocalDateTime.parse(dueAt);
        return Timestamp.valueOf(value);
    }

    private Optional<Map<String, Object>> findOne(String sql, Object... params) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public List<Map<String, Object>> emptyList() {
        return Collections.emptyList();
    }
}
