package com.example.bankcards.config;

import com.example.bankcards.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;}

//    Определяем бин цепочки фильтров — главный объект, который описывает, как обрабатывать HTTP-запросы.
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    /** Отключаем CSRF-защиту. Она нужна для форм на cookie-сессиях; для чистого REST + JWT обычно не нужна,
                    * иначе помешает POST/PUT/PATCH/DELETE.почему: JWT, нет cookie-сессии
                     */
                    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        //            Полностью статлесс: сервер не хранит сессию; каждый запрос должен нести JWT.
                    .authorizeHttpRequests(reg -> reg
                            .requestMatchers("/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                            .anyRequest().authenticated())
                    /**
                    * Правила доступа:
                    * permitAll() для аутентификации и Swagger/OpenAPI — удобно тестировать без токена.
                    * Любые другие запросы требуют аутентификации (наличие валидного JWT).
                     */
                    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
                    /**
                    * Вставляем наш JWT-фильтр перед стандартным UsernamePasswordAuthenticationFilter, чтобы:
                    * вытащить пользователя из токена и положить в SecurityContext
                    * дальнейшие фильтры видели, что пользователь уже аутентифицирован.
                     */
            return http.build(); //Строим финальный SecurityFilterChain — Spring поднимет его как бин.
        }

        @Bean
        public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
            return cfg.getAuthenticationManager();
        }
    }



