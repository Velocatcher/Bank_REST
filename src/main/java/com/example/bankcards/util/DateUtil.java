package com.example.bankcards.util;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM/yy");

    public static Boolean isExpired(String mmYY){
        YearMonth ym = YearMonth.parse(mmYY, FMT);
        return ym.isBefore(YearMonth.now());
    }
}
