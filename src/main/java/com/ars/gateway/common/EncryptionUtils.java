package com.ars.gateway.common;

import com.ars.gateway.dto.CheckValidDeviceIdResponseDTO;
import com.dct.model.constants.BaseExceptionConstants;
import com.dct.model.exception.BaseIllegalArgumentException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@SuppressWarnings("unused")
public class EncryptionUtils {
    private final String deviceKey;
    private static final int KEY_SIZE = 32;
    private static final int IV_LENGTH_BYTE = 16;
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String ENTITY_NAME = "sds.easypos.gateway.common.EncryptionUtils";
    private static final Logger log = LoggerFactory.getLogger(EncryptionUtils.class);

    public EncryptionUtils(@Value("${app.device-key}") String deviceKey) {
        deviceKey = StringUtils.trimAllWhitespace(deviceKey);

        if (StringUtils.hasText(deviceKey) && deviceKey.length() >= KEY_SIZE) {
            this.deviceKey = deviceKey.trim().substring(0, KEY_SIZE);
        } else {
            throw new BaseIllegalArgumentException(ENTITY_NAME, BaseExceptionConstants.UNCERTAIN_ERROR);
        }
    }

    public CheckValidDeviceIdResponseDTO checkValidDeviceId(final String deviceId) {
        try {
            String deviceIdDecoded = decrypt(deviceId);
            return new CheckValidDeviceIdResponseDTO(deviceIdDecoded, true);
        } catch (Exception e) {
            log.error("[COULD_NOT_DECRYPT_DEVICE_ID] - error: ", e);
            return new CheckValidDeviceIdResponseDTO(deviceId, false);
        }
    }

    public String encrypt(String plainText) {
        try {
            // Generate random IV
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            SecretKeySpec secretKeySpec = new SecretKeySpec(deviceKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            // Combine the IV and ciphertext, then Base64 encode
            String encodedIv = Base64.getEncoder().encodeToString(iv);
            String encodedCipherText = Base64.getEncoder().encodeToString(cipherText);
            return encodedIv + ":" + encodedCipherText;
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while encrypting data", e);
        }
    }

    public String decrypt(String encryptedData) {
        try {
            // Extract IV and ciphertext from input string
            String[] parts = encryptedData.split(":");

            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid encrypted data format");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getDecoder().decode(parts[1]);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            SecretKeySpec secretKeySpec = new SecretKeySpec(deviceKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] decryptedText = cipher.doFinal(cipherText);
            return new String(decryptedText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while decrypting data", e);
        }
    }
}
