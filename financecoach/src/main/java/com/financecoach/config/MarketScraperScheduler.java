package com.financecoach.config;

import com.financecoach.model.entity.MarketPrice;
import com.financecoach.repository.MarketPriceRepository;
import com.financecoach.service.scraper.MarketScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketScraperScheduler {

    private final List<MarketScraper> scrapers;
    private final MarketPriceRepository marketPriceRepository;

    @Scheduled(cron = "${scraper.cron:0 0 3 * * MON}")
    @Transactional
    public void runWeeklyScraping() {
        log.info("═══ Haftalık market fiyat güncelleme başladı ═══");
        long startTime = System.currentTimeMillis();

        int totalSaved = 0;
        int totalFailed = 0;

        for (MarketScraper scraper : scrapers) {
            String marketName = scraper.getMarketName();
            try {
                log.info("Scraping başlıyor: {}", marketName);

                // 1. ÖNCE scrape et
                List<MarketPrice> scraped = scraper.scrape();

                // 2. Boş dönerse eski veriyi koru
                if (scraped.isEmpty()) {
                    log.warn("{}: scraping sonuç vermedi, eski veriler korunuyor", marketName);
                    totalFailed++;
                    continue;
                }

                // 3. Başarılıysa eski veriyi sil
                deleteExistingPricesForToday(marketName);

                // 4. Yeni veriyi kaydet
                marketPriceRepository.saveAll(scraped);
                totalSaved += scraped.size();

                log.info("{}: {} ürün fiyatı güncellendi", marketName, scraped.size());

            } catch (Exception e) {
                // 5. Hata durumunda eski veri korunuyor
                log.error("{} scraping başarısız, eski veriler korunuyor: {}", marketName, e.getMessage(), e);
                totalFailed++;
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("═══ Haftalık güncelleme tamamlandı: {} kayıt, {} başarısız, {}ms ═══",
                totalSaved, totalFailed, elapsed);
    }

    private void deleteExistingPricesForToday(String marketName) {
        List<MarketPrice> existing = marketPriceRepository
                .findByMarketNameIgnoreCaseOrderByPriceDateDesc(marketName)
                .stream()
                .filter(mp -> mp.getPriceDate().equals(LocalDate.now()))
                .toList();

        if (!existing.isEmpty()) {
            marketPriceRepository.deleteAll(existing);
            log.debug("{}: {} eski kayıt silindi", marketName, existing.size());
        }
    }
}