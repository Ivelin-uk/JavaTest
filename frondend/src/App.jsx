import { useEffect, useMemo, useState } from "react";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8081/api";
const DEFAULT_ROLES = ["USER", "ADMIN", "MODERATOR", "GUEST"];
const AUTH_TOKEN_KEY = "admin_panel_auth_token";

function buildBasicToken(username, password) {
  return `Basic ${btoa(`${username}:${password}`)}`;
}

async function request(path, options = {}, authToken) {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  if (authToken) {
    headers.Authorization = authToken;
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers
  });

  if (response.status === 204) {
    return null;
  }

  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(data?.message || "Грешка при връзка със сървъра.");
  }

  return data;
}

export default function App() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ text: "", type: "success" });
  const [authToken, setAuthToken] = useState(localStorage.getItem(AUTH_TOKEN_KEY) || "");
  const [loginForm, setLoginForm] = useState({ username: "", password: "" });
  const [createForm, setCreateForm] = useState({
    username: "",
    name: "",
    email: "",
    password: "",
    role: "USER"
  });
  const [roleById, setRoleById] = useState({});
  const [passwordById, setPasswordById] = useState({});
  const [activeById, setActiveById] = useState({});

  const isAuthenticated = Boolean(authToken);
  const hasMessage = Boolean(message.text);

  async function loadUsers(token = authToken) {
    if (!token) {
      setUsers([]);
      return;
    }

    setLoading(true);
    try {
      const result = await request("/users", {}, token);
      setUsers(result);

      const nextRoles = {};
      const nextActive = {};
      result.forEach((user) => {
        nextRoles[user.id] = (user.role || "USER").toUpperCase();
        nextActive[user.id] = Boolean(user.active);
      });

      setRoleById(nextRoles);
      setActiveById(nextActive);
      setMessage((prev) => (prev.type === "error" ? prev : { text: "", type: "success" }));
    } catch (error) {
      setMessage({ text: error.message, type: "error" });
      if (error.message.includes("Неоторизиран")) {
        handleLogout();
      }
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (authToken) {
      loadUsers(authToken);
    }
  }, [authToken]);

  const roleOptionsByUser = useMemo(() => {
    const map = {};
    users.forEach((user) => {
      const role = (roleById[user.id] || "USER").toUpperCase();
      const options = [...DEFAULT_ROLES];
      if (!options.includes(role)) {
        options.push(role);
      }
      map[user.id] = options;
    });
    return map;
  }, [users, roleById]);

  async function handleLogin(event) {
    event.preventDefault();
    const token = buildBasicToken(loginForm.username.trim(), loginForm.password);
    try {
      await request("/users", {}, token);
      localStorage.setItem(AUTH_TOKEN_KEY, token);
      setAuthToken(token);
      setLoginForm({ username: "", password: "" });
      setMessage({ text: "Успешен вход.", type: "success" });
    } catch (error) {
      setMessage({ text: error.message, type: "error" });
    }
  }

  function handleLogout() {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    setAuthToken("");
    setUsers([]);
    setRoleById({});
    setPasswordById({});
    setActiveById({});
  }

  async function handleCreateUser(event) {
    event.preventDefault();
    try {
      await request("/users", {
        method: "POST",
        body: JSON.stringify(createForm)
      }, authToken);

      setCreateForm({
        username: "",
        name: "",
        email: "",
        password: "",
        role: "USER"
      });
      setMessage({ text: "Потребителят е създаден успешно.", type: "success" });
      await loadUsers();
    } catch (error) {
      setMessage({ text: error.message, type: "error" });
    }
  }

  async function handleUpdateRole(userId) {
    try {
      await request(`/users/${userId}/role`, {
        method: "PUT",
        body: JSON.stringify({ role: roleById[userId] || "USER" })
      }, authToken);
      setMessage({ text: "Ролята е обновена успешно.", type: "success" });
      await loadUsers();
    } catch (error) {
      setMessage({ text: error.message, type: "error" });
    }
  }

  async function handleUpdatePassword(userId) {
    const password = passwordById[userId] || "";
    if (!password.trim()) {
      setMessage({ text: "Въведи нова парола.", type: "error" });
      return;
    }

    try {
      await request(`/users/${userId}/password`, {
        method: "PUT",
        body: JSON.stringify({ password })
      }, authToken);
      setPasswordById((prev) => ({ ...prev, [userId]: "" }));
      setMessage({ text: "Паролата е обновена успешно.", type: "success" });
      await loadUsers();
    } catch (error) {
      setMessage({ text: error.message, type: "error" });
    }
  }

  async function handleToggleActivation(userId) {
    const currentActive = Boolean(activeById[userId]);
    const nextActive = !currentActive;
    try {
      await request(`/users/${userId}/activation`, {
        method: "PUT",
        body: JSON.stringify({ active: nextActive })
      }, authToken);
      setMessage({
        text: nextActive ? "Потребителят е активиран." : "Потребителят е деактивиран.",
        type: "success"
      });
      await loadUsers();
    } catch (error) {
      setMessage({ text: error.message, type: "error" });
    }
  }

  async function handleDeleteUser(userId) {
    if (!window.confirm("Сигурен ли си, че искаш да изтриеш този потребител?")) {
      return;
    }

    try {
      await request(`/users/${userId}`, { method: "DELETE" }, authToken);
      setMessage({ text: "Потребителят е изтрит успешно.", type: "success" });
      await loadUsers();
    } catch (error) {
      setMessage({ text: error.message, type: "error" });
    }
  }

  return (
    <main className="layout">
      <header className="hero">
        <p className="badge">Frontend MVC (React)</p>
        <h1>Администраторски панел</h1>
        <p className="subtitle">
          Създаване и изтриване на потребители, смяна на парола, смяна на роля и
          активиране/деактивиране.
        </p>
      </header>

      {!isAuthenticated ? (
        <section className="panel">
          <h2>Вход (ADMIN)</h2>
          <form className="form-grid" onSubmit={handleLogin}>
            <input
              type="text"
              placeholder="Username или Email"
              value={loginForm.username}
              onChange={(event) =>
                setLoginForm((prev) => ({ ...prev, username: event.target.value }))
              }
              required
            />
            <input
              type="password"
              placeholder="Парола"
              value={loginForm.password}
              onChange={(event) =>
                setLoginForm((prev) => ({ ...prev, password: event.target.value }))
              }
              required
            />
            <button type="submit" className="btn btn-primary">
              Вход
            </button>
          </form>
        </section>
      ) : (
        <>
          <section className="panel">
            <div className="panel-title">
              <h2>Създаване на потребител</h2>
              <button type="button" className="btn btn-ghost" onClick={handleLogout}>
                Изход
              </button>
            </div>
            <form className="form-grid" onSubmit={handleCreateUser}>
              <input
                name="username"
                type="text"
                placeholder="Username"
                value={createForm.username}
                onChange={(event) =>
                  setCreateForm((prev) => ({ ...prev, username: event.target.value }))
                }
                required
              />
              <input
                name="name"
                type="text"
                placeholder="Име"
                value={createForm.name}
                onChange={(event) =>
                  setCreateForm((prev) => ({ ...prev, name: event.target.value }))
                }
                required
              />
              <input
                name="email"
                type="email"
                placeholder="Email"
                value={createForm.email}
                onChange={(event) =>
                  setCreateForm((prev) => ({ ...prev, email: event.target.value }))
                }
                required
              />
              <input
                name="password"
                type="password"
                placeholder="Парола (мин. 6)"
                value={createForm.password}
                onChange={(event) =>
                  setCreateForm((prev) => ({ ...prev, password: event.target.value }))
                }
                required
              />
              <select
                name="role"
                value={createForm.role}
                onChange={(event) =>
                  setCreateForm((prev) => ({ ...prev, role: event.target.value }))
                }
              >
                {DEFAULT_ROLES.map((role) => (
                  <option key={role} value={role}>
                    {role}
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
              <button className="btn btn-ghost" type="button" onClick={() => loadUsers()}>
                {loading ? "Зарежда..." : "Обнови"}
              </button>
            </div>

            {hasMessage ? (
              <p className={`message ${message.type}`}>{message.text}</p>
            ) : null}

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
                      <td colSpan="8">Няма потребители в таблицата users.</td>
                    </tr>
                  ) : (
                    users.map((user) => {
                      const isActive = Boolean(activeById[user.id]);
                      return (
                        <tr key={user.id}>
                          <td>{user.id}</td>
                          <td>{user.username || ""}</td>
                          <td>{user.name || ""}</td>
                          <td>{user.email || ""}</td>
                          <td>
                            <div className="row-controls">
                              <select
                                value={roleById[user.id] || "USER"}
                                onChange={(event) =>
                                  setRoleById((prev) => ({
                                    ...prev,
                                    [user.id]: event.target.value
                                  }))
                                }
                              >
                                {(roleOptionsByUser[user.id] || DEFAULT_ROLES).map((role) => (
                                  <option key={role} value={role}>
                                    {role}
                                  </option>
                                ))}
                              </select>
                              <button
                                type="button"
                                className="btn btn-primary"
                                onClick={() => handleUpdateRole(user.id)}
                              >
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
                                type="button"
                                className={`btn ${isActive ? "btn-danger" : "btn-primary"}`}
                                onClick={() => handleToggleActivation(user.id)}
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
                                value={passwordById[user.id] || ""}
                                onChange={(event) =>
                                  setPasswordById((prev) => ({
                                    ...prev,
                                    [user.id]: event.target.value
                                  }))
                                }
                              />
                              <button
                                type="button"
                                className="btn btn-primary"
                                onClick={() => handleUpdatePassword(user.id)}
                              >
                                Смени
                              </button>
                            </div>
                          </td>
                          <td>
                            <button
                              type="button"
                              className="btn btn-danger"
                              onClick={() => handleDeleteUser(user.id)}
                            >
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
        </>
      )}

      {!isAuthenticated && hasMessage ? (
        <section className="panel">
          <p className={`message ${message.type}`}>{message.text}</p>
        </section>
      ) : null}
    </main>
  );
}
