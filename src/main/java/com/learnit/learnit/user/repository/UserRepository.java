package com.learnit.learnit.user.repository;

import com.learnit.learnit.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(String email);
    
    /**
     * OAuth provider와 providerId로 사용자 조회
     */
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    
    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);
}

