package com.example.bankcards.service;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты UserService без Spring-контекста.
 * Проверяем все ветки: happy path, валидацию и not-found.
 */
@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository repo;

    @Mock
    PasswordEncoder encoder;

    @Captor
    ArgumentCaptor<User> userCaptor;

    UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(repo, encoder);
    }

    @Test
    @DisplayName("register(): OK -> сохраняет пользователя с ROLE_USER и зашифрованным паролем")
    void register_ok() {
        // given
        String username = "john";
        String raw = "secret123";
        String encoded = "ENC(secret123)";

        when(repo.existsByUsername(username)).thenReturn(false);
        when(encoder.encode(raw)).thenReturn(encoded);
        // возвращаем тот же объект, что пришел в save (как бы JPA присвоит id и вернет)
        when(repo.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0, User.class);
            u.setId(42L);
            return u;
        });

        // when
        User saved = service.register(username, raw);

        // then: возвращаемый объект корректен
        assertNotNull(saved.getId());
        assertEquals(username, saved.getUsername());
        assertEquals(encoded, saved.getPassword());
        assertEquals(Set.of(Role.USER), saved.getRoles());

        // verify: вызовы ровно как в сервисе (существование -> encode -> save)
        InOrder inOrder = inOrder(repo, encoder);
        inOrder.verify(repo).existsByUsername(username);
        inOrder.verify(encoder).encode(raw);
        inOrder.verify(repo).save(userCaptor.capture());
        inOrder.verifyNoMoreInteractions();

        // а в save ушёл уже правильно собранный User
        User toSave = userCaptor.getValue();
        assertEquals(username, toSave.getUsername());
        assertEquals(encoded, toSave.getPassword());
        assertEquals(Set.of(Role.USER), toSave.getRoles());
    }

    @Test
    @DisplayName("register(): username слишком короткий -> BadRequest и НИКАКИХ вызовов repo/encoder")
    void register_usernameTooShort() {
        assertThrows(BadRequestException.class, () -> service.register("ab", "whatever"));
        verifyNoInteractions(repo, encoder);
    }

    @Test
    @DisplayName("register(): password слишком короткий -> BadRequest, existsByUsername/encode/save не вызываются")
    void register_passwordTooShort() {
        assertThrows(BadRequestException.class, () -> service.register("john", "123"));
        verify(repo, never()).existsByUsername(any());
        verify(encoder, never()).encode(any());
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("register(): username занят -> BadRequest и save/encode не вызываются")
    void register_usernameTaken() {
        when(repo.existsByUsername("john")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.register("john", "secret123"));

        verify(repo).existsByUsername("john");
        verify(encoder, never()).encode(any());
        verify(repo, never()).save(any());
        verifyNoMoreInteractions(repo, encoder);
    }

    @Test
    @DisplayName("byUsername(): найден -> вернуть пользователя")
    void byUsername_found() {
        User u = new User();
        u.setId(7L);
        u.setUsername("alice");
        u.setPassword("x");
        u.setRoles(Set.of(Role.USER));

        when(repo.findByUsername("alice")).thenReturn(Optional.of(u));

        User actual = service.byUsername("alice");

        assertSame(u, actual);
        verify(repo).findByUsername("alice");
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(encoder);
    }

    @Test
    @DisplayName("byUsername(): не найден -> NotFoundException")
    void byUsername_notFound() {
        when(repo.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.byUsername("ghost"));

        verify(repo).findByUsername("ghost");
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(encoder);
    }
}
