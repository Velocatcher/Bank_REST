package com.example.bankcards.dto.error;

import java.time.OffsetDateTime;

//	Единый формат ошибки упрощает обработку на фронте и логирование
public record ApiError(String message, String path, OffsetDateTime timestamp) {
}
