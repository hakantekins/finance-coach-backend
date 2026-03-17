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
public class A101Scraper implements MarketScraper {

    private static final String BASE_URL = "https://www.a101.com.tr";
    private static final Pattern PRICE_PATTERN = Pattern.compile("₺([\\d.]+,\\d{2})");
    private static final Pattern ID_PATTERN = Pattern.compile("_p-(\\d+)");

    private static final List<String> CATEGORY_PATHS = List.of(
            "/kapida/sut-ve-sut-urunleri-c-1013",
            "/kapida/kahvaltilik-c-1014",
            "/kapida/temel-gida-c-1015",
            "/kapida/et-tavuk-balik-c-1016",
            "/kapida/meyve-sebze-c-1017",
            "/kapida/dondurulmus-urunler-c-1019",
            "/kapida/icecekler-c-1020",
            "/kapida/atistirmalik-c-1021",
            "/kapida/temizlik-urunleri-c-1024",
            "/kapida/kisisel-bakim-c-1025",
            "/kapida/kagit-urunleri-c-1026"
    );

    @Override
    public String getMarketName() {
        return "A101";
    }

    @Override
    public List<MarketPrice> scrape() {
        List<MarketPrice> allProducts = new ArrayList<>();
        log.info("A101 scraping başlatılıyor (Selenium headless)...");

        WebDriver driver = null;
        try {
            driver = createHeadlessDriver();

            for (String path : CATEGORY_PATHS) {
                try {
                    List<MarketPrice> products = scrapeCategory(driver, path);
                    List<MarketPrice> filtered = products.stream()
                            .filter(p -> FmcgFilter.isFmcg(p.getProductName(), p.getCategory()))
                            .toList();
                    allProducts.addAll(filtered);
                    log.info("  A101 {} → {} ürün (filtrelenmeden: {})", path, filtered.size(), products.size());
                    Thread.sleep(2000);
                } catch (Exception e) {
                    log.error("  A101 {} hatası: {}", path, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("A101 Selenium başlatılamadı: {}", e.getMessage());
        } finally {
            if (driver != null) {
                try { driver.quit(); } catch (Exception ignored) {}
            }
        }

        log.info("A101 scraping tamamlandı: {} ürün", allProducts.size());
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

            // A101 ürün kartları
            Elements cards = doc.select("a[href*='_p-']");

            if (cards.isEmpty()) {
                // Alternatif selector'lar
                cards = doc.select("[class*='product-card'], [class*='ProductCard']");
            }

            if (cards.isEmpty()) {
                cards = doc.select("div:has(h3):has(span:containsOwn(₺))");
            }

            log.debug("A101 {} — {} potansiyel kart", path, cards.size());

            for (Element card : cards) {
                try {
                    MarketPrice product = parseCard(card, path);
                    if (product != null) {
                        products.add(product);
                    }
                } catch (Exception e) {
                    log.trace("A101 parse hatası: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("A101 kategori hatası [{}]: {}", url, e.getMessage());
        }

        return products;
    }

    private MarketPrice parseCard(Element card, String categoryPath) {
        // Ürün adı
        String name = null;
        Element h3 = card.selectFirst("h3");
        if (h3 != null) name = h3.text().trim();
        if (name == null || name.isBlank()) name = card.attr("title").trim();
        if (name == null || name.isBlank()) {
            Element img = card.selectFirst("img[alt]");
            if (img != null) name = img.attr("alt").trim();
        }
        if (name == null || name.isBlank() || name.length() < 3) return null;

        // Fiyat
        List<BigDecimal> prices = new ArrayList<>();
        Matcher matcher = PRICE_PATTERN.matcher(card.text());
        while (matcher.find()) {
            try {
                String priceStr = matcher.group(1).replace(".", "").replace(",", ".");
                prices.add(new BigDecimal(priceStr));
            } catch (NumberFormatException ignored) {}
        }
        if (prices.isEmpty()) return null;
        BigDecimal price = prices.get(prices.size() - 1);
        if (price.compareTo(BigDecimal.ZERO) <= 0) return null;

        // External ID
        String href = card.attr("href");
        String externalId = "A101-" + href.hashCode();
        Matcher idMatcher = ID_PATTERN.matcher(href);
        if (idMatcher.find()) externalId = "A101-" + idMatcher.group(1);

        String productUrl = href.startsWith("http") ? href : BASE_URL + href;

        String imageUrl = null;
        Element img = card.selectFirst("img[src*='cdn2.a101.com.tr']");
        if (img != null) imageUrl = img.attr("abs:src");

        String category = mapCategory(categoryPath);
        String unit = extractUnit(name);

        return MarketPrice.builder()
                .productName(name)
                .marketName("A101")
                .price(price)
                .priceDate(LocalDate.now())
                .unit(unit)
                .externalId(externalId)
                .productUrl(productUrl)
                .imageUrl(imageUrl)
                .category(category)
                .active(true)
                .scrapedAt(OffsetDateTime.now())
                .build();
    }

    private String mapCategory(String path) {
        if (path.contains("sut")) return "Süt Ürünleri";
        if (path.contains("kahvaltilik")) return "Kahvaltılık";
        if (path.contains("temel-gida")) return "Temel Gıda";
        if (path.contains("et-tavuk")) return "Et & Tavuk";
        if (path.contains("meyve-sebze")) return "Meyve & Sebze";
        if (path.contains("icecek")) return "İçecekler";
        if (path.contains("dondurulmus")) return "Dondurulmuş";
        if (path.contains("atistirmalik")) return "Atıştırmalıklar";
        if (path.contains("temizlik")) return "Temizlik";
        if (path.contains("kisisel-bakim")) return "Kişisel Bakım";
        if (path.contains("kagit")) return "Kağıt Ürünleri";
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