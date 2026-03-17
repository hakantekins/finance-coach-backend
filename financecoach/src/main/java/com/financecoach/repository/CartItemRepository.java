package com.financecoach.repository;

import com.financecoach.model.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /** Kullanıcının aktif (tamamlanmamış) sepet öğeleri */
    List<CartItem> findByUserIdAndCompletedFalseOrderByCreatedAtDesc(Long userId);

    /** Kullanıcının tamamlanmış (geçmiş) alışveriş öğeleri */
    List<CartItem> findByUserIdAndCompletedTrueOrderByCompletedAtDesc(Long userId);

    /** Kullanıcının aktif sepetinde bu ürün var mı? */
    Optional<CartItem> findByUserIdAndProductNameAndCompletedFalse(Long userId, String productName);

    /** Güvenlik: bu öğe bu kullanıcıya mı ait? */
    Optional<CartItem> findByIdAndUserId(Long id, Long userId);

    /** Kullanıcının aktif sepetindeki toplam ürün sayısı */
    long countByUserIdAndCompletedFalse(Long userId);

    /** Kullanıcının aktif sepetini toplu sil */
    void deleteByUserIdAndCompletedFalse(Long userId);
}