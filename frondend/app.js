class UserModel {
    constructor(apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    async getUsers() {
        return this.request("/users");
    }

    async createUser(payload) {
        return this.request("/users", {
            method: "POST",
            body: JSON.stringify(payload)
        });
    }

    async updateRole(id, role) {
        return this.request(`/users/${id}/role`, {
            method: "PUT",
            body: JSON.stringify({role})
        });
    }

    async updatePassword(id, password) {
        return this.request(`/users/${id}/password`, {
            method: "PUT",
            body: JSON.stringify({password})
        });
    }

    async deleteUser(id) {
        await this.request(`/users/${id}`, {method: "DELETE"});
    }

    async request(path, options = {}) {
        const response = await fetch(`${this.apiBaseUrl}${path}`, {
            headers: {"Content-Type": "application/json"},
            ...options
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
}

class UserView {
    constructor() {
        this.createForm = document.querySelector("#create-form");
        this.reloadButton = document.querySelector("#reload-btn");
        this.usersBody = document.querySelector("#users-body");
        this.message = document.querySelector("#message");
        this.defaultRoles = ["USER", "ADMIN", "MODERATOR", "GUEST"];
    }

    bindCreate(handler) {
        this.createForm.addEventListener("submit", (event) => {
            event.preventDefault();
            const formData = new FormData(this.createForm);
            handler({
                username: String(formData.get("username") || ""),
                name: String(formData.get("name") || ""),
                email: String(formData.get("email") || ""),
                password: String(formData.get("password") || ""),
                role: String(formData.get("role") || "")
            });
        });
    }

    bindReload(handler) {
        this.reloadButton.addEventListener("click", handler);
    }

    bindTableActions({onUpdateRole, onUpdatePassword, onDeleteUser}) {
        this.usersBody.addEventListener("click", (event) => {
            const roleBtn = event.target.closest("[data-action='update-role']");
            const passwordBtn = event.target.closest("[data-action='update-password']");
            const deleteBtn = event.target.closest("[data-action='delete-user']");
            const row = event.target.closest("tr");
            if (!row) {
                return;
            }

            const id = Number(row.dataset.userId);
            if (Number.isNaN(id)) {
                return;
            }

            if (roleBtn) {
                const roleSelect = row.querySelector("select[data-role-select]");
                onUpdateRole(id, roleSelect.value);
            }

            if (passwordBtn) {
                const passwordInput = row.querySelector("input[data-password-input]");
                onUpdatePassword(id, passwordInput.value);
            }

            if (deleteBtn) {
                onDeleteUser(id);
            }
        });
    }

    renderUsers(users) {
        if (!users.length) {
            this.usersBody.innerHTML = `
                <tr>
                    <td colspan="7">Няма потребители в таблицата users.</td>
                </tr>
            `;
            return;
        }

        this.usersBody.innerHTML = users.map((user) => `
            <tr data-user-id="${user.id}">
                <td>${user.id}</td>
                <td>${this.escapeHtml(user.username || "")}</td>
                <td>${this.escapeHtml(user.name || "")}</td>
                <td>${this.escapeHtml(user.email || "")}</td>
                <td>
                    <div class="row-controls">
                        <select data-role-select>
                            ${this.roleOptions(user.role)}
                        </select>
                        <button type="button" class="btn btn-primary" data-action="update-role">Запази роля</button>
                    </div>
                </td>
                <td>
                    <div class="row-controls">
                        <input data-password-input type="password" placeholder="Нова парола" />
                        <button type="button" class="btn btn-primary" data-action="update-password">Смени парола</button>
                    </div>
                </td>
                <td>
                    <button type="button" class="btn btn-danger" data-action="delete-user">Изтрий</button>
                </td>
            </tr>
        `).join("");
    }

    roleOptions(currentRole) {
        const normalizedRole = (currentRole || "USER").toUpperCase();
        const roles = [...this.defaultRoles];
        if (!roles.includes(normalizedRole)) {
            roles.push(normalizedRole);
        }
        return roles.map((role) =>
            `<option value="${role}" ${role === normalizedRole ? "selected" : ""}>${role}</option>`
        ).join("");
    }

    showMessage(text, type = "success") {
        this.message.textContent = text;
        this.message.className = `message ${type}`;
        this.message.hidden = false;
    }

    clearMessage() {
        this.message.hidden = true;
        this.message.textContent = "";
        this.message.className = "message";
    }

    resetCreateForm() {
        this.createForm.reset();
    }

    escapeHtml(value) {
        return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#39;");
    }
}

class UserController {
    constructor(model, view) {
        this.model = model;
        this.view = view;

        this.view.bindCreate((payload) => this.createUser(payload));
        this.view.bindReload(() => this.loadUsers());
        this.view.bindTableActions({
            onUpdateRole: (id, role) => this.updateRole(id, role),
            onUpdatePassword: (id, password) => this.updatePassword(id, password),
            onDeleteUser: (id) => this.deleteUser(id)
        });
    }

    async initialize() {
        await this.loadUsers();
    }

    async loadUsers() {
        try {
            this.view.clearMessage();
            const users = await this.model.getUsers();
            this.view.renderUsers(users);
        } catch (error) {
            this.view.showMessage(error.message, "error");
        }
    }

    async createUser(payload) {
        try {
            await this.model.createUser(payload);
            this.view.showMessage("Потребителят е създаден успешно.");
            this.view.resetCreateForm();
            await this.loadUsers();
        } catch (error) {
            this.view.showMessage(error.message, "error");
        }
    }

    async updateRole(id, role) {
        try {
            await this.model.updateRole(id, role);
            this.view.showMessage("Ролята е обновена успешно.");
            await this.loadUsers();
        } catch (error) {
            this.view.showMessage(error.message, "error");
        }
    }

    async updatePassword(id, password) {
        try {
            await this.model.updatePassword(id, password);
            this.view.showMessage("Паролата е обновена успешно.");
            await this.loadUsers();
        } catch (error) {
            this.view.showMessage(error.message, "error");
        }
    }

    async deleteUser(id) {
        if (!window.confirm("Сигурен ли си, че искаш да изтриеш този потребител?")) {
            return;
        }

        try {
            await this.model.deleteUser(id);
            this.view.showMessage("Потребителят е изтрит успешно.");
            await this.loadUsers();
        } catch (error) {
            this.view.showMessage(error.message, "error");
        }
    }
}

(async function bootstrap() {
    const apiBaseUrl = "http://localhost:8081/api";
    const model = new UserModel(apiBaseUrl);
    const view = new UserView();
    const controller = new UserController(model, view);
    await controller.initialize();
})();
