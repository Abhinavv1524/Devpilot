package com.devpilot.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.devpilot.auth.dto.LoginRequest;
import com.devpilot.auth.dto.RegisterRequest;
import com.devpilot.auth.dto.TokenResponse;
import com.devpilot.auth.dto.UserResponse;
import com.devpilot.auth.entity.AppUser;
import com.devpilot.auth.exception.DuplicateUserException;
import com.devpilot.auth.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateUserException("Username is already in use");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateUserException("Email is already in use");
        }

        AppUser user = new AppUser(request.username(), request.email(), passwordEncoder.encode(request.password()));
        AppUser savedUser = userRepository.save(user);
        return new UserResponse(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());
    }

    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        return jwtService.createToken(request.username());
    }

}
