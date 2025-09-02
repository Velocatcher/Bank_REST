package com.example.bankcards.controller;

import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.auth.LoginRequest;
import com.example.bankcards.dto.auth.RegisterRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.security.JwtService;
import com.example.bankcards.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerUnitTest {

    @Mock private UserService userService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthController controller;

    private User mkUser(String username, Set<Role> roles) {
        User u = new User();
        u.setUsername(username);
        u.setRoles(roles);
        return u;
    }

    @BeforeEach
    void resetMocks() { Mockito.reset(userService, authenticationManager, jwtService); }

    @Test
    void register_returnsJwtAnd200_andCallsDependenciesWithExactArgs() {
        // given
        String username = "alice";
        String rawPwd   = "p@ss";
        Set<Role> roles = new HashSet<>(); // можно оставить пустым — важна передача ровно того же Set
        User saved = mkUser(username, roles);

        when(userService.register(username, rawPwd)).thenReturn(saved);
        when(jwtService.generateToken(username, roles)).thenReturn("JWT-123");

        // when
        ResponseEntity<AuthResponse> resp =
                controller.register(new RegisterRequest(username, rawPwd));

        // then
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().token()).isEqualTo("JWT-123");

        // verify exact calls / order
        InOrder inOrder = inOrder(userService, jwtService);
        inOrder.verify(userService).register(eq(username), eq(rawPwd));
        inOrder.verify(jwtService).generateToken(eq(username), same(roles)); // тот же Set-объект
        inOrder.verifyNoMoreInteractions();
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void login_success_returnsJwt200_andPassesExactAuthTokenToAuthenticationManager() {
        // given
        String username = "admin";
        String rawPwd   = "secret";
        Set<Role> roles = new HashSet<>();
        User found = mkUser(username, roles);

        // В ответе от authenticationManager нам достаточно getName()
        Authentication returnedAuth =
                new UsernamePasswordAuthenticationToken(username, rawPwd);

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(returnedAuth);
        when(userService.byUsername(username)).thenReturn(found);
        when(jwtService.generateToken(username, roles)).thenReturn("JWT-OK");

        // when
        ResponseEntity<AuthResponse> resp =
                controller.login(new LoginRequest(username, rawPwd));

        // then
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().token()).isEqualTo("JWT-OK");

        // verify: пришёл ровно UsernamePasswordAuthenticationToken с нужными полями
        ArgumentCaptor<Authentication> authCap = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authCap.capture());
        Authentication passed = authCap.getValue();
        assertThat(passed).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(passed.getName()).isEqualTo(username);
        assertThat(String.valueOf(passed.getCredentials())).isEqualTo(rawPwd);

        InOrder order = inOrder(authenticationManager, userService, jwtService);
        order.verify(authenticationManager).authenticate(any(Authentication.class));
        order.verify(userService).byUsername(eq(username));
        order.verify(jwtService).generateToken(eq(username), same(roles));
        order.verifyNoMoreInteractions();
    }

    @Test
    void login_badCredentials_bubblesException() {
        // given
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad creds"));

        // when/then
        assertThrows(BadCredentialsException.class,
                () -> controller.login(new LoginRequest("x", "y")));

        verify(authenticationManager).authenticate(any(Authentication.class));
        verifyNoInteractions(userService, jwtService);
    }
}
