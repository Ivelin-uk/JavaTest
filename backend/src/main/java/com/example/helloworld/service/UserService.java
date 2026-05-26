package com.example.helloworld.service;

import com.example.helloworld.model.User;
import java.util.List;

public interface UserService {

    List<User> getAllUsers();

    User getUserById(Long id);

    User createUser(String username, String name, String email, String plainPassword, String role);

    User updateUserRole(Long id, String role);

    User updateUserPassword(Long id, String plainPassword);

    void deleteUser(Long id);
}
