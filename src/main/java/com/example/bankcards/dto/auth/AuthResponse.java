package com.example.bankcards.dto.auth;

public record AuthResponse(String token) {
}
//	Возвращаем только JWT — фронту достаточно положить его в Authorization: Bearer
