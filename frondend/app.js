class UserModel {
    constructor(apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    async getUsers() {
        return this.request("/users");
    }

    async createUser(username) {
        return this.request("/users", {
            method: "POST",
            body: JSON.stringify({username})
        });
    }

    async updateUser(id, username) {
        return this.request(`/users/${id}`, {
            method: "PUT",
            body: JSON.stringify({username})
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
        this.form = document.querySelector("#create-form");
        this.createInput = document.querySelector("#create-username");
        this.usersBody = document.querySelector("#users-body");
        this.reloadButton = document.querySelector("#reload-btn");
        this.message = document.querySelector("#message");
    }

    bindCreate(handler) {
        this.form.addEventListener("submit", (event) => {
            event.preventDefault();
            handler(this.createInput.value);
        });
    }

    bindReload(handler) {
        this.reloadButton.addEventListener("click", handler);
    }

    bindTableActions({onUpdate, onDelete}) {
        this.usersBody.addEventListener("click", (event) => {
            const updateBtn = event.target.closest("[data-action='update']");
            const deleteBtn = event.target.closest("[data-action='delete']");

            if (updateBtn) {
                const row = updateBtn.closest("tr");
                const id = row.dataset.userId;
                const input = row.querySelector("input");
                onUpdate(Number(id), input.value);
            }

            if (deleteBtn) {
                const row = deleteBtn.closest("tr");
                const id = Number(row.dataset.userId);
                onDelete(id);
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
                    <input type="text" value="${this.escapeHtml(user.username)}" />
                </td>
                <td>
                    <div class="row-controls">
                        <button type="button" class="btn btn-primary" data-action="update">Запази</button>
                        <button type="button" class="btn btn-danger" data-action="delete">Изтрий</button>
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

    clearCreateInput() {
        this.createInput.value = "";
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

        this.view.bindCreate((username) => this.createUser(username));
        this.view.bindReload(() => this.loadUsers());
        this.view.bindTableActions({
            onUpdate: (id, username) => this.updateUser(id, username),
            onDelete: (id) => this.deleteUser(id)
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

    async createUser(username) {
        try {
            await this.model.createUser(username);
            this.view.showMessage("Потребителят е създаден успешно.");
            this.view.clearCreateInput();
            await this.loadUsers();
        } catch (error) {
            this.view.showMessage(error.message, "error");
        }
    }

    async updateUser(id, username) {
        try {
            await this.model.updateUser(id, username);
            this.view.showMessage("Потребителят е редактиран успешно.");
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
