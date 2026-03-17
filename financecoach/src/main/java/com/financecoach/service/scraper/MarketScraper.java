package com.financecoach.service.scraper;

import com.financecoach.model.entity.MarketPrice;

import java.util.List;

/**
 * Tüm market scraper'larının implement etmesi gereken ortak arayüz.
 * Spring, bu interface'i implement eden tüm bean'leri otomatik olarak
 * MarketScraperScheduler'a inject eder.
 *
 * Yeni bir market eklemek için:
 * 1. Bu interface'i implement eden yeni bir @Service sınıfı yaz
 * 2. Spring otomatik olarak tanır, Scheduler'a eklenir
 */
public interface MarketScraper {

    /**
     * Market zincirinin adı.
     * Loglama ve DB kaydı için kullanılır.
     * Örnek: "BİM", "A101", "ŞOK"
     */
    String getMarketName();

    /**
     * Market sitesini scrape eder ve fiyat listesi döner.
     * Boş liste dönerse Scheduler seed data'yı korur.
     *
     * @return Scrape edilen ürün fiyatları
     */
    List<MarketPrice> scrape();
}