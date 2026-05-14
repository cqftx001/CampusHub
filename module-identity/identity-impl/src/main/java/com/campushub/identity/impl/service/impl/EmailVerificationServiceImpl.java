package com.campushub.identity.impl.service.impl;

import com.campushub.identity.impl.config.EmailVerificationProperties;
import com.campushub.identity.impl.domain.enums.IdentityErrorCode;
import com.campushub.identity.impl.service.EmailVerificationService;
import com.campushub.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Objects;


@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final EmailVerificationProperties properties;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    static final int EMAIL_CODE_LENGTH = 6;

    @Lazy
    @Autowired
    private EmailVerificationServiceImpl self;

    private static final String EMAIL_LIMIT_PREFIX = "email:%s:limit:";
    private static final String EMAIL_ATTEMPTS_PREFIX = "email:%s:attempts:";
    private static final String EMAIL_CODE_PREFIX = "email:%s:code:";

    @Override
    public void sendRegisterCode(String email) {
        sendCode(email, EmailCodePurpose.REGISTER);
    }

    @Override
    public void verifyRegisterCode(String email, String code) {
        verifyCode(email, code, EmailCodePurpose.REGISTER);
    }

    @Override
    public void sendLoginCode(String email) {
        sendCode(email, EmailCodePurpose.LOGIN);
    }

    @Override
    public void verifyLoginCode(String email, String code) {
        verifyCode(email, code, EmailCodePurpose.LOGIN);
    }

    private void sendCode(String rawEmail, EmailCodePurpose purpose) {
        String email = normalizeEmail(rawEmail);
        String limitKey = limitKey(purpose, email);
        Boolean firstSend = redisTemplate.opsForValue()
                .setIfAbsent(limitKey, "1", properties.resendInterval());

        if(Boolean.FALSE.equals(firstSend)){
            throw new BadRequestException(IdentityErrorCode.EMAIL_CODE_RESEND_INTERVAL);
        }

        String code = generateCode(EMAIL_CODE_LENGTH);
        String codeKey = codeKey(purpose, email);
        String attemptsKey = attemptsKey(purpose, email);

        redisTemplate.opsForValue().set(codeKey, hash(purpose, email, code), properties.codeTtl());
        redisTemplate.delete(attemptsKey);

        self.sendEmailAsync(email, code, purpose);
    }

    private void verifyCode(String rawEmail, String code, EmailCodePurpose purpose) {
        String email = normalizeEmail(rawEmail);
        String codeKey = codeKey(purpose, email);
        String attemptsKey = attemptsKey(purpose, email);
        String limitKey = limitKey(purpose, email);

        String storedCodeHash = redisTemplate.opsForValue().get(codeKey);
        if(storedCodeHash == null){
            throw new BadRequestException(IdentityErrorCode.EMAIL_CODE_EXPIRED);
        }

        if(!storedCodeHash.equals(hash(purpose, email, code))){
            Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
            // Only set TTL when the attempts key is first created.
            // Avoid refreshing the attempts window on every wrong verification code.
            if (Objects.equals(attempts, 1L)) {
                redisTemplate.expire(attemptsKey, properties.codeTtl());
            }


            if(attempts != null && attempts >= properties.maxAttempts()){
                redisTemplate.delete(attemptsKey);
                redisTemplate.delete(codeKey);
                throw new BadRequestException(IdentityErrorCode.EMAIL_CODE_MAX_ATTEMPTS);
            }
            throw new BadRequestException(IdentityErrorCode.WRONG_VERIFICATION_CODE);
        }

        redisTemplate.delete(codeKey);
        redisTemplate.delete(attemptsKey);
        redisTemplate.delete(limitKey);

    }
    // -- helper --
    private String hash(EmailCodePurpose purpose, String email, String code){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            String raw = purpose.key + ":" + email + ":" + code + ":" + properties.codePepper();
            byte[] hashBytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private String generateCode(int length){
        if (length <= 0) {
            throw new IllegalArgumentException("Code length must be greater than 0");
        }

        StringBuilder code = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int digit = SECURE_RANDOM.nextInt(10);
            code.append(digit);
        }

        return code.toString();
    }

    @Async("mailExecutor")
    public void sendEmailAsync(String email, String code, EmailCodePurpose purpose) {
        try{
            SimpleMailMessage message = new SimpleMailMessage();

            message.setTo(email);
            message.setSubject(purpose.subject);
            message.setText(buildEmailText(code, purpose));

            mailSender.send(message);
        } catch(MailException ex){
            // 异步线程里的异常不会冒泡到调用方，必须自己 catch + 记日志
            log.error("Failed to send verification email. email={}, purpose={}",
                    email, purpose.key, ex);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String codeKey(EmailCodePurpose purpose, String email) {
        return EMAIL_CODE_PREFIX.formatted(purpose.key) + email;
    }

    private String attemptsKey(EmailCodePurpose purpose, String email) {
        return EMAIL_ATTEMPTS_PREFIX.formatted(purpose.key) + email;
    }

    private String limitKey(EmailCodePurpose purpose, String email) {
        return EMAIL_LIMIT_PREFIX.formatted(purpose.key) + email;
    }

    private String buildEmailText(String code, EmailCodePurpose purpose) {
        return """
                Welcome to CampusHub!

                %s

                %s

                This code will expire in %d minutes.

                %s

                Thanks,
                CampusHub
                """.formatted(
                purpose.bodyLine,
                code,
                properties.codeTtl().toMinutes(),
                purpose.footer
        );
    }

    enum EmailCodePurpose {
        REGISTER(
                "register",
                "Verify your CampusHub email",
                "Use the verification code below to complete your registration:",
                """
                If you did not create a CampusHub account, please ignore this email.
                Your account will not be activated unless this code is verified.
                """
        ),
        LOGIN(
                "login",
                "Your CampusHub login code",
                "Use the verification code below to sign in to CampusHub:",
                "If you did not request this login code, please ignore this email."
        );

        private final String key;
        private final String subject;
        private final String bodyLine;
        private final String footer;

        EmailCodePurpose(String key, String subject, String bodyLine, String footer) {
            this.key = key;
            this.subject = subject;
            this.bodyLine = bodyLine;
            this.footer = footer;
        }
    }
}
