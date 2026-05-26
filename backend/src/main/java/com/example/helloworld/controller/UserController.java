package com.example.helloworld.controller;

import com.example.helloworld.dto.CreateUserRequest;
import com.example.helloworld.dto.UpdateActivationRequest;
import com.example.helloworld.dto.UpdatePasswordRequest;
import com.example.helloworld.dto.UpdateRoleRequest;
import com.example.helloworld.model.User;
import com.example.helloworld.service.UserService;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(
                request.username(),
                request.name(),
                request.email(),
                request.password(),
                request.role()
        );
    }

    @PutMapping("/{id}/role")
    public User updateUserRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return userService.updateUserRole(id, request.role());
    }

    @PutMapping("/{id}/password")
    public User updateUserPassword(@PathVariable Long id, @Valid @RequestBody UpdatePasswordRequest request) {
        return userService.updateUserPassword(id, request.password());
    }

    @PutMapping("/{id}/activation")
    public User updateUserActivation(@PathVariable Long id, @Valid @RequestBody UpdateActivationRequest request) {
        return userService.updateUserActivation(id, request.active());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }
}
