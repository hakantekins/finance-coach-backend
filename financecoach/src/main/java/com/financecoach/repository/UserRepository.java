package com.financecoach.repository;

import com.financecoach.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Kullanıcı veritabanı işlemleri için repository.
 *
 * DÜZELTME: User.id tipi Long olduğundan JpaRepository<User, Long> kullanılır.
 * Önceki versiyonda UUID kullanılıyordu — bu runtime hataya yol açıyordu.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}