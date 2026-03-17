package com.financecoach.repository;

import com.financecoach.model.entity.MarketPrice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {

    List<MarketPrice> findByProductNameContainingIgnoreCaseOrderByPriceDateDesc(
            String productName
    );

    List<MarketPrice> findByMarketNameIgnoreCaseOrderByPriceDateDesc(
            String marketName
    );

    List<MarketPrice> findByProductNameContainingIgnoreCaseAndMarketNameIgnoreCaseOrderByPriceDateDesc(
            String productName,
            String marketName
    );

    @Query("""
        SELECT mp FROM MarketPrice mp
        WHERE mp.productName = :productName
          AND mp.priceDate = (
              SELECT MAX(mp2.priceDate)
              FROM MarketPrice mp2
              WHERE mp2.productName = :productName
                AND mp2.marketName  = mp.marketName
          )
        ORDER BY mp.price ASC
    """)
    List<MarketPrice> findLatestPricesByProductName(
            @Param("productName") String productName
    );

    @Query("""
        SELECT mp FROM MarketPrice mp
        WHERE mp.productName LIKE %:productName%
          AND mp.priceDate BETWEEN :startDate AND :endDate
        ORDER BY mp.priceDate DESC
    """)
    List<MarketPrice> findByProductNameAndDateRange(
            @Param("productName") String productName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT mp FROM MarketPrice mp
        WHERE mp.active = true
        ORDER BY mp.category ASC NULLS LAST,
                 mp.productName ASC,
                 mp.price ASC
    """)
    List<MarketPrice> findAllGroupedByCategoryAndProduct();

    List<MarketPrice> findByCategoryIgnoreCaseAndActiveTrueOrderByProductNameAscPriceAsc(String category);

    @Query("SELECT DISTINCT mp.category FROM MarketPrice mp WHERE mp.category IS NOT NULL ORDER BY mp.category")
    List<String> findDistinctCategories();

    // ─── YENİ: Sayfalama destekli sorgular ────────────────────────────────

    /** Tüm aktif ürünleri sayfalı getir */
    Page<MarketPrice> findByActiveTrue(Pageable pageable);

    /** Kategori bazlı sayfalı getir */
    Page<MarketPrice> findByCategoryIgnoreCaseAndActiveTrue(String category, Pageable pageable);
}