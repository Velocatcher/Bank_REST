package com.example.bankcards.controller;

import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.auth.LoginRequest;
import com.example.bankcards.dto.auth.RegisterRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.security.JwtService;
import com.example.bankcards.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Swagger/OpenAPI аннотации
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Auth", description = "Authentication endpoints")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          JwtService jwtService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Operation(
            summary = "Register user",
            description = "Создаёт пользователя и сразу возвращает JWT",
            security = {} // снимаем глобальную безопасность для этого метода
    )
    @PostMapping(value = "/register", consumes = "application/json", produces = "application/json")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest req) {
        User u = userService.register(req.username(), req.password());
        String token = jwtService.generateToken(u.getUsername(), u.getRoles());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @Operation(
            summary = "Login",
            description = "Аутентификация по username/password. Возвращает JWT",
            security = {} // снимаем глобальную безопасность для этого метода
    )
    @PostMapping(value = "/login", consumes = "application/json", produces = "application/json")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );
        User u = userService.byUsername(auth.getName());
        String token = jwtService.generateToken(u.getUsername(), u.getRoles());
        return ResponseEntity.ok(new AuthResponse(token));
    }
}



//package com.example.bankcards.controller;
//
//import com.example.bankcards.dto.auth.AuthResponse;
//import com.example.bankcards.dto.auth.LoginRequest;
//import com.example.bankcards.dto.auth.RegisterRequest;
//import com.example.bankcards.entity.User;
//import com.example.bankcards.security.JwtService;
//import com.example.bankcards.service.UserService;
//import jakarta.validation.Valid;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.*;
//
//
//@RestController
//@RequestMapping("/api/auth")
//public class AuthController {
//    private final UserService userService;
//    private final AuthenticationManager authenticationManager;
//    private final JwtService jwtService;
//
//    public AuthController(UserService userService, AuthenticationManager authenticationManager, JwtService jwtService) {
//        this.userService = userService;
//        this.authenticationManager = authenticationManager;
//        this.jwtService = jwtService;
//    }
//
//    @PostMapping("/register")
//    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest req) {
//        User u = userService.register(req.username(), req.password());
//        String token = jwtService.generateToken(u.getUsername(), u.getRoles());
//        return ResponseEntity.ok(new AuthResponse(token));
//    }
//
//    @PostMapping("/login")
//    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest req) {
//        // правильно
//        Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.username(), req.password()));
//        User u = userService.byUsername(auth.getName());
//        String token = jwtService.generateToken(u.getUsername(), u.getRoles());
//        return ResponseEntity.ok(new AuthResponse(token));
//    }
//
//}
