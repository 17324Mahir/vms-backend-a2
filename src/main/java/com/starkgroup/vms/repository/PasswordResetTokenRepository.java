package com.starkgroup.vms.repository;

import com.starkgroup.vms.domain.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken> findFirstByEmailIgnoreCaseAndTokenCodeAndUsedFalseOrderByCreatedAtDesc(
            String email,
            String tokenCode
    );

    void deleteAllByEmailIgnoreCase(String email);
}

