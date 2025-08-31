package com.example.bankcards.exception;

// Карта/пользователь не найдены
public class NotFoundException extends RuntimeException{
    public NotFoundException(String message){
        super(message);
    }
}
