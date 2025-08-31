package com.example.bankcards.exception;

//	Используем, когда входные данные неверны: формат номера, сумма ≤ 0, недостаточно средств и т.п.
public class BadRequestException extends RuntimeException{
    public BadRequestException(String message){
        super(message);
    }
}
