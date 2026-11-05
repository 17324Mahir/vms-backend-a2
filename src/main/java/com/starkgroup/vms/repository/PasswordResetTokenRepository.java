package com.starkgroup.vms.repository;

import com.starkgroup.vms.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findFirstByEmailIgnoreCaseAndTokenCodeAndUsedFalseOrderByCreatedAtDesc(
            String email,
            String tokenCode
    );

    void deleteAllByEmailIgnoreCase(String email);
}

