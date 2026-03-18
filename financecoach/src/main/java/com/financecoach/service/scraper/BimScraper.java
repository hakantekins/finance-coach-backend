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

/**
 * BİM Market Scraper — Selenium (headless Chrome) ile.
 * BİM sitesi tamamen JS ile render edildiğinden Jsoup tek başına çalışmaz.
 * Selenium sayfayı render eder → HTML alınır → Jsoup ile parse edilir.
 */
@Service
@Slf4j
public class BimScraper implements MarketScraper {

    private static final String BASE_URL = "https://www.bim.com.tr";
    private static final Pattern PRICE_PATTERN = Pattern.compile("([\\d.,]+)\\s*TL|₺\\s*([\\d.,]+)");

    private static final List<String> CATEGORY_URLS = List.of(
            "/kategori/aktuel-urunler",
            "/kategori/sut-urunleri",
            "/kategori/temel-gida",
            "/kategori/icecekler",
            "/kategori/temizlik",
            "/kategori/kisisel-bakim",
            "/kategori/et-tavuk-balik",
            "/kategori/meyve-sebze",
            "/kategori/kahvaltilik",
            "/kategori/dondurulmus-gida"
    );

    @Override
    public String getMarketName() {
        return "BİM";
    }

    @Override
    public List<MarketPrice> scrape() {
        List<MarketPrice> allProducts = new ArrayList<>();
        log.info("BİM scraping başlatılıyor (Selenium headless)...");

        WebDriver driver = null;
        try {
            driver = createHeadlessDriver();

            for (String categoryPath : CATEGORY_URLS) {
                try {
                    List<MarketPrice> products = scrapeCategory(driver, categoryPath);
                    List<MarketPrice> filtered = products.stream()
                            .filter(p -> FmcgFilter.isFmcg(p.getProductName(), p.getCategory()))
                            .toList();
                    allProducts.addAll(filtered);
                    log.info("  BİM {} → {} ürün (filtrelenmeden: {})", categoryPath, filtered.size(), products.size());
                    Thread.sleep(2000); // Rate limiting
                } catch (Exception e) {
                    log.error("  BİM {} hatası: {}", categoryPath, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("BİM Selenium başlatılamadı: {}", e.getMessage());
        } finally {
            if (driver != null) {
                try { driver.quit(); } catch (Exception ignored) {}
            }
        }

        log.info("BİM scraping tamamlandı: {} ürün", allProducts.size());
        return allProducts;
    }

    private List<MarketPrice> scrapeCategory(WebDriver driver, String categoryPath) {
        List<MarketPrice> products = new ArrayList<>();
        String url = BASE_URL + categoryPath;

        try {
            driver.get(url);

            // Sayfanın yüklenmesini bekle
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(d -> ((JavascriptExecutor) d)
                            .executeScript("return document.readyState").equals("complete"));

            // React/Next.js hydration için ek bekleme
            Thread.sleep(3000);

            // Scroll down — lazy load ürünleri tetikle
            JavascriptExecutor js = (JavascriptExecutor) driver;
            for (int i = 0; i < 5; i++) {
                js.executeScript("window.scrollBy(0, 800)");
                Thread.sleep(800);
            }

            // Render edilmiş HTML'i al
            String pageSource = driver.getPageSource();
            Document doc = Jsoup.parse(pageSource);

            // BİM ürün kartlarını parse et
            // BİM'in HTML yapısı değişken — birden fazla selector dene
            Elements cards = doc.select("[class*='product'], [class*='Product'], [class*='card'], [class*='Card']");

            if (cards.isEmpty()) {
                // Alternatif: tüm linkleri tara
                cards = doc.select("a[href*='/urun/'], a[href*='/product/']");
            }

            if (cards.isEmpty()) {
                // Son çare: fiyat içeren div'leri bul
                // (Jsoup :has(...) her ortamda parse edilemeyebiliyor)
                // parseCard zaten fiyat/isim bulamazsa null döndüğü için anchor'lar üzerinden deniyoruz.
                cards = doc.select("a[href]");
            }

            log.debug("BİM {} — {} potansiyel kart bulundu", categoryPath, cards.size());

            for (Element card : cards) {
                try {
                    MarketPrice product = parseCard(card, categoryPath);
                    if (product != null) {
                        products.add(product);
                    }
                } catch (Exception e) {
                    log.trace("BİM ürün parse hatası: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("BİM kategori scrape hatası [{}]: {}", url, e.getMessage());
        }

        return products;
    }

    private MarketPrice parseCard(Element card, String categoryPath) {
        // Ürün adı — çeşitli selector'lar dene
        String name = extractText(card, "h2, h3, h4, [class*='name'], [class*='Name'], [class*='title'], [class*='Title']");

        if (name == null || name.isBlank() || name.length() < 3) {
            // Link text veya img alt'tan dene
            Element link = card.selectFirst("a[href]");
            if (link != null) name = link.attr("title");
            if (name == null || name.isBlank()) {
                Element img = card.selectFirst("img[alt]");
                if (img != null) name = img.attr("alt");
            }
        }

        if (name == null || name.isBlank() || name.length() < 3) return null;
        name = name.trim();

        // Fiyat
        BigDecimal price = extractPrice(card);
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) return null;
        // Mantıksız fiyatları filtrele (0.01 TL altı veya 100.000 TL üstü)
        if (price.compareTo(new BigDecimal("0.5")) < 0 || price.compareTo(new BigDecimal("100000")) > 0) return null;

        // Birim
        String unit = extractUnit(name);

        // Kategori
        String category = mapCategory(categoryPath);

        return MarketPrice.builder()
                .productName(name)
                .marketName("BİM")
                .price(price)
                .priceDate(LocalDate.now())
                .unit(unit)
                .externalId("BIM-" + name.hashCode())
                .category(category)
                .active(true)
                .scrapedAt(OffsetDateTime.now())
                .build();
    }

    private String extractText(Element parent, String selectors) {
        for (String selector : selectors.split(",")) {
            Element el = parent.selectFirst(selector.trim());
            if (el != null && !el.text().isBlank()) {
                return el.text().trim();
            }
        }
        return null;
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
        // Son fiyat = geçerli/indirimli fiyat
        return prices.get(prices.size() - 1);
    }

    private String mapCategory(String path) {
        if (path.contains("aktuel")) return "Aktüel";
        if (path.contains("sut")) return "Süt Ürünleri";
        if (path.contains("temel-gida")) return "Temel Gıda";
        if (path.contains("icecek")) return "İçecekler";
        if (path.contains("temizlik")) return "Temizlik";
        if (path.contains("kisisel")) return "Kişisel Bakım";
        if (path.contains("et-tavuk")) return "Et & Tavuk";
        if (path.contains("meyve")) return "Meyve & Sebze";
        if (path.contains("kahvalti")) return "Kahvaltılık";
        if (path.contains("dondurulmus")) return "Dondurulmuş";
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
        // WebDriverManager Chrome driver'ı otomatik indirir
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
                "--disable-extensions",
                "--disable-infobars"
        );

        // Bot algılamayı azalt
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));

        return new ChromeDriver(options);
    }
}