import { useEffect, useMemo, useRef, useState } from "react";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ||
  `${window.location.protocol}//${window.location.hostname}:8081/api`;
const AUTH_TOKEN_KEY = "smart_test_auth_token";
const DEFAULT_ROLES = ["STUDENT", "TEACHER", "ADMIN"];
const ROLE_ACCESS_KEY_SEPARATOR = "::";

async function request(path, options = {}, authToken) {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  if (authToken) {
    headers.Authorization = authToken;
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
  } catch {
    throw new Error(`Няма връзка с backend-а (${API_BASE_URL}).`);
  }

  if (response.status === 204) {
    return null;
  }

  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(data?.message || "Грешка при заявка към сървъра.");
  }
  return data;
}

function normalizeRole(role) {
  return String(role || "").trim().toUpperCase();
}

export default function App() {
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ text: "", type: "success" });

  const [authToken, setAuthToken] = useState(localStorage.getItem(AUTH_TOKEN_KEY) || "");
  const [currentUser, setCurrentUser] = useState(null);
  const [authView, setAuthView] = useState("register");

  const [loginForm, setLoginForm] = useState({ login: "", password: "" });
  const [registerForm, setRegisterForm] = useState({
    username: "",
    name: "",
    email: "",
    password: "",
    role: "STUDENT"
  });

  const [users, setUsers] = useState([]);
  const [rolesCatalog, setRolesCatalog] = useState([]);
  const [accessObjectsCatalog, setAccessObjectsCatalog] = useState([]);
  const [roleAccessMap, setRoleAccessMap] = useState({});
  const [createUserForm, setCreateUserForm] = useState({
    username: "",
    name: "",
    email: "",
    password: "",
    role: "STUDENT"
  });
  const [roleByUserId, setRoleByUserId] = useState({});
  const [passwordByUserId, setPasswordByUserId] = useState({});
  const [activeByUserId, setActiveByUserId] = useState({});

  const [subjects, setSubjects] = useState([]);
  const [tests, setTests] = useState([]);
  const [groups, setGroups] = useState([]);
  const [students, setStudents] = useState([]);
  const [teacherAssignments, setTeacherAssignments] = useState([]);
  const [teacherOverview, setTeacherOverview] = useState(null);
  const [testReportRows, setTestReportRows] = useState([]);

  const [subjectForm, setSubjectForm] = useState({ name: "", description: "" });
  const [testForm, setTestForm] = useState({ title: "", description: "", subjectId: "", timeLimitMinutes: 30 });
  const [selectedTestId, setSelectedTestId] = useState("");
  const [selectedTestDetails, setSelectedTestDetails] = useState(null);

  const [manualQuestionForm, setManualQuestionForm] = useState({
    questionText: "",
    points: 1,
    timeLimitSeconds: 60,
    options: ["", ""],
    correctIndexes: [0]
  });
  const [editingQuestionId, setEditingQuestionId] = useState(null);

  const [aiQuestionForm, setAiQuestionForm] = useState({
    topic: "",
    difficulty: "medium",
    count: 3,
    timeLimitSeconds: 60
  });

  const [groupForm, setGroupForm] = useState({ name: "" });
  const [memberByGroupId, setMemberByGroupId] = useState({});

  const [assignStudentForm, setAssignStudentForm] = useState({ testId: "", studentId: "", dueAt: "" });
  const [assignGroupForm, setAssignGroupForm] = useState({ testId: "", groupId: "", dueAt: "" });
  const [reportTestId, setReportTestId] = useState("");

  const [studentAssignments, setStudentAssignments] = useState([]);
  const [studentResults, setStudentResults] = useState([]);
  const [activeAttemptId, setActiveAttemptId] = useState(null);
  const [currentQuestionPayload, setCurrentQuestionPayload] = useState(null);
  const [attemptResult, setAttemptResult] = useState(null);
  const [selectedOptionIds, setSelectedOptionIds] = useState([]);
  const [questionRemainingSeconds, setQuestionRemainingSeconds] = useState(null);
  const violationLockRef = useRef(false);
  const timeoutHandledQuestionIdRef = useRef(null);

  const isAuthenticated = Boolean(authToken);
  const hasMessage = Boolean(message.text);
  const role = normalizeRole(currentUser?.role);
  const isAdmin = role === "ADMIN";
  const isTeacher = role === "TEACHER";
  const isStudent = role === "STUDENT";

  const roleOptionsByUser = useMemo(() => {
    const activeRoleCodes = rolesCatalog
      .filter((roleEntry) => Boolean(roleEntry.active))
      .map((roleEntry) => normalizeRole(roleEntry.code))
      .filter(Boolean);

    const baseRoleOptions = activeRoleCodes.length > 0 ? activeRoleCodes : DEFAULT_ROLES;
    const map = {};
    users.forEach((user) => {
      const currentRole = normalizeRole(roleByUserId[user.id] || user.role || "STUDENT");
      const options = [...baseRoleOptions];
      if (!options.includes(currentRole)) {
        options.push(currentRole);
      }
      map[user.id] = options;
    });
    return map;
  }, [users, roleByUserId, rolesCatalog]);

  function pushMessage(text, type = "success") {
    setMessage({ text, type });
  }

  async function loadCurrentUser(token = authToken) {
    const me = await request("/auth/me", {}, token);
    setCurrentUser(me);
    return me;
  }

  async function loadAdminData(token = authToken) {
    if (!token) return;
    const [data, accessConfig] = await Promise.all([
      request("/users", {}, token),
      request("/users/access-config", {}, token)
    ]);
    setUsers(data);

    const roles = {};
    const pass = {};
    const active = {};
    data.forEach((user) => {
      roles[user.id] = normalizeRole(user.role || "STUDENT");
      pass[user.id] = "";
      active[user.id] = Boolean(user.active);
    });
    setRoleByUserId(roles);
    setPasswordByUserId(pass);
    setActiveByUserId(active);

    const rolesCatalogData = Array.isArray(accessConfig?.roles) ? accessConfig.roles : [];
    const accessObjectsData = Array.isArray(accessConfig?.accessObjects) ? accessConfig.accessObjects : [];
    const roleAccessData = Array.isArray(accessConfig?.roleAccess) ? accessConfig.roleAccess : [];

    setRolesCatalog(rolesCatalogData);
    setAccessObjectsCatalog(accessObjectsData);

    const matrix = {};
    roleAccessData.forEach((entry) => {
      const roleCode = normalizeRole(entry.roleCode);
      const accessObjectCode = normalizeRole(entry.accessObjectCode);
      matrix[`${roleCode}${ROLE_ACCESS_KEY_SEPARATOR}${accessObjectCode}`] = Boolean(entry.canView);
    });
    setRoleAccessMap(matrix);

    const activeRoleCodes = rolesCatalogData
      .filter((roleEntry) => Boolean(roleEntry.active))
      .map((roleEntry) => normalizeRole(roleEntry.code))
      .filter(Boolean);

    setCreateUserForm((prev) => ({
      ...prev,
      role: activeRoleCodes.includes(normalizeRole(prev.role))
        ? normalizeRole(prev.role)
        : activeRoleCodes[0] || "STUDENT"
    }));
  }

  async function loadTeacherData(token = authToken) {
    if (!token) return;
    const [subjectsData, testsData, groupsData, studentsData, assignmentsData, overviewData] = await Promise.all([
      request("/teacher/subjects", {}, token),
      request("/teacher/tests", {}, token),
      request("/teacher/groups", {}, token),
      request("/teacher/students", {}, token),
      request("/teacher/assignments", {}, token),
      request("/teacher/reports/overview", {}, token)
    ]);

    setSubjects(subjectsData);
    setTests(testsData);
    setGroups(groupsData);
    setStudents(studentsData);
    setTeacherAssignments(assignmentsData);
    setTeacherOverview(overviewData);

    if (!selectedTestId && testsData.length > 0) {
      setSelectedTestId(String(testsData[0].id));
    }
    if (!assignStudentForm.testId && testsData.length > 0) {
      setAssignStudentForm((prev) => ({ ...prev, testId: String(testsData[0].id) }));
    }
    if (!assignGroupForm.testId && testsData.length > 0) {
      setAssignGroupForm((prev) => ({ ...prev, testId: String(testsData[0].id) }));
    }
    if (!assignGroupForm.groupId && groupsData.length > 0) {
      setAssignGroupForm((prev) => ({ ...prev, groupId: String(groupsData[0].id) }));
    }
    if (!assignStudentForm.studentId && studentsData.length > 0) {
      setAssignStudentForm((prev) => ({ ...prev, studentId: String(studentsData[0].id) }));
    }
  }

  async function loadStudentData(token = authToken) {
    if (!token) return;
    const [assignments, results] = await Promise.all([
      request("/student/assignments", {}, token),
      request("/student/results", {}, token)
    ]);
    setStudentAssignments(assignments);
    setStudentResults(results);
  }

  useEffect(() => {
    let active = true;

    async function initialize() {
      if (!authToken) {
        setCurrentUser(null);
        return;
      }

      setLoading(true);
      try {
        const me = await loadCurrentUser(authToken);
        if (!active) return;

        const meRole = normalizeRole(me.role);
        if (meRole === "ADMIN") {
          await loadAdminData(authToken);
        } else if (meRole === "TEACHER") {
          await loadTeacherData(authToken);
        } else if (meRole === "STUDENT") {
          await loadStudentData(authToken);
        }
      } catch (error) {
        if (!active) return;
        pushMessage(error.message, "error");
        handleLogout();
      } finally {
        if (active) setLoading(false);
      }
    }

    initialize();
    return () => {
      active = false;
    };
  }, [authToken]);

  useEffect(() => {
    let cancelled = false;

    async function loadSelectedTest() {
      if (!isTeacher || !selectedTestId) {
        setSelectedTestDetails(null);
        return;
      }

      try {
        const details = await request(`/teacher/tests/${selectedTestId}`, {}, authToken);
        if (!cancelled) {
          setSelectedTestDetails(details);
        }
      } catch (error) {
        if (!cancelled) {
          pushMessage(error.message, "error");
        }
      }
    }

    loadSelectedTest();
    return () => {
      cancelled = true;
    };
  }, [isTeacher, selectedTestId, authToken]);

  useEffect(() => {
    if (!isStudent || !activeAttemptId || !currentQuestionPayload || currentQuestionPayload.completed) {
      return;
    }

    const onViolationEvent = async (reason) => {
      if (violationLockRef.current) return;
      violationLockRef.current = true;
      try {
        const payload = await request(
          `/student/attempts/${activeAttemptId}/violation`,
          {
            method: "POST",
            body: JSON.stringify({ reason })
          },
          authToken
        );
        applyAttemptPayload(payload, "Нарушение отчетено: " + reason);
      } catch (error) {
        pushMessage(error.message, "error");
      } finally {
        setTimeout(() => {
          violationLockRef.current = false;
        }, 400);
      }
    };

    const visibilityHandler = () => {
      if (document.hidden) {
        onViolationEvent("TAB_SWITCH");
      }
    };

    const blurHandler = () => {
      onViolationEvent("WINDOW_BLUR");
    };

    document.addEventListener("visibilitychange", visibilityHandler);
    window.addEventListener("blur", blurHandler);

    return () => {
      document.removeEventListener("visibilitychange", visibilityHandler);
      window.removeEventListener("blur", blurHandler);
    };
  }, [isStudent, activeAttemptId, currentQuestionPayload, authToken]);

  useEffect(() => {
    if (!isStudent || !activeAttemptId || !currentQuestionPayload || currentQuestionPayload.completed) {
      return;
    }

    const questionId = currentQuestionPayload?.question?.id;
    if (!questionId || typeof questionRemainingSeconds !== "number") {
      return;
    }

    if (questionRemainingSeconds <= 0) {
      if (timeoutHandledQuestionIdRef.current === questionId) {
        return;
      }

      timeoutHandledQuestionIdRef.current = questionId;
      if (violationLockRef.current) {
        return;
      }

      violationLockRef.current = true;
      request(
        `/student/attempts/${activeAttemptId}/violation`,
        { method: "POST", body: JSON.stringify({ reason: "TIME_EXPIRED" }) },
        authToken
      )
        .then((payload) => applyAttemptPayload(payload, "Времето за въпроса изтече."))
        .catch((error) => pushMessage(error.message, "error"))
        .finally(() => {
          setTimeout(() => {
            violationLockRef.current = false;
          }, 400);
        });
      return;
    }

    const timer = window.setTimeout(() => {
      setQuestionRemainingSeconds((prev) => {
        if (typeof prev !== "number") return prev;
        return Math.max(prev - 1, 0);
      });
    }, 1000);

    return () => {
      window.clearTimeout(timer);
    };
  }, [isStudent, activeAttemptId, currentQuestionPayload, questionRemainingSeconds, authToken]);

  function handleLogout() {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    setAuthToken("");
    setCurrentUser(null);

    setUsers([]);
    setRolesCatalog([]);
    setAccessObjectsCatalog([]);
    setRoleAccessMap({});
    setSubjects([]);
    setTests([]);
    setGroups([]);
    setStudents([]);
    setTeacherAssignments([]);
    setTeacherOverview(null);
    setTestReportRows([]);
    setSelectedTestId("");
    setSelectedTestDetails(null);
    setEditingQuestionId(null);
    setManualQuestionForm({ questionText: "", points: 1, timeLimitSeconds: 60, options: ["", ""], correctIndexes: [0] });

    setStudentAssignments([]);
    setStudentResults([]);
    setActiveAttemptId(null);
    setCurrentQuestionPayload(null);
    setAttemptResult(null);
    setSelectedOptionIds([]);
    setQuestionRemainingSeconds(null);
    timeoutHandledQuestionIdRef.current = null;
  }

  async function handleLogin(event) {
    event.preventDefault();
    try {
      const result = await request("/auth/login", {
        method: "POST",
        body: JSON.stringify(loginForm)
      });

      localStorage.setItem(AUTH_TOKEN_KEY, result.token);
      setAuthToken(result.token);
      setCurrentUser(result.user || null);
      setLoginForm({ login: "", password: "" });
      pushMessage(result.message || "Успешен вход.", "success");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  async function handleRegister(event) {
    event.preventDefault();
    const draft = { ...registerForm };
    try {
      await request("/auth/register", {
        method: "POST",
        body: JSON.stringify(registerForm)
      });

      setRegisterForm({ username: "", name: "", email: "", password: "", role: "STUDENT" });
      setLoginForm({ login: draft.username, password: draft.password });
      setAuthView("login");
      pushMessage("Регистрацията е успешна. Можеш да влезеш.", "success");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  async function handleAdminCreateUser(event) {
    event.preventDefault();
    try {
      await request("/users", { method: "POST", body: JSON.stringify(createUserForm) }, authToken);
      const activeRoleCodes = rolesCatalog
        .filter((roleEntry) => Boolean(roleEntry.active))
        .map((roleEntry) => normalizeRole(roleEntry.code))
        .filter(Boolean);
      setCreateUserForm({
        username: "",
        name: "",
        email: "",
        password: "",
        role: activeRoleCodes[0] || "STUDENT"
      });
      await loadAdminData();
      pushMessage("Потребителят е създаден.", "success");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  async function handleAdminUpdateRole(userId) {
    try {
      await request(
        `/users/${userId}/role`,
        { method: "PUT", body: JSON.stringify({ role: roleByUserId[userId] || "STUDENT" }) },
        authToken
      );
      await loadAdminData();
      pushMessage("Ролята е обновена.", "success");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  async function handleAdminUpdatePassword(userId) {
    const password = passwordByUserId[userId] || "";
    if (!password.trim()) {
      pushMessage("Въведи нова парола.", "error");
      return;
    }

    try {
      await request(`/users/${userId}/password`, { method: "PUT", body: JSON.stringify({ password }) }, authToken);
      await loadAdminData();
      pushMessage("Паролата е сменена.", "success");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  async function handleAdminToggleActive(userId) {
    const nextActive = !Boolean(activeByUserId[userId]);
    try {
      await request(
        `/users/${userId}/activation`,
        { method: "PUT", body: JSON.stringify({ active: nextActive }) },
        authToken
      );
      await loadAdminData();
      pushMessage(nextActive ? "Потребителят е активиран." : "Потребителят е деактивиран.", "success");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  async function handleAdminDeleteUser(userId) {
    if (!window.confirm("Сигурен ли си, че искаш да изтриеш потребителя?")) return;
    try {
      await request(`/users/${userId}`, { method: "DELETE" }, authToken);
      await loadAdminData();
      pushMessage("Потребителят е изтрит.", "success");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  function roleAccessKey(roleCode, accessObjectCode) {
    return `${normalizeRole(roleCode)}${ROLE_ACCESS_KEY_SEPARATOR}${normalizeRole(accessObjectCode)}`;
  }

  async function handleRoleAccessToggle(roleCode, accessObjectCode, nextCanView) {
    try {
      await request(
        "/users/access-config/role-access",
        {
          method: "PUT",
          body: JSON.stringify({
            roleCode: normalizeRole(roleCode),
            accessObjectCode: normalizeRole(accessObjectCode),
            canView: nextCanView
          })
        },
        authToken
      );

      setRoleAccessMap((prev) => ({
        ...prev,
        [roleAccessKey(roleCode, accessObjectCode)]: Boolean(nextCanView)
      }));
      pushMessage("Правата са обновени.", "success");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  async function handleRoleActivation(roleCode, nextActive) {
    try {
      await request(
        `/users/roles/${encodeURIComponent(roleCode)}/activation`,
        { method: "PUT", body: JSON.stringify({ active: nextActive }) },
        authToken
      );
      await loadAdminData();
      pushMessage(nextActive ? "Ролята е активирана." : "Ролята е деактивирана.", "success");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  function resetManualQuestionForm() {
    setManualQuestionForm({
      questionText: "",
      points: 1,
      timeLimitSeconds: 60,
      options: ["", ""],
      correctIndexes: [0]
    });
    setEditingQuestionId(null);
  }

  function handleManualOptionCountChange(nextCountRaw) {
    const nextCount = Math.max(2, Math.min(8, Number(nextCountRaw) || 2));
    setManualQuestionForm((prev) => {
      const options = [...prev.options];
      while (options.length < nextCount) {
        options.push("");
      }
      const resizedOptions = options.slice(0, nextCount);
      const validCorrectIndexes = (prev.correctIndexes || [])
        .filter((index) => index >= 0 && index < nextCount);
      return {
        ...prev,
        options: resizedOptions,
        correctIndexes: validCorrectIndexes.length > 0 ? validCorrectIndexes : [0]
      };
    });
  }

  function handleToggleCorrectOption(index) {
    setManualQuestionForm((prev) => {
      const set = new Set(prev.correctIndexes || []);
      if (set.has(index)) {
        set.delete(index);
      } else {
        set.add(index);
      }
      const next = Array.from(set).sort((a, b) => a - b);
      return {
        ...prev,
        correctIndexes: next.length > 0 ? next : [index]
      };
    });
  }

  function handleEditQuestion(question) {
    const options = [...(question.options || [])].sort((a, b) => Number(a.positionIndex) - Number(b.positionIndex));
    const correctIndexes = [];
    const optionTexts = options.map((entry, index) => {
      if (entry.correct) {
        correctIndexes.push(index);
      }
      return entry.optionText || "";
    });

    setEditingQuestionId(question.id);
    setManualQuestionForm({
      questionText: question.questionText || "",
      points: Number(question.points) || 1,
      timeLimitSeconds: Number(question.timeLimitSeconds) || 60,
      options: optionTexts.length >= 2 ? optionTexts : ["", ""],
      correctIndexes: correctIndexes.length > 0 ? correctIndexes : [0]
    });
    pushMessage("Режим редакция на въпрос.", "success");
  }

  function cancelQuestionEdit() {
    resetManualQuestionForm();
    pushMessage("Редакцията е отказана.", "success");
  }

  async function handleCreateSubject(event) {
    event.preventDefault();
    try {
      await request("/teacher/subjects", { method: "POST", body: JSON.stringify(subjectForm) }, authToken);
      setSubjectForm({ name: "", description: "" });
      await loadTeacherData();
      pushMessage("Предметът е създаден.", "success");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  async function handleCreateTest(event) {
    event.preventDefault();
    try {
      const payload = {
        ...testForm,
        subjectId: Number(testForm.subjectId),
        timeLimitMinutes: Number(testForm.timeLimitMinutes)
      };
      const created = await request("/teacher/tests", { method: "POST", body: JSON.stringify(payload) }, authToken);
      await loadTeacherData();
      setSelectedTestId(String(created.id));
      pushMessage("Тестът е създаден.", "success");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  async function handleDeleteTest(testId) {
    if (!window.confirm("Изтриване на теста?")) return;
    try {
      await request(`/teacher/tests/${testId}`, { method: "DELETE" }, authToken);
      await loadTeacherData();
      if (String(testId) === selectedTestId) {
        setSelectedTestId("");
        setSelectedTestDetails(null);
      }
      pushMessage("Тестът е изтрит.", "success");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  async function handleAddManualQuestion(event) {
    event.preventDefault();
    if (!selectedTestId) {
      pushMessage("Избери тест.", "error");
      return;
    }

    const options = manualQuestionForm.options
      .map((text, index) => ({
        text: text.trim(),
        correct: (manualQuestionForm.correctIndexes || []).includes(index)
      }))
      .filter((item) => item.text.length > 0);

    if (options.length < 2) {
      pushMessage("Добави поне 2 отговора.", "error");
      return;
    }

    if (!options.some((entry) => entry.correct)) {
      pushMessage("Маркирай поне един верен отговор.", "error");
      return;
    }

    try {
      const payload = {
        questionText: manualQuestionForm.questionText,
        points: Number(manualQuestionForm.points),
        timeLimitSeconds: Number(manualQuestionForm.timeLimitSeconds) || 60,
        options
      };
      const response = editingQuestionId
        ? await request(
            `/teacher/questions/${editingQuestionId}`,
            { method: "PUT", body: JSON.stringify(payload) },
            authToken
          )
        : await request(
            `/teacher/tests/${selectedTestId}/questions/manual`,
            { method: "POST", body: JSON.stringify(payload) },
            authToken
          );

      setSelectedTestDetails(response.test);
      resetManualQuestionForm();
      await loadTeacherData();
      pushMessage(editingQuestionId ? "Въпросът е обновен." : "Въпросът е добавен.", "success");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  async function handleGenerateAiQuestions(event) {
    event.preventDefault();
    if (!selectedTestId) {
      pushMessage("Избери тест.", "error");
      return;
    }

    try {
      const response = await request(
        `/teacher/tests/${selectedTestId}/questions/ai`,
        {
          method: "POST",
          body: JSON.stringify({
            topic: aiQuestionForm.topic,
            difficulty: aiQuestionForm.difficulty,
            count: Number(aiQuestionForm.count),
            timeLimitSeconds: Number(aiQuestionForm.timeLimitSeconds) || 60
          })
        },
        authToken
      );

      setSelectedTestDetails(response.test);
      await loadTeacherData();
      pushMessage(response.message || "AI въпроси са добавени.", "success");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  async function handleDeleteQuestion(questionId) {
    if (!window.confirm("Изтриване на въпроса?")) return;
    try {
      await request(`/teacher/questions/${questionId}`, { method: "DELETE" }, authToken);
      const refreshed = await request(`/teacher/tests/${selectedTestId}`, {}, authToken);
      setSelectedTestDetails(refreshed);
      if (editingQuestionId === questionId) {
        resetManualQuestionForm();
      }
      await loadTeacherData();
      pushMessage("Въпросът е изтрит.", "success");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  async function handleCreateGroup(event) {
    event.preventDefault();
    try {
      await request("/teacher/groups", { method: "POST", body: JSON.stringify(groupForm) }, authToken);
      setGroupForm({ name: "" });
      await loadTeacherData();
      pushMessage("Групата е създадена.", "success");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  async function handleAddMember(groupId) {
    const studentId = Number(memberByGroupId[groupId]);
    if (!studentId) {
      pushMessage("Избери ученик.", "error");
      return;
    }

    try {
      await request(
        `/teacher/groups/${groupId}/members`,
        { method: "POST", body: JSON.stringify({ studentId }) },
        authToken
      );
      await loadTeacherData();
      pushMessage("Ученикът е добавен в групата.", "success");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  async function handleRemoveMember(groupId, studentId) {
    try {
      await request(`/teacher/groups/${groupId}/members/${studentId}`, { method: "DELETE" }, authToken);
      await loadTeacherData();
      pushMessage("Ученикът е премахнат от групата.", "success");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  async function handleAssignToStudent(event) {
    event.preventDefault();
    try {
      await request(
        "/teacher/assignments/student",
        {
          method: "POST",
          body: JSON.stringify({
            testId: Number(assignStudentForm.testId),
            studentId: Number(assignStudentForm.studentId),
            dueAt: assignStudentForm.dueAt || null
          })
        },
        authToken
      );
      await loadTeacherData();
      pushMessage("Тестът е зададен на ученик.", "success");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  async function handleAssignToGroup(event) {
    event.preventDefault();
    try {
      await request(
        "/teacher/assignments/group",
        {
          method: "POST",
          body: JSON.stringify({
            testId: Number(assignGroupForm.testId),
            groupId: Number(assignGroupForm.groupId),
            dueAt: assignGroupForm.dueAt || null
          })
        },
        authToken
      );
      await loadTeacherData();
      pushMessage("Тестът е зададен към групата.", "success");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  async function handleLoadReportByTest() {
    if (!reportTestId) {
      pushMessage("Избери тест за справката.", "error");
      return;
    }

    try {
      const rows = await request(`/teacher/reports/tests/${reportTestId}`, {}, authToken);
      setTestReportRows(rows);
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  function applyAttemptPayload(payload, successMessage = "") {
    if (payload?.completed === false) {
      setCurrentQuestionPayload(payload);
      setSelectedOptionIds([]);
      setQuestionRemainingSeconds(
        typeof payload.remainingSeconds === "number" ? payload.remainingSeconds : null
      );
      timeoutHandledQuestionIdRef.current = null;
      if (successMessage) {
        pushMessage(successMessage, "success");
      }
      return;
    }

    if (payload?.completed === true && payload.result) {
      setCurrentQuestionPayload(payload);
      setAttemptResult(payload.result);
      setSelectedOptionIds([]);
      setQuestionRemainingSeconds(null);
      timeoutHandledQuestionIdRef.current = null;
      loadStudentData();
      if (successMessage) {
        pushMessage(successMessage, "success");
      }
      return;
    }

    if (payload?.answers) {
      setAttemptResult(payload);
      setCurrentQuestionPayload({ completed: true, result: payload });
      setSelectedOptionIds([]);
      setQuestionRemainingSeconds(null);
      timeoutHandledQuestionIdRef.current = null;
      loadStudentData();
      if (successMessage) {
        pushMessage(successMessage, "success");
      }
      return;
    }

    pushMessage("Неочакван формат на отговор от сървъра.", "error");
  }

  async function handleStartAttempt(assignmentId) {
    try {
      const started = await request(`/student/attempts/${assignmentId}/start`, { method: "POST" }, authToken);
      setActiveAttemptId(started.attemptId);
      setAttemptResult(null);
      setSelectedOptionIds([]);
      setQuestionRemainingSeconds(null);
      timeoutHandledQuestionIdRef.current = null;
      applyAttemptPayload(started.current, "Тестът е стартиран.");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  async function handleSubmitStudentAnswer() {
    if (!activeAttemptId) return;

    try {
      const payload = await request(
        `/student/attempts/${activeAttemptId}/answer`,
        {
          method: "POST",
          body: JSON.stringify({ optionIds: selectedOptionIds.map((entry) => Number(entry)) })
        },
        authToken
      );

      applyAttemptPayload(payload, "Отговорът е записан.");
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  async function handleFinishAttemptAndRefresh() {
    if (!activeAttemptId) return;
    try {
      const result = await request(`/student/attempts/${activeAttemptId}/result`, {}, authToken);
      setAttemptResult(result);
      setCurrentQuestionPayload({ completed: true, result });
      await loadStudentData();
    } catch (error) {
      pushMessage(error.message, "error");
    }
  }

  function renderAuth() {
    return (
      <>
        <section className="panel">
          <div className="auth-tabs">
            <button
              type="button"
              className={`btn ${authView === "register" ? "btn-primary" : "btn-ghost"}`}
              onClick={() => setAuthView("register")}
            >
              Регистрация
            </button>
            <button
              type="button"
              className={`btn ${authView === "login" ? "btn-primary" : "btn-ghost"}`}
              onClick={() => setAuthView("login")}
            >
              Вход
            </button>
          </div>
        </section>

        {authView === "register" ? (
          <section className="panel">
            <h2>Регистрация</h2>
            <form className="form-grid" onSubmit={handleRegister}>
              <input
                type="text"
                placeholder="Username"
                value={registerForm.username}
                onChange={(event) => setRegisterForm((prev) => ({ ...prev, username: event.target.value }))}
                required
              />
              <input
                type="text"
                placeholder="Име"
                value={registerForm.name}
                onChange={(event) => setRegisterForm((prev) => ({ ...prev, name: event.target.value }))}
                required
              />
              <input
                type="email"
                placeholder="Email"
                value={registerForm.email}
                onChange={(event) => setRegisterForm((prev) => ({ ...prev, email: event.target.value }))}
                required
              />
              <input
                type="password"
                placeholder="Парола (мин. 6)"
                value={registerForm.password}
                onChange={(event) => setRegisterForm((prev) => ({ ...prev, password: event.target.value }))}
                required
              />
              <select
                value={registerForm.role}
                onChange={(event) => setRegisterForm((prev) => ({ ...prev, role: event.target.value }))}
              >
                <option value="STUDENT">STUDENT (Ученик)</option>
                <option value="TEACHER">TEACHER (Преподавател)</option>
              </select>
              <button type="submit" className="btn btn-primary">
                Регистрирай
              </button>
            </form>
          </section>
        ) : (
          <section className="panel">
            <h2>Вход</h2>
            <form className="form-grid" onSubmit={handleLogin}>
              <input
                type="text"
                placeholder="Username или Email"
                value={loginForm.login}
                onChange={(event) => setLoginForm((prev) => ({ ...prev, login: event.target.value }))}
                required
              />
              <input
                type="password"
                placeholder="Парола"
                value={loginForm.password}
                onChange={(event) => setLoginForm((prev) => ({ ...prev, password: event.target.value }))}
                required
              />
              <button type="submit" className="btn btn-primary">
                Вход
              </button>
            </form>
          </section>
        )}
      </>
    );
  }

  function renderAdminPanel() {
    const activeRoleOptions = rolesCatalog
      .filter((roleEntry) => Boolean(roleEntry.active))
      .map((roleEntry) => normalizeRole(roleEntry.code))
      .filter(Boolean);
    const effectiveRoleOptions = activeRoleOptions.length > 0 ? activeRoleOptions : DEFAULT_ROLES;

    return (
      <>
        <section className="panel">
          <div className="panel-title">
            <h2>Администраторски панел</h2>
            <button type="button" className="btn btn-ghost" onClick={handleLogout}>
              Изход
            </button>
          </div>

          <form className="form-grid" onSubmit={handleAdminCreateUser}>
            <input
              type="text"
              placeholder="Username"
              value={createUserForm.username}
              onChange={(event) => setCreateUserForm((prev) => ({ ...prev, username: event.target.value }))}
              required
            />
            <input
              type="text"
              placeholder="Име"
              value={createUserForm.name}
              onChange={(event) => setCreateUserForm((prev) => ({ ...prev, name: event.target.value }))}
              required
            />
            <input
              type="email"
              placeholder="Email"
              value={createUserForm.email}
              onChange={(event) => setCreateUserForm((prev) => ({ ...prev, email: event.target.value }))}
              required
            />
            <input
              type="password"
              placeholder="Парола"
              value={createUserForm.password}
              onChange={(event) => setCreateUserForm((prev) => ({ ...prev, password: event.target.value }))}
              required
            />
            <select
              value={createUserForm.role}
              onChange={(event) => setCreateUserForm((prev) => ({ ...prev, role: event.target.value }))}
            >
              {effectiveRoleOptions.map((entry) => (
                <option key={entry} value={entry}>
                  {entry}
                </option>
              ))}
            </select>
            <button type="submit" className="btn btn-primary">
              Създай
            </button>
          </form>
        </section>

        <section className="panel">
          <div className="panel-title">
            <h2>Потребители</h2>
            <button className="btn btn-ghost" type="button" onClick={() => loadAdminData()}>
              Обнови
            </button>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Username</th>
                  <th>Име</th>
                  <th>Email</th>
                  <th>Роля</th>
                  <th>Статус</th>
                  <th>Парола</th>
                  <th>Изтриване</th>
                </tr>
              </thead>
              <tbody>
                {users.length === 0 ? (
                  <tr>
                    <td colSpan="8">Няма потребители.</td>
                  </tr>
                ) : (
                  users.map((user) => {
                    const isActive = Boolean(activeByUserId[user.id]);
                    return (
                      <tr key={user.id}>
                        <td>{user.id}</td>
                        <td>{user.username}</td>
                        <td>{user.name}</td>
                        <td>{user.email}</td>
                        <td>
                          <div className="row-controls">
                            <select
                              value={roleByUserId[user.id] || "STUDENT"}
                              onChange={(event) =>
                                setRoleByUserId((prev) => ({ ...prev, [user.id]: event.target.value }))
                              }
                            >
                              {(roleOptionsByUser[user.id] || DEFAULT_ROLES).map((entry) => (
                                <option key={entry} value={entry}>
                                  {entry}
                                </option>
                              ))}
                            </select>
                            <button className="btn btn-primary" type="button" onClick={() => handleAdminUpdateRole(user.id)}>
                              Запази
                            </button>
                          </div>
                        </td>
                        <td>
                          <div className="row-controls">
                            <span className={isActive ? "status-active" : "status-inactive"}>
                              {isActive ? "Активен" : "Неактивен"}
                            </span>
                            <button
                              className={`btn ${isActive ? "btn-danger" : "btn-primary"}`}
                              type="button"
                              onClick={() => handleAdminToggleActive(user.id)}
                            >
                              {isActive ? "Деактивирай" : "Активирай"}
                            </button>
                          </div>
                        </td>
                        <td>
                          <div className="row-controls">
                            <input
                              type="password"
                              placeholder="Нова парола"
                              value={passwordByUserId[user.id] || ""}
                              onChange={(event) =>
                                setPasswordByUserId((prev) => ({ ...prev, [user.id]: event.target.value }))
                              }
                            />
                            <button className="btn btn-primary" type="button" onClick={() => handleAdminUpdatePassword(user.id)}>
                              Смени
                            </button>
                          </div>
                        </td>
                        <td>
                          <button className="btn btn-danger" type="button" onClick={() => handleAdminDeleteUser(user.id)}>
                            Изтрий
                          </button>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel">
          <h2>Роли (номенклатура)</h2>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Код</th>
                  <th>Име</th>
                  <th>Описание</th>
                  <th>Статус</th>
                  <th>Действие</th>
                </tr>
              </thead>
              <tbody>
                {rolesCatalog.length === 0 ? (
                  <tr>
                    <td colSpan="5">Няма роли.</td>
                  </tr>
                ) : (
                  rolesCatalog.map((entry) => {
                    const active = Boolean(entry.active);
                    const roleCode = normalizeRole(entry.code);
                    const canToggle = roleCode !== "ADMIN";
                    return (
                      <tr key={roleCode}>
                        <td>{roleCode}</td>
                        <td>{entry.name}</td>
                        <td>{entry.description || "-"}</td>
                        <td>
                          <span className={active ? "status-active" : "status-inactive"}>
                            {active ? "Активна" : "Неактивна"}
                          </span>
                        </td>
                        <td>
                          <button
                            className={`btn ${active ? "btn-danger" : "btn-primary"}`}
                            type="button"
                            disabled={!canToggle}
                            onClick={() => handleRoleActivation(roleCode, !active)}
                          >
                            {active ? "Деактивирай" : "Активирай"}
                          </button>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel">
          <h2>Матрица за достъп (Role -&gt; Controller)</h2>
          <p className="subtitle">
            Управлява видимостта на контролерите чрез таблицата <code>role_access</code>.
          </p>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Роля</th>
                  {accessObjectsCatalog.map((accessObject) => (
                    <th key={accessObject.code}>{accessObject.code}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rolesCatalog.length === 0 || accessObjectsCatalog.length === 0 ? (
                  <tr>
                    <td colSpan={Math.max(2, accessObjectsCatalog.length + 1)}>
                      Няма конфигурирани роли или обекти за достъп.
                    </td>
                  </tr>
                ) : (
                  rolesCatalog.map((roleEntry) => {
                    const roleCode = normalizeRole(roleEntry.code);
                    return (
                      <tr key={roleCode}>
                        <td>{roleCode}</td>
                        {accessObjectsCatalog.map((accessObject) => {
                          const accessObjectCode = normalizeRole(accessObject.code);
                          const key = roleAccessKey(roleCode, accessObjectCode);
                          const checked = Boolean(roleAccessMap[key]);
                          return (
                            <td key={accessObjectCode}>
                              <input
                                type="checkbox"
                                checked={checked}
                                onChange={(event) =>
                                  handleRoleAccessToggle(roleCode, accessObjectCode, event.target.checked)
                                }
                              />
                            </td>
                          );
                        })}
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </section>
      </>
    );
  }

  function renderTeacherPanel() {
    return (
      <>
        <section className="panel">
          <div className="panel-title">
            <h2>Teacher панел</h2>
            <button type="button" className="btn btn-ghost" onClick={handleLogout}>
              Изход
            </button>
          </div>
          <p className="subtitle">
            Създаване на предмети/тестове, ръчно и AI добавяне на въпроси, задаване към ученик/група и справки.
          </p>
        </section>

        <section className="panel">
          <h2>Предмети</h2>
          <form className="form-grid" onSubmit={handleCreateSubject}>
            <input
              type="text"
              placeholder="Име на предмет"
              value={subjectForm.name}
              onChange={(event) => setSubjectForm((prev) => ({ ...prev, name: event.target.value }))}
              required
            />
            <input
              type="text"
              placeholder="Описание"
              value={subjectForm.description}
              onChange={(event) => setSubjectForm((prev) => ({ ...prev, description: event.target.value }))}
            />
            <button type="submit" className="btn btn-primary">
              Създай предмет
            </button>
          </form>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Предмет</th>
                  <th>Преподавател</th>
                </tr>
              </thead>
              <tbody>
                {subjects.length === 0 ? (
                  <tr>
                    <td colSpan="3">Няма предмети.</td>
                  </tr>
                ) : (
                  subjects.map((subject) => (
                    <tr key={subject.id}>
                      <td>{subject.id}</td>
                      <td>{subject.name}</td>
                      <td>{subject.teacherName}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel">
          <h2>Тестове</h2>
          <form className="form-grid" onSubmit={handleCreateTest}>
            <input
              type="text"
              placeholder="Заглавие"
              value={testForm.title}
              onChange={(event) => setTestForm((prev) => ({ ...prev, title: event.target.value }))}
              required
            />
            <input
              type="text"
              placeholder="Описание"
              value={testForm.description}
              onChange={(event) => setTestForm((prev) => ({ ...prev, description: event.target.value }))}
            />
            <select
              value={testForm.subjectId}
              onChange={(event) => setTestForm((prev) => ({ ...prev, subjectId: event.target.value }))}
              required
            >
              <option value="">Избери предмет</option>
              {subjects.map((subject) => (
                <option key={subject.id} value={subject.id}>
                  {subject.name}
                </option>
              ))}
            </select>
            <input
              type="number"
              min="1"
              max="300"
              placeholder="Време (минути)"
              value={testForm.timeLimitMinutes}
              onChange={(event) =>
                setTestForm((prev) => ({ ...prev, timeLimitMinutes: Number(event.target.value) || 30 }))
              }
            />
            <button type="submit" className="btn btn-primary">
              Създай тест
            </button>
          </form>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Тест</th>
                  <th>Предмет</th>
                  <th>Въпроси</th>
                  <th>Избери</th>
                  <th>Изтрий</th>
                </tr>
              </thead>
              <tbody>
                {tests.length === 0 ? (
                  <tr>
                    <td colSpan="6">Няма тестове.</td>
                  </tr>
                ) : (
                  tests.map((test) => (
                    <tr key={test.id}>
                      <td>{test.id}</td>
                      <td>{test.title}</td>
                      <td>{test.subjectName}</td>
                      <td>{test.questionsCount}</td>
                      <td>
                        <button
                          className="btn btn-primary"
                          type="button"
                          onClick={() => setSelectedTestId(String(test.id))}
                        >
                          Отвори
                        </button>
                      </td>
                      <td>
                        <button className="btn btn-danger" type="button" onClick={() => handleDeleteTest(test.id)}>
                          Изтрий
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel">
          <h2>Въпроси към тест</h2>
          <div className="form-grid">
            <select value={selectedTestId} onChange={(event) => setSelectedTestId(event.target.value)}>
              <option value="">Избери тест</option>
              {tests.map((test) => (
                <option key={test.id} value={test.id}>
                  #{test.id} {test.title}
                </option>
              ))}
            </select>
          </div>

          {selectedTestDetails ? (
            <>
              <p className="subtitle">
                Тест: <strong>{selectedTestDetails.title}</strong> | Предмет: {selectedTestDetails.subjectName} | Време: {" "}
                {selectedTestDetails.timeLimitMinutes} мин.
              </p>

              <form className="form-grid" onSubmit={handleAddManualQuestion}>
                <input
                  type="text"
                  placeholder="Текст на въпрос"
                  value={manualQuestionForm.questionText}
                  onChange={(event) =>
                    setManualQuestionForm((prev) => ({ ...prev, questionText: event.target.value }))
                  }
                  required
                />
                <input
                  type="number"
                  min="0.1"
                  step="0.1"
                  placeholder="Точки"
                  value={manualQuestionForm.points}
                  onChange={(event) =>
                    setManualQuestionForm((prev) => ({ ...prev, points: Number(event.target.value) || 1 }))
                  }
                />
                <input
                  type="number"
                  min="5"
                  max="3600"
                  placeholder="Време за отговор (сек.)"
                  value={manualQuestionForm.timeLimitSeconds}
                  onChange={(event) =>
                    setManualQuestionForm((prev) => ({
                      ...prev,
                      timeLimitSeconds: Number(event.target.value) || 60
                    }))
                  }
                />
                <input
                  type="number"
                  min="2"
                  max="8"
                  placeholder="Брой отговори"
                  value={manualQuestionForm.options.length}
                  onChange={(event) => handleManualOptionCountChange(event.target.value)}
                />

                {manualQuestionForm.options.map((option, index) => (
                  <label key={index} className="row-controls">
                    <input
                      type="checkbox"
                      checked={(manualQuestionForm.correctIndexes || []).includes(index)}
                      onChange={() => handleToggleCorrectOption(index)}
                    />
                    <span>Верен</span>
                    <input
                      type="text"
                      placeholder={`Отговор ${index + 1}`}
                      value={option}
                      onChange={(event) => {
                        const clone = [...manualQuestionForm.options];
                        clone[index] = event.target.value;
                        setManualQuestionForm((prev) => ({ ...prev, options: clone }));
                      }}
                    />
                  </label>
                ))}

                <button type="submit" className="btn btn-primary">
                  {editingQuestionId ? "Запази редакция" : "Добави ръчен въпрос"}
                </button>
                {editingQuestionId ? (
                  <button type="button" className="btn btn-ghost" onClick={cancelQuestionEdit}>
                    Откажи редакция
                  </button>
                ) : null}
              </form>

              <form className="form-grid" onSubmit={handleGenerateAiQuestions}>
                <input
                  type="text"
                  placeholder="AI тема (напр. Java колекции)"
                  value={aiQuestionForm.topic}
                  onChange={(event) => setAiQuestionForm((prev) => ({ ...prev, topic: event.target.value }))}
                  required
                />
                <input
                  type="text"
                  placeholder="Трудност"
                  value={aiQuestionForm.difficulty}
                  onChange={(event) =>
                    setAiQuestionForm((prev) => ({ ...prev, difficulty: event.target.value }))
                  }
                />
                <input
                  type="number"
                  min="1"
                  max="20"
                  value={aiQuestionForm.count}
                  onChange={(event) => setAiQuestionForm((prev) => ({ ...prev, count: Number(event.target.value) || 1 }))}
                />
                <input
                  type="number"
                  min="5"
                  max="3600"
                  placeholder="Време за AI въпрос (сек.)"
                  value={aiQuestionForm.timeLimitSeconds}
                  onChange={(event) =>
                    setAiQuestionForm((prev) => ({ ...prev, timeLimitSeconds: Number(event.target.value) || 60 }))
                  }
                />
                <button type="submit" className="btn btn-primary">
                  Генерирай AI въпроси
                </button>
              </form>

              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>№</th>
                      <th>Въпрос</th>
                      <th>Тип</th>
                      <th>Точки</th>
                      <th>Време (сек.)</th>
                      <th>Отговори</th>
                      <th>Редакция</th>
                      <th>Изтриване</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(selectedTestDetails.questions || []).length === 0 ? (
                      <tr>
                        <td colSpan="8">Няма добавени въпроси.</td>
                      </tr>
                    ) : (
                      (selectedTestDetails.questions || []).map((question) => (
                        <tr key={question.id}>
                          <td>{question.positionIndex}</td>
                          <td>{question.questionText}</td>
                          <td>{question.sourceType}</td>
                          <td>{question.points}</td>
                          <td>{question.timeLimitSeconds}</td>
                          <td>
                            <ol>
                              {(question.options || []).map((option) => (
                                <li key={option.id}>
                                  {option.optionText} {option.correct ? "(правилен)" : ""}
                                </li>
                              ))}
                            </ol>
                          </td>
                          <td>
                            <button className="btn btn-primary" type="button" onClick={() => handleEditQuestion(question)}>
                              Редакция
                            </button>
                          </td>
                          <td>
                            <button className="btn btn-danger" type="button" onClick={() => handleDeleteQuestion(question.id)}>
                              Изтрий
                            </button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </>
          ) : (
            <p className="subtitle">Избери тест, за да управляваш въпросите.</p>
          )}
        </section>

        <section className="panel">
          <h2>Групи</h2>
          <form className="form-grid" onSubmit={handleCreateGroup}>
            <input
              type="text"
              placeholder="Име на група"
              value={groupForm.name}
              onChange={(event) => setGroupForm((prev) => ({ ...prev, name: event.target.value }))}
              required
            />
            <button type="submit" className="btn btn-primary">
              Създай група
            </button>
          </form>

          {(groups || []).map((group) => (
            <div key={group.id} className="panel" style={{ marginTop: "0.8rem" }}>
              <h2>
                {group.groupName} (членове: {(group.members || []).length})
              </h2>
              <div className="row-controls">
                <select
                  value={memberByGroupId[group.id] || ""}
                  onChange={(event) =>
                    setMemberByGroupId((prev) => ({ ...prev, [group.id]: event.target.value }))
                  }
                >
                  <option value="">Избери ученик</option>
                  {students.map((student) => (
                    <option key={student.id} value={student.id}>
                      {student.name} ({student.username})
                    </option>
                  ))}
                </select>
                <button className="btn btn-primary" type="button" onClick={() => handleAddMember(group.id)}>
                  Добави
                </button>
              </div>

              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Име</th>
                      <th>Username</th>
                      <th>Премахване</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(group.members || []).length === 0 ? (
                      <tr>
                        <td colSpan="4">Няма ученици в групата.</td>
                      </tr>
                    ) : (
                      (group.members || []).map((member) => (
                        <tr key={member.id}>
                          <td>{member.id}</td>
                          <td>{member.name}</td>
                          <td>{member.username}</td>
                          <td>
                            <button
                              type="button"
                              className="btn btn-danger"
                              onClick={() => handleRemoveMember(group.id, member.id)}
                            >
                              Премахни
                            </button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          ))}
        </section>

        <section className="panel">
          <h2>Задаване на тест</h2>

          <form className="form-grid" onSubmit={handleAssignToStudent}>
            <select
              value={assignStudentForm.testId}
              onChange={(event) => setAssignStudentForm((prev) => ({ ...prev, testId: event.target.value }))}
              required
            >
              <option value="">Тест</option>
              {tests.map((test) => (
                <option key={test.id} value={test.id}>
                  {test.title}
                </option>
              ))}
            </select>
            <select
              value={assignStudentForm.studentId}
              onChange={(event) => setAssignStudentForm((prev) => ({ ...prev, studentId: event.target.value }))}
              required
            >
              <option value="">Ученик</option>
              {students.map((student) => (
                <option key={student.id} value={student.id}>
                  {student.name} ({student.username})
                </option>
              ))}
            </select>
            <input
              type="datetime-local"
              value={assignStudentForm.dueAt}
              onChange={(event) => setAssignStudentForm((prev) => ({ ...prev, dueAt: event.target.value }))}
            />
            <button type="submit" className="btn btn-primary">
              Задай на ученик
            </button>
          </form>

          <form className="form-grid" onSubmit={handleAssignToGroup}>
            <select
              value={assignGroupForm.testId}
              onChange={(event) => setAssignGroupForm((prev) => ({ ...prev, testId: event.target.value }))}
              required
            >
              <option value="">Тест</option>
              {tests.map((test) => (
                <option key={test.id} value={test.id}>
                  {test.title}
                </option>
              ))}
            </select>
            <select
              value={assignGroupForm.groupId}
              onChange={(event) => setAssignGroupForm((prev) => ({ ...prev, groupId: event.target.value }))}
              required
            >
              <option value="">Група</option>
              {groups.map((group) => (
                <option key={group.id} value={group.id}>
                  {group.groupName}
                </option>
              ))}
            </select>
            <input
              type="datetime-local"
              value={assignGroupForm.dueAt}
              onChange={(event) => setAssignGroupForm((prev) => ({ ...prev, dueAt: event.target.value }))}
            />
            <button type="submit" className="btn btn-primary">
              Задай на група
            </button>
          </form>
        </section>

        <section className="panel">
          <h2>Зададени тестове</h2>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Тест</th>
                  <th>Ученик</th>
                  <th>Група</th>
                  <th>Статус</th>
                  <th>Резултат</th>
                </tr>
              </thead>
              <tbody>
                {teacherAssignments.length === 0 ? (
                  <tr>
                    <td colSpan="6">Няма задания.</td>
                  </tr>
                ) : (
                  teacherAssignments.map((assignment) => (
                    <tr key={assignment.id}>
                      <td>{assignment.id}</td>
                      <td>{assignment.testTitle}</td>
                      <td>
                        {assignment.studentName} ({assignment.studentUsername})
                      </td>
                      <td>{assignment.groupName || "-"}</td>
                      <td>{assignment.latestStatus || "NOT_STARTED"}</td>
                      <td>{assignment.latestScorePercent ?? "-"}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel">
          <h2>Справки и статистика</h2>
          {teacherOverview ? (
            <div className="row-controls">
              <span className="status-active">Тестове: {teacherOverview.totalTests}</span>
              <span className="status-active">Задания: {teacherOverview.totalAssignments}</span>
              <span className="status-active">Завършени опити: {teacherOverview.completedAttempts}</span>
              <span className="status-active">Среден успех: {teacherOverview.averageScore}%</span>
            </div>
          ) : null}

          <div className="row-controls" style={{ marginTop: "0.7rem" }}>
            <select value={reportTestId} onChange={(event) => setReportTestId(event.target.value)}>
              <option value="">Избери тест за детайлен отчет</option>
              {tests.map((test) => (
                <option key={test.id} value={test.id}>
                  {test.title}
                </option>
              ))}
            </select>
            <button type="button" className="btn btn-primary" onClick={handleLoadReportByTest}>
              Зареди отчет
            </button>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Ученик</th>
                  <th>Статус</th>
                  <th>Оценка %</th>
                  <th>Точки</th>
                  <th>Нарушения</th>
                </tr>
              </thead>
              <tbody>
                {testReportRows.length === 0 ? (
                  <tr>
                    <td colSpan="5">Няма данни за отчет.</td>
                  </tr>
                ) : (
                  testReportRows.map((entry) => (
                    <tr key={entry.attemptId}>
                      <td>
                        {entry.studentName} ({entry.studentUsername})
                      </td>
                      <td>{entry.status}</td>
                      <td>{entry.scorePercent}</td>
                      <td>
                        {entry.earnedPoints}/{entry.totalPoints}
                      </td>
                      <td>{entry.violationsCount}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
      </>
    );
  }

  function renderStudentPanel() {
    return (
      <>
        <section className="panel">
          <div className="panel-title">
            <h2>Student панел</h2>
            <button type="button" className="btn btn-ghost" onClick={handleLogout}>
              Изход
            </button>
          </div>
          <p className="subtitle">
            Решаваш тестовете по един въпрос на страница. При смяна на таб/минимизиране текущият въпрос се
            маркира като грешен.
          </p>
        </section>

        <section className="panel">
          <h2>Моите зададени тестове</h2>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Тест</th>
                  <th>Предмет</th>
                  <th>Статус</th>
                  <th>Резултат</th>
                  <th>Действие</th>
                </tr>
              </thead>
              <tbody>
                {studentAssignments.length === 0 ? (
                  <tr>
                    <td colSpan="6">Няма зададени тестове.</td>
                  </tr>
                ) : (
                  studentAssignments.map((assignment) => (
                    <tr key={assignment.id}>
                      <td>{assignment.id}</td>
                      <td>{assignment.testTitle}</td>
                      <td>{assignment.subjectName}</td>
                      <td>{assignment.attemptStatus || "NOT_STARTED"}</td>
                      <td>{assignment.scorePercent ?? "-"}</td>
                      <td>
                        <button className="btn btn-primary" type="button" onClick={() => handleStartAttempt(assignment.id)}>
                          {assignment.attemptStatus === "COMPLETED" ? "Преглед" : "Старт"}
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>

        {activeAttemptId ? (
          <section className="panel">
            <h2>Решаване на тест</h2>

            {currentQuestionPayload?.completed ? (
              <>
                <p className="subtitle">Тестът е приключил.</p>
                <button className="btn btn-primary" type="button" onClick={handleFinishAttemptAndRefresh}>
                  Обнови резултат
                </button>
              </>
            ) : currentQuestionPayload?.question ? (
              <>
                <p>
                  Въпрос {currentQuestionPayload.currentPosition} / {currentQuestionPayload.totalQuestions}
                </p>
                <h3>{currentQuestionPayload.question.questionText}</h3>
                <div className="row-controls">
                  <span className="status-active">
                    Време: {currentQuestionPayload.question.timeLimitSeconds} сек.
                  </span>
                  <span className="status-inactive">
                    Остава: {typeof questionRemainingSeconds === "number" ? questionRemainingSeconds : "-"} сек.
                  </span>
                </div>

                <div className="row-controls" style={{ flexDirection: "column", alignItems: "stretch" }}>
                  {(currentQuestionPayload.options || []).map((option) => (
                    <label key={option.id} className="panel" style={{ margin: 0, boxShadow: "none" }}>
                      <input
                        type="checkbox"
                        value={option.id}
                        checked={selectedOptionIds.includes(String(option.id))}
                        onChange={(event) => {
                          const value = String(event.target.value);
                          setSelectedOptionIds((prev) => {
                            if (prev.includes(value)) {
                              return prev.filter((entry) => entry !== value);
                            }
                            return [...prev, value];
                          });
                        }}
                      />{" "}
                      {option.optionText}
                    </label>
                  ))}
                </div>

                <div className="row-controls" style={{ marginTop: "0.7rem" }}>
                  <button className="btn btn-primary" type="button" onClick={handleSubmitStudentAnswer}>
                    Потвърди и следващ
                  </button>
                  <span className="status-inactive">Нарушения: {currentQuestionPayload.violationsCount || 0}</span>
                </div>
              </>
            ) : (
              <p className="subtitle">Зареждане на текущ въпрос...</p>
            )}
          </section>
        ) : null}

        {attemptResult ? (
          <section className="panel">
            <h2>Резултат от теста</h2>
            <div className="row-controls">
              <span className="status-active">Резултат: {attemptResult.scorePercent}%</span>
              <span className="status-active">
                Точки: {attemptResult.earnedPoints}/{attemptResult.totalPoints}
              </span>
              <span className="status-inactive">Нарушения: {attemptResult.violationsCount}</span>
            </div>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Въпрос</th>
                    <th>Избран отговор</th>
                    <th>Точки</th>
                    <th>Нарушение</th>
                  </tr>
                </thead>
                <tbody>
                  {(attemptResult.answers || []).map((answer) => (
                    <tr key={answer.id}>
                      <td>{answer.questionText}</td>
                      <td>{answer.selectedOptionText || "-"}</td>
                      <td>{answer.earnedPoints}</td>
                      <td>{answer.violationReason || "-"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        ) : null}

        <section className="panel">
          <h2>История на резултати</h2>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Тест</th>
                  <th>Статус</th>
                  <th>Резултат %</th>
                </tr>
              </thead>
              <tbody>
                {studentResults.length === 0 ? (
                  <tr>
                    <td colSpan="3">Няма завършени тестове.</td>
                  </tr>
                ) : (
                  studentResults.map((entry) => (
                    <tr key={entry.id}>
                      <td>{entry.testTitle}</td>
                      <td>{entry.attemptStatus}</td>
                      <td>{entry.scorePercent}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
      </>
    );
  }

  return (
    <main className="layout">
      <header className="hero">
        <p className="badge">Smart Test • React + Spring</p>
        <h1>Интелигентна платформа за онлайн тестове</h1>
        <p className="subtitle">
          {isAuthenticated
            ? `Влязъл потребител: ${currentUser?.username} (${role})`
            : "Начална страница: регистрация и вход в системата."}
        </p>
      </header>

      {loading ? (
        <section className="panel">
          <p className="subtitle">Зареждане...</p>
        </section>
      ) : null}

      {!isAuthenticated ? renderAuth() : null}
      {isAuthenticated && isAdmin ? renderAdminPanel() : null}
      {isAuthenticated && isTeacher ? renderTeacherPanel() : null}
      {isAuthenticated && isStudent ? renderStudentPanel() : null}

      {hasMessage ? (
        <section className="panel">
          <p className={`message ${message.type}`}>{message.text}</p>
        </section>
      ) : null}
    </main>
  );
}
