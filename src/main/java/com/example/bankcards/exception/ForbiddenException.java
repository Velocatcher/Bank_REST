package com.example.bankcards.exception;

//	Бросаем при попытке доступа к чужим ресурсам (карта не принадлежит пользователю).
public class ForbiddenException extends RuntimeException{
    public ForbiddenException(String message){
        super(message);
    }
}
