package com.example.helloworld.service;

import com.example.helloworld.model.User;
import java.util.List;

public interface UserService {

    List<User> getAllUsers();

    User getUserById(Long id);

    User updateUserRole(Long id, String role);
}
