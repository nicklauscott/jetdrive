package com.niclauscott.jetdrive.file_feature.file.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class FileEncryptionService {
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12;
    private static final int TAG_BIT_LENGTH = 128;

    private final byte[] masterKey;

    public FileEncryptionService(@Value("${enc.base64Key}") String base64MasterKey) {
        this.masterKey = Base64.getDecoder().decode(base64MasterKey);
    }

    public byte[] encrypt(byte[] plaintext, String userId, String fileId) throws Exception {
        byte[] deriveKey = deriveKey(userId, fileId);
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AES_GCM);
        SecretKey key = new SecretKeySpec(deriveKey, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_BIT_LENGTH, iv);

        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
        byte[] cipherText = cipher.doFinal(plaintext);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(iv);
        out.write(cipherText);
        return out.toByteArray();
    }

    public byte[] decrypt(byte[] encrypted, String userId, String fileId) throws Exception {
        byte[] iv = Arrays.copyOfRange(encrypted, 0, IV_SIZE);
        byte[] cipherText = Arrays.copyOfRange(encrypted, IV_SIZE, encrypted.length);
        byte[] deriveKey = deriveKey(userId, fileId);

        Cipher cipher = Cipher.getInstance(AES_GCM);
        SecretKey key = new SecretKeySpec(deriveKey, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_BIT_LENGTH, iv);

        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
        return cipher.doFinal(cipherText);
    }

    private byte[] deriveKey(String userId, String fileId) throws Exception {
        byte[] salt = (userId + ":" + fileId).getBytes(StandardCharsets.UTF_8);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(
                Base64.getEncoder().encodeToString(masterKey).toCharArray(),
                salt, 10_000, 256
        );
        SecretKey secretKey = factory.generateSecret(spec);
        return secretKey.getEncoded();
    }
}
