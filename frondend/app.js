class UserModel {
    constructor(apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    async getUsers() {
        return this.request("/users");
    }

    async updateRole(id, role) {
        return this.request(`/users/${id}/role`, {
            method: "PUT",
            body: JSON.stringify({role})
        });
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
        this.usersBody = document.querySelector("#users-body");
        this.reloadButton = document.querySelector("#reload-btn");
        this.message = document.querySelector("#message");
        this.defaultRoles = ["USER", "ADMIN", "MODERATOR", "GUEST"];
    }

    bindReload(handler) {
        this.reloadButton.addEventListener("click", handler);
    }

    bindTableActions({onUpdateRole}) {
        this.usersBody.addEventListener("click", (event) => {
            const updateBtn = event.target.closest("[data-action='update']");

            if (updateBtn) {
                const row = updateBtn.closest("tr");
                const id = Number(row.dataset.userId);
                const select = row.querySelector("select");
                onUpdateRole(id, select.value);
            }
        });
    }

    renderUsers(users) {
        if (!users.length) {
            this.usersBody.innerHTML = `
                <tr>
                    <td colspan="3">Няма намерени потребители.</td>
                </tr>
            `;
            return;
        }

        this.usersBody.innerHTML = users.map((user) => `
            <tr data-user-id="${user.id}">
                <td>${user.id}</td>
                <td>
                    ${this.escapeHtml(user.username)}
                </td>
                <td>
                    <div class="row-controls">
                        <select>
                            ${this.roleOptions(user.role)}
                        </select>
                        <button type="button" class="btn btn-primary" data-action="update">Запази роля</button>
                    </div>
                </td>
            </tr>
        `).join("");
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

    roleOptions(currentRole) {
        const normalizedRole = (currentRole || "").toUpperCase();
        const roles = [...this.defaultRoles];
        if (normalizedRole && !roles.includes(normalizedRole)) {
            roles.push(normalizedRole);
        }

        return roles.map((role) => `
            <option value="${role}" ${role === normalizedRole ? "selected" : ""}>${role}</option>
        `).join("");
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

        this.view.bindReload(() => this.loadUsers());
        this.view.bindTableActions({
            onUpdateRole: (id, role) => this.updateRole(id, role)
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

    async updateRole(id, role) {
        try {
            await this.model.updateRole(id, role);
            this.view.showMessage("Ролята е обновена успешно.");
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
