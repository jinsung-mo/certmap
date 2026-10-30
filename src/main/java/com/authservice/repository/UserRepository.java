package com.authservice.repository;

import com.authservice.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    // 소셜 로그인은 이메일이 아니라 제공자와 제공자 사용자 ID로 먼저 찾는다
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}
