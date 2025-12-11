package com.example.mini_drive_storage.service;

import com.example.mini_drive_storage.entity.UserPrincipal;
import com.example.mini_drive_storage.entity.Users;
import com.example.mini_drive_storage.repo.UserRepo;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MyUserDetailsService implements UserDetailsService {
    private UserRepo userRepo;
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Users users = userRepo.findByEmail(email);
        if (users == null) {
            System.out.println("Email not found");
            throw new UsernameNotFoundException(email + " not found");
        }
        return new UserPrincipal(users);
    }
}
