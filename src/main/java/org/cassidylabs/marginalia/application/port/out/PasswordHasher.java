package org.cassidylabs.marginalia.application.port.out;

/** 비밀번호 해싱 포트. 구현체: infrastructure/security/BcryptPasswordHasher */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hashedPassword);
}
