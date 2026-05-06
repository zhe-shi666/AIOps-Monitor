package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TemporaryPasswordService {
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGIT = "23456789";
    private static final String SPECIAL = "@#$%*!?";
    private static final String ALL = UPPER + LOWER + DIGIT + SPECIAL;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String issueTemporaryPassword(User user) {
        String temporaryPassword = generatePassword(14);
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setPasswordChangeRequired(true);
        user.setTemporaryPasswordIssuedAt(LocalDateTime.now());
        userRepository.save(user);
        return temporaryPassword;
    }

    private String generatePassword(int length) {
        StringBuilder builder = new StringBuilder(length);
        builder.append(randomChar(UPPER));
        builder.append(randomChar(LOWER));
        builder.append(randomChar(DIGIT));
        builder.append(randomChar(SPECIAL));
        while (builder.length() < length) {
            builder.append(randomChar(ALL));
        }
        return shuffle(builder.toString());
    }

    private char randomChar(String source) {
        return source.charAt(secureRandom.nextInt(source.length()));
    }

    private String shuffle(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }
}
