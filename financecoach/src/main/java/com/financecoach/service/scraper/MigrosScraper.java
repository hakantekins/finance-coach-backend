package com.financecoach.service.scraper;

import com.financecoach.model.entity.MarketPrice;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class MigrosScraper implements MarketScraper {

    private static final String BASE_URL = "https://www.migros.com.tr";
    private static final Pattern PRICE_PATTERN = Pattern.compile("([\\d.,]+)\\s*TL|₺\\s*([\\d.,]+)");

    private static final List<String> CATEGORY_URLS = List.of(
            "/sut-ve-sut-urunleri-c-2",
            "/kahvaltilik-c-3",
            "/temel-gida-c-4",
            "/meyve-sebze-c-5",
            "/et-balik-tavuk-c-6",
            "/dondurulmus-c-7",
            "/icecek-c-8",
            "/atistirmalik-c-9",
            "/ekmek-ve-pastane-urunleri-c-12",
            "/temizlik-c-10",
            "/kisisel-bakim-c-11",
            "/kagit-urunleri-c-14"
    );

    @Override
    public String getMarketName() {
        return "Migros";
    }

    @Override
    public List<MarketPrice> scrape() {
        List<MarketPrice> allProducts = new ArrayList<>();
        log.info("Migros scraping başlatılıyor (Selenium headless)...");

        WebDriver driver = null;
        try {
            driver = createHeadlessDriver();

            for (String path : CATEGORY_URLS) {
                try {
                    List<MarketPrice> products = scrapeCategory(driver, path);
                    List<MarketPrice> filtered = products.stream()
                            .filter(p -> FmcgFilter.isFmcg(p.getProductName(), p.getCategory()))
                            .toList();
                    allProducts.addAll(filtered);
                    log.info("  Migros {} → {} ürün (filtrelenmeden: {})", path, filtered.size(), products.size());
                    Thread.sleep(2500);
                } catch (Exception e) {
                    log.error("  Migros {} hatası: {}", path, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Migros Selenium başlatılamadı: {}", e.getMessage());
        } finally {
            if (driver != null) {
                try { driver.quit(); } catch (Exception ignored) {}
            }
        }

        log.info("Migros scraping tamamlandı: {} ürün", allProducts.size());
        return allProducts;
    }

    private List<MarketPrice> scrapeCategory(WebDriver driver, String path) {
        List<MarketPrice> products = new ArrayList<>();
        String url = BASE_URL + path;

        try {
            driver.get(url);

            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(d -> ((JavascriptExecutor) d)
                            .executeScript("return document.readyState").equals("complete"));

            Thread.sleep(3000);

            // Scroll — lazy load tetikle
            JavascriptExecutor js = (JavascriptExecutor) driver;
            for (int i = 0; i < 8; i++) {
                js.executeScript("window.scrollBy(0, 600)");
                Thread.sleep(700);
            }

            String pageSource = driver.getPageSource();
            Document doc = Jsoup.parse(pageSource);

            // Migros HTML yapısı — ürün kartları
            Elements cards = doc.select(
                    "[class*='product-card'], " +
                            "[class*='mdc-card'], " +
                            "[data-monitor-name], " +
                            "article[class*='product'], " +
                            "div[class*='product-item'], " +
                            "a[href*='/urun/']"
            );

            // Alternatif: fiyat içeren herhangi bir container
            if (cards.isEmpty()) {
                cards = doc.select("div:has(> span:containsOwn(TL)):has(img)");
            }

            log.debug("Migros {} — {} potansiyel kart", path, cards.size());

            for (Element card : cards) {
                try {
                    MarketPrice product = parseCard(card, path);
                    if (product != null) {
                        products.add(product);
                    }
                } catch (Exception e) {
                    log.trace("Migros parse hatası: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Migros kategori hatası [{}]: {}", url, e.getMessage());
        }

        return products;
    }

    private MarketPrice parseCard(Element card, String categoryPath) {
        String name = null;

        // 1. data-monitor-name
        name = card.attr("data-monitor-name");

        // 2. Çeşitli class selector'lar
        if (name == null || name.isBlank()) {
            Element nameEl = card.selectFirst(
                    "[class*='product-name'], [class*='productName'], " +
                            "[class*='product-title'], h5, h4, h3"
            );
            if (nameEl != null) name = nameEl.text().trim();
        }

        // 3. img alt
        if (name == null || name.isBlank()) {
            Element img = card.selectFirst("img[alt]");
            if (img != null && !img.attr("alt").isBlank()) {
                name = img.attr("alt").trim();
            }
        }

        // 4. title attribute
        if (name == null || name.isBlank()) {
            name = card.attr("title").trim();
        }

        if (name == null || name.isBlank() || name.length() < 3) return null;

        // Fiyat
        BigDecimal price = extractPrice(card);
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) return null;
        if (price.compareTo(new BigDecimal("0.5")) < 0 || price.compareTo(new BigDecimal("50000")) > 0) return null;

        String unit = extractUnit(name);
        String category = mapCategory(categoryPath);

        String productUrl = null;
        if (card.tagName().equals("a")) {
            productUrl = card.attr("abs:href");
        } else {
            Element link = card.selectFirst("a[href*='/urun/']");
            if (link != null) productUrl = link.attr("abs:href");
        }

        String imageUrl = null;
        Element img = card.selectFirst("img[src*='migros']");
        if (img != null) imageUrl = img.attr("abs:src");
        if (imageUrl == null) {
            img = card.selectFirst("img[data-src]");
            if (img != null) imageUrl = img.attr("data-src");
        }

        return MarketPrice.builder()
                .productName(name)
                .marketName("Migros")
                .price(price)
                .priceDate(LocalDate.now())
                .unit(unit)
                .externalId("MIG-" + name.hashCode())
                .productUrl(productUrl)
                .imageUrl(imageUrl)
                .category(category)
                .active(true)
                .scrapedAt(OffsetDateTime.now())
                .build();
    }

    private BigDecimal extractPrice(Element card) {
        String text = card.text();
        List<BigDecimal> prices = new ArrayList<>();

        Matcher matcher = PRICE_PATTERN.matcher(text);
        while (matcher.find()) {
            try {
                String priceStr = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                if (priceStr != null) {
                    priceStr = priceStr.replace(".", "").replace(",", ".");
                    prices.add(new BigDecimal(priceStr));
                }
            } catch (NumberFormatException ignored) {}
        }

        if (prices.isEmpty()) return null;
        return prices.get(prices.size() - 1);
    }

    private String mapCategory(String path) {
        if (path.contains("sut")) return "Süt Ürünleri";
        if (path.contains("kahvaltilik")) return "Kahvaltılık";
        if (path.contains("temel-gida")) return "Temel Gıda";
        if (path.contains("meyve-sebze")) return "Meyve & Sebze";
        if (path.contains("et-balik")) return "Et & Tavuk";
        if (path.contains("dondurulmus")) return "Dondurulmuş";
        if (path.contains("icecek")) return "İçecekler";
        if (path.contains("atistirmalik")) return "Atıştırmalıklar";
        if (path.contains("ekmek")) return "Ekmek";
        if (path.contains("temizlik")) return "Temizlik";
        if (path.contains("kisisel-bakim")) return "Kişisel Bakım";
        if (path.contains("kagit")) return "Kağıt Ürünleri";
        if (path.contains("bebek")) return "Anne & Bebek";
        return "Diğer";
    }

    private String extractUnit(String name) {
        Pattern unitPattern = Pattern.compile(
                "(\\d+[.,]?\\d*)\\s*(kg|gr|g|ml|lt|litre|l|adet|'(?:l[iıuü]|lu|lü))",
                Pattern.CASE_INSENSITIVE);
        Matcher m = unitPattern.matcher(name);
        return m.find() ? m.group(0).trim() : null;
    }

    private WebDriver createHeadlessDriver() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--window-size=1920,1080",
                "--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                "--lang=tr-TR",
                "--disable-blink-features=AutomationControlled",
                "--disable-extensions"
        );
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));

        return new ChromeDriver(options);
    }
}