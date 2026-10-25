package com.findit.repository;

import com.findit.model.User;
import com.findit.model.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmailIgnoreCaseOrPhone(String email, String phone);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    long countByRole(Role role);

    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
