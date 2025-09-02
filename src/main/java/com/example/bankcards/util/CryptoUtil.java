package com.example.bankcards.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.security.SecureRandom;

@Component
public class CryptoUtil {

    private static final String ALG = "AES";
    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LEN = 12;          // bytes for GCM (96 bits)

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public CryptoUtil(@Value("${app.crypto.aes-key-base64}") String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("app.crypto.aes-key-base64 is empty: provide Base64 of 16/24/32 bytes");
        }
        byte[] k;
        try {
            k = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.crypto.aes-key-base64 is not valid Base64", e);
        }
        int len = k.length;
        if (len != 16 && len != 24 && len != 32) {
            throw new IllegalStateException("Invalid AES key length: " + len + " bytes (need 16/24/32)");
        }
        this.key = new SecretKeySpec(k, ALG);
    }

    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_LEN];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] enc = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(enc);
        } catch (Exception e) {
            throw new IllegalStateException("Encrypt failed: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    public String decrypt(String token) {
        try {
            String[] parts = token.split(":", 2);
            if (parts.length != 2) throw new IllegalArgumentException("cipher token has no IV separator ':'");
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            if (iv.length != IV_LEN) throw new IllegalArgumentException("IV length is " + iv.length + " (need " + IV_LEN + ")");
            byte[] cipherBytes = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] dec = cipher.doFinal(cipherBytes);
            return new String(dec, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decrypt failed: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}





//package com.example.bankcards.util;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import javax.crypto.Cipher;
//import javax.crypto.spec.GCMParameterSpec;
//import javax.crypto.spec.SecretKeySpec;
//import javax.crypto.SecretKey;
//import java.security.SecureRandom;
//import java.util.Base64;
//
//@Component
//public class CryptoUtil {
//
//    private static final String ALG = "AES";
//    private static final String TRANSFORM = "AES/GCM/NoPadding";
//    private static final int GCM_TAG_LENGTH = 128; // почему: стандартный 128 битный тег аутентичности
//
//    private final SecretKey key;
//    private final SecureRandom random = new SecureRandom();
//
//    public CryptoUtil(@Value("${app.crypto.aes-key-base64}") String base64Key) {
//        byte[] k = Base64.getDecoder().decode(base64Key); // почему: удобная передача ключа через env/конфиг
//        this.key = new SecretKeySpec(k, ALG);
//    }
//
//    public String encrypt(String plain) {
//        try {
//            byte[] iv = new byte[12]; // почему: для GCM рекомендуют 96 битный IV
//            random.nextBytes(iv);
//            Cipher cipher = Cipher.getInstance(TRANSFORM);
//            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
//            byte[] enc = cipher.doFinal(plain.getBytes());
//            return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(enc);
//        } catch (Exception e) {
//            throw new IllegalStateException("Encrypt failed");
//        }
//    }
//
//    public String decrypt(String token) {
//        try {
//            String[] parts = token.split(":", 2);
//            byte[] iv = Base64.getDecoder().decode(parts[0]);
//            byte[] cipherBytes = Base64.getDecoder().decode(parts[1]);
//            Cipher cipher = Cipher.getInstance(TRANSFORM);
//            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
//            byte[] dec = cipher.doFinal(cipherBytes);
//            return new String(dec);
//        } catch (Exception e) {
//            throw new IllegalStateException("Decrypt failed");
//        }
//    }
//}
