package com.example.bankcards.util;

public class MaskingUtil {
    public static String mask(String number) {
        if (number == null || number.length() < 4) {return "****";} //защита от некорректного ввода
        String last4 = number.substring(number.length() - 4);
        return "**** **** ****" + last4;
    }
}
