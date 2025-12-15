package com.example.mini_drive_storage.controller;

import com.example.mini_drive_storage.entity.Users;
import com.example.mini_drive_storage.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class UserController {
    private UserService userService;
    @PostMapping("/auth/register")
    public Users registerUser(@RequestBody Users user){
        return userService.register(user);
    }

    @PostMapping("/auth/login")
    public  String login(@RequestBody Users user){
        return userService.verify(user);
    }
}
