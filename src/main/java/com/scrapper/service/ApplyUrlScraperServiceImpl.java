package com.scrapper.service;

import com.scrapper.model.Job;
import com.scrapper.util.ScrapingSelectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// ✅ ДОДАНО: Нові сервіси для збереження додаткової інформації
import com.scrapper.service.criteriaServices.TagIngestService;
import com.scrapper.service.criteriaServices.LocationIngestService;
import com.scrapper.service.criteriaServices.PostedDateIngestService;
import com.scrapper.service.criteriaServices.LogoIngestService;
import com.scrapper.service.criteriaServices.TitleIngestService;
import com.scrapper.service.criteriaServices.DescriptionIngestService;

/**
 * ✅ ОНОВЛЕНИЙ СЕРВІС: Змінена логіка фільтрації з гібридним завантаженням
 * 
 * НОВА ГІБРИДНА ЛОГІКА ФІЛЬТРАЦІЇ:
 * 1. Спочатку вибирається job Function (фільтрація за функціями)
 * 2. Потім натискається кнопка Load More ОДИН раз (якщо вона є)
 * 3. Далі запускається цикл нескінченної прокрутки з автоматичним завантаженням
 * 4. Виходимо з циклу тільки коли кількість вакансій перестає зростати
 * 5. І тільки потім зчитування URL та перевірка префіксу
 * 6. Якщо URL містить https://jobs.techstars.com/companies/ то вакансія зберігається
 * 8. Теги збираються для всіх збережених вакансій
 * 
 * ГІБРИДНИЙ ПІДХІД ЗАВАНТАЖЕННЯ:
 * - Спочатку кнопка Load More (якщо є)
 * - Потім автоматичне завантаження при прокрутці
 * - Адаптивне завершення коли контент більше не завантажується
 * 
 * Це дозволяє зберігати всі вакансії компаній Techstars, незалежно від тегів
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplyUrlScraperServiceImpl implements ApplyUrlScraperService {

    @Value("${scraping.base-url:https://jobs.techstars.com/jobs}")
    private String baseUrl;

    @Value("${scraping.selenium.timeout:30}")
    private long timeoutSeconds;

    @Value("${scraping.selenium.scroll.delay:10000}")
    private long scrollDelay;

    @Value("${scraping.selenium.scroll.max-attempts:20}")
    private int maxScrollAttempts;

    @Value("${scraping.selenium.scroll.max-no-new-jobs:3}")
    private int maxNoNewJobsAttempts;

    /**
     * ✅ КЛЮЧОВА КОНСТАНТА: Префікс URL компаній Techstars
     * 
     * Якщо URL вакансії містить цей префікс, то вакансія зберігається 
     * БЕЗ перевірки тегів (тільки фільтрація за функціями).
     * 
     * Теги збираються для всіх збережених вакансій.
     * 
     * Це дозволяє зберігати всі вакансії компаній Techstars, 
     * незалежно від тегів, після застосування нової гібридної логіки
     * завантаження (Load More + нескінченна прокрутка).
     */
    private static final String REQUIRED_PREFIX = "https://jobs.techstars.com/companies/";
    private static final String LOAD_MORE_SELECTOR = ScrapingSelectors.LOAD_MORE_BUTTON[0];
    private static final String JOB_CARD_SELECTOR = ScrapingSelectors.JOB_CARD[0];

    // ✅ ДОДАНО: Нові сервіси для збереження додаткової інформації
    private final TagIngestService tagIngestService;
    private final LocationIngestService locationIngestService;
    private final PostedDateIngestService postedDateIngestService;
    private final LogoIngestService logoIngestService;
    private final TitleIngestService titleIngestService;
    private final DescriptionIngestService descriptionIngestService;
    private final JobCreationService jobCreationService;

    private WebDriver initializeWebDriver() {
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        
        // ✅ ДОДАНО: Обхід блокування ботів
        // options.addArguments("--headless"); // Тимчасово вимкнемо headless режим
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        
        // ✅ ДОДАНО: Обхід детекції автоматизації
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-plugins");
        options.addArguments("--disable-images");
        // options.addArguments("--disable-javascript"); // Вимкнемо JS тільки якщо потрібно
        
        // ✅ ДОДАНО: User-Agent для обходу блокування
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        
        // ✅ ДОДАНО: Додаткові налаштування
        options.addArguments("--remote-debugging-port=9222");
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");
        
        // ✅ ДОДАНО: Додаткові налаштування для обходу блокування
        options.addArguments("--disable-blink-features");
        options.addArguments("--disable-features=VizDisplayCompositor");
        options.addArguments("--disable-ipc-flooding-protection");
        
        log.info("🔧 Initializing Chrome WebDriver with anti-bot protection bypass (visible mode)");
        return new ChromeDriver(options);
    }

    @Override
    public List<String> fetchApplyUrls(List<String> jobFunctions, List<String> requiredTags) {
        Objects.requireNonNull(jobFunctions, "jobFunctions cannot be null");
        
        log.info("🚀 Starting Selenium scraping with NEW LOGIC: jobFunctions={}, tags={}", 
                jobFunctions, 
                requiredTags);

        WebDriver driver = null;
        try {
            driver = initializeWebDriver();
            
            // ✅ ВИПРАВЛЕНО: Завжди переходимо на основний, нефільтрований URL
            log.info("📍 Navigating to base URL: {}", baseUrl);
            driver.get(baseUrl);
            
            // ✅ ОПТИМІЗОВАНО: Швидке завантаження сторінки
            log.info("⏳ Quick page load...");
            sleep(3000); // Зменшуємо до 3 секунд
            
            // ✅ ОПТИМІЗОВАНО: Базова перевірка сторінки
            String pageTitle = driver.getTitle();
            String currentUrl = driver.getCurrentUrl();
            log.info("📄 Page loaded - Title: '{}', URL: '{}'", pageTitle, currentUrl);
            
            // ✅ ОПТИМІЗОВАНО: Швидка перевірка елементів
            int initialElements = driver.findElements(By.cssSelector("*")).size();
            log.info("🔍 Total elements on page: {}", initialElements);
            
            if (initialElements < 50) {
                log.warn("⚠️ Page seems to be empty! Only {} elements found", initialElements);
                // ✅ ОПТИМІЗОВАНО: Коротка затримка
                sleep(2000);
            }
            
            // ✅ ОНОВЛЕНО: Використовуємо нову гібридну логіку з правильним порядком
            log.info("🔍 Applying NEW HYBRID LOGIC: 1) job functions → 2) Load More (ОДИН раз) → 3) нескінченна прокрутка → 4) URL → 5) префікс компанії → 6) збір тегів");
            List<Job> jobs = scrapeAllJobsWithImprovedLogic(driver, requiredTags, jobFunctions);
            
            log.info("✅ Scraping completed with NEW LOGIC. Found {} jobs matching criteria.", jobs.size());
            
            return jobs.stream()
                    .map(Job::getJobPageUrl)
                    .filter(url -> url != null && url.startsWith(REQUIRED_PREFIX))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("❌ Error during Selenium scraping", e);
            throw new RuntimeException("Failed to scrape jobs with Selenium", e);
        } finally {
            if (driver != null) {
                try { 
                    driver.quit(); 
                    log.info("🔒 WebDriver closed successfully"); 
                } catch (Exception e) { 
                    log.warn("⚠️ Error closing WebDriver", e); 
                }
            }
        }
    }
    
    @Override
    public List<Job> scrapeAndCreateJobs(List<String> jobFunctions, List<String> requiredTags) {
        log.info("🚀 Starting job scraping and creation with NEW LOGIC for job functions: {} and tags: {}", 
                jobFunctions, 
                requiredTags);
        
        WebDriver driver = null;
        try {
            driver = initializeWebDriver();
            log.info("🌐 WebDriver initialized successfully");
            
            driver.get(baseUrl);
            log.info("🌐 Navigated to: {}", baseUrl);
            
            // ✅ ОПТИМІЗОВАНО: Швидке очікування завантаження сторінки
            log.info("🔍 Quick page load check...");
            
            // Чекаємо тільки 5 секунд на завантаження
            sleep(5000);
            
            // ✅ ОПТИМІЗОВАНО: Швидкий пошук карток вакансій
            log.info("🔍 Quick job cards search...");
            boolean pageLoaded = false;
            
            // Спробуємо тільки основні селектори з коротким таймаутом
            for (String selector : ScrapingSelectors.JOB_CARD) {
                try {
                    // Зменшуємо таймаут до 3 секунд
                    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(selector)));
                    
                    int elementCount = driver.findElements(By.cssSelector(selector)).size();
                    if (elementCount > 0) {
                        log.info("✅ Found {} job cards with selector: '{}'", elementCount, selector);
                        pageLoaded = true;
                        break;
                    }
                } catch (Exception e) {
                    log.debug("⚠️ Selector '{}' not found: {}", selector, e.getMessage());
                }
            }
            
            // ✅ ОПТИМІЗОВАНО: Якщо не знайшли, продовжуємо без додаткових перевірок
            if (!pageLoaded) {
                log.warn("⚠️ No job cards found with primary selectors, continuing anyway...");
                pageLoaded = true; // Продовжуємо роботу
            }
            
            // ✅ ОНОВЛЕНО: Використовуємо нову гібридну логіку для різних типів сторінок
            log.info("🔍 Applying NEW HYBRID LOGIC: 1) job functions → 2) Load More (ОДИН раз) → 3) нескінченна прокрутка → 4) URL → 5) префікс компанії → 6) збір тегів");
            List<Job> jobs = scrapeJobsBasedOnPageType(driver, requiredTags, jobFunctions);
            
            log.info("🎯 Job scraping completed with NEW LOGIC. Created {} Job objects with real data", jobs.size());
            return jobs;
            
        } catch (Exception e) {
            log.error("❌ Error during job scraping: {}", e.getMessage(), e);
            return new ArrayList<>();
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    log.warn("⚠️ Error closing WebDriver: {}", e.getMessage());
                }
            }
        }
    }

    private void clickLoadMoreButton(WebDriver driver) {
        log.info("🔄 Looking for Load More button...");
        
        // ✅ ДОДАНО: Різні варіанти кнопки "Load More"
        String[] loadMoreSelectors = {
            LOAD_MORE_SELECTOR,
            "button:contains('Load More')",
            "button:contains('Show More')",
            "button:contains('Load')",
            "a:contains('Load More')",
            "a:contains('Show More')",
            "[data-testid*='load-more']",
            "[data-testid*='show-more']",
            ".load-more",
            ".show-more",
            "button[class*='load']",
            "button[class*='more']",
            "a[class*='load']",
            "a[class*='more']"
        };
        
        for (String selector : loadMoreSelectors) {
            try {
                List<WebElement> buttons = driver.findElements(By.cssSelector(selector));
                if (!buttons.isEmpty()) {
                    WebElement button = buttons.get(0);
                    if (button.isDisplayed() && button.isEnabled()) {
                        log.info("✅ Load More button found with selector: '{}'", selector);
                        
                        // ✅ ДОДАНО: Скролимо до кнопки перед кліком
                        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", button);
                        sleep(1000);
                        
                        button.click();
                        sleep(scrollDelay);
                        log.info("✅ Load More button clicked successfully");
                        return;
                    }
                }
            } catch (Exception e) {
                log.debug("⚠️ Selector '{}' failed: {}", selector, e.getMessage());
            }
        }
        
        // ✅ ДОДАНО: Спробуємо знайти кнопку за текстом
        try {
            List<WebElement> allButtons = driver.findElements(By.cssSelector("button, a"));
            for (WebElement button : allButtons) {
                String buttonText = button.getText().toLowerCase();
                if (buttonText.contains("load") || buttonText.contains("more") || buttonText.contains("show")) {
                    if (button.isDisplayed() && button.isEnabled()) {
                        log.info("✅ Load More button found by text: '{}'", buttonText);
                        
                        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", button);
                        sleep(1000);
                        
                        button.click();
                        sleep(scrollDelay);
                        log.info("✅ Load More button clicked successfully");
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Text-based button search failed: {}", e.getMessage());
        }
        
        log.warn("⚠️ No Load More button found");
    }

    private void scrollToBottom(WebDriver driver) {
        log.info("📜 Starting scroll to bottom process...");
        
        // ✅ ДОДАНО: Перевіряємо початкову кількість карток
        int initialJobCount = driver.findElements(By.cssSelector(JOB_CARD_SELECTOR)).size();
        log.info("🔍 Initial job cards found: {}", initialJobCount);
        
        // ✅ ДОДАНО: Спробуємо альтернативні селектори
        if (initialJobCount == 0) {
            log.warn("⚠️ Primary selector found 0 cards, trying alternatives...");
            tryAlternativeSelectors(driver);
        }
        
        // ✅ ДОДАНО: Використовуємо покращений метод з Load More
        scrollToLoadMore(driver);
    }

    /**
     * ✅ НОВИЙ МЕТОД: Спробуємо альтернативні селектори
     */
    private void tryAlternativeSelectors(WebDriver driver) {
        log.info("🔍 Testing alternative selectors...");
        
        // Отримуємо HTML сторінки для аналізу
        String pageSource = driver.getPageSource();
        log.info("📄 Page source length: {} characters", pageSource.length());
        
        // Перевіряємо різні селектори
        String[] alternativeSelectors = {
            "div[class*='job']",
            "div[class*='position']", 
            "div[class*='vacancy']",
            "div[class*='card']",
            "div[class*='item']",
            "div[class*='listing']",
            "div[class*='posting']",
            "[data-testid*='job']",
            "[data-testid*='position']",
            "[data-testid*='card']",
            ".job-card",
            ".position-card",
            ".vacancy-card",
            ".job-item",
            ".position-item"
        };
        
        for (String selector : alternativeSelectors) {
            try {
                int count = driver.findElements(By.cssSelector(selector)).size();
                if (count > 0) {
                    log.info("✅ Alternative selector '{}' found {} elements", selector, count);
                } else {
                    log.debug("❌ Selector '{}' found 0 elements", selector);
                }
            } catch (Exception e) {
                log.debug("⚠️ Selector '{}' failed: {}", selector, e.getMessage());
            }
        }
        
        // ✅ ДОДАНО: Перевіряємо загальну структуру сторінки
        log.info("🔍 Page title: {}", driver.getTitle());
        log.info("🔍 Current URL: {}", driver.getCurrentUrl());
        
        // Шукаємо будь-які посилання
        int totalLinks = driver.findElements(By.cssSelector("a[href]")).size();
        log.info("🔗 Total links on page: {}", totalLinks);
        
        // Шукаємо будь-які div елементи
        int totalDivs = driver.findElements(By.cssSelector("div")).size();
        log.info("📦 Total div elements on page: {}", totalDivs);
    }

    /**
     * ✅ ОНОВЛЕНИЙ МЕТОД: Адаптивна прокрутка з гібридним завантаженням
     * 
     * НОВА ЛОГІКА:
     * 1. Спочатку натискаємо кнопку Load More ОДИН раз (якщо вона є)
     * 2. Потім запускаємо цикл нескінченної прокрутки
     * 3. Виходимо з циклу тільки коли кількість вакансій перестає зростати
     * 
     * Це дозволяє адаптуватися до гібридного підходу сайту:
     * - Спочатку кнопка Load More
     * - Потім автоматичне завантаження при прокрутці
     */
    private void scrollToLoadMore(WebDriver driver) {
        log.info("📜 Starting ADAPTIVE scroll and load more process with hybrid approach...");
        
        JavascriptExecutor js = (JavascriptExecutor) driver;
        
        int initialJobCount = driver.findElements(By.cssSelector(JOB_CARD_SELECTOR)).size();
        log.info("🔍 Initial job cards found: {}", initialJobCount);
        
        // ✅ КРОК 1: Спочатку натискаємо кнопку Load More ОДИН раз (якщо вона є)
        log.info("🔍 КРОК 1: Шукаємо та натискаємо кнопку Load More ОДИН раз...");
        boolean loadMoreClicked = clickLoadMoreButtonOnce(driver);
        
        if (loadMoreClicked) {
            log.info("✅ Load More button clicked successfully, waiting for content to load...");
            sleep(3000); // Даємо час на завантаження
        } else {
            log.info("ℹ️ No Load More button found or clicked, proceeding with scroll-only approach");
        }
        
        // ✅ КРОК 2: Запускаємо цикл нескінченної прокрутки
        log.info("🔍 КРОК 2: Запускаємо цикл нескінченної прокрутки...");
        int previousJobCount = driver.findElements(By.cssSelector(JOB_CARD_SELECTOR)).size();
        int scrollAttempts = 0;
        int maxScrollAttempts = 50; // Максимальна кількість спроб прокрутки
        int noNewJobsCount = 0;
        int maxNoNewJobsAttempts = 5; // Максимальна кількість спроб без нових вакансій
        
        while (scrollAttempts < maxScrollAttempts && noNewJobsCount < maxNoNewJobsAttempts) {
            scrollAttempts++;
            
            // ✅ Крок 2.1: Скролимо до низу сторінки
            log.info("📜 Scroll attempt {}/{}: Scrolling to bottom...", scrollAttempts, maxScrollAttempts);
            js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            sleep(2000); // Чекаємо завантаження
            
            // ✅ Крок 2.2: Перевіряємо, чи з'явилися нові вакансії
            int currentJobCount = driver.findElements(By.cssSelector(JOB_CARD_SELECTOR)).size();
            log.info("🔍 Current job count: {} (was: {})", currentJobCount, previousJobCount);
            
            if (currentJobCount > previousJobCount) {
                // Знайшли нові вакансії - продовжуємо
                int newJobs = currentJobCount - previousJobCount;
                log.info("🎉 Found {} new jobs after scroll! Total: {}", newJobs, currentJobCount);
                previousJobCount = currentJobCount;
                noNewJobsCount = 0; // Скидаємо лічильник
                
                // Додаткова затримка для завантаження
                sleep(2000);
                
            } else {
                // Нові вакансії не з'явилися
                noNewJobsCount++;
                log.info("⚠️ No new jobs found. Attempt {}/{} without new jobs", noNewJobsCount, maxNoNewJobsAttempts);
                
                // Спробуємо додаткову прокрутку
                if (noNewJobsCount < maxNoNewJobsAttempts) {
                    log.info("📜 Trying additional scroll...");
                    js.executeScript("window.scrollTo(0, document.body.scrollHeight - 100);");
                    sleep(1000);
                    
                    // Перевіряємо ще раз
                    int finalJobCount = driver.findElements(By.cssSelector(JOB_CARD_SELECTOR)).size();
                    if (finalJobCount > currentJobCount) {
                        log.info("🎉 Additional scroll helped! Found {} more jobs", finalJobCount - currentJobCount);
                        currentJobCount = finalJobCount;
                        previousJobCount = finalJobCount;
                        noNewJobsCount = 0; // Скидаємо лічильник
                    }
                }
            }
            
            // Перевіряємо, чи не досягли ми бажаної кількості
            if (currentJobCount >= 369) {
                log.info("🎯 Reached target job count: {}", currentJobCount);
                break;
            }
            
            // Перевіряємо, чи не занадто довго чекаємо
            if (scrollAttempts % 10 == 0) {
                log.info("📊 Progress: {} scroll attempts, {} jobs found, {} attempts without new jobs", 
                        scrollAttempts, currentJobCount, noNewJobsCount);
            }
        }
        
        int finalJobCount = driver.findElements(By.cssSelector(JOB_CARD_SELECTOR)).size();
        log.info("🏁 ADAPTIVE scroll and load more process completed:");
        log.info("   • Total scroll attempts: {}", scrollAttempts);
        log.info("   • Final job count: {}", finalJobCount);
        log.info("   • Jobs added: {}", finalJobCount - initialJobCount);
        log.info("   • Load More button clicked: {}", loadMoreClicked);
        
        if (noNewJobsCount >= maxNoNewJobsAttempts) {
            log.info("ℹ️ Process stopped: {} consecutive attempts without new jobs", maxNoNewJobsAttempts);
        }
        
        if (scrollAttempts >= maxScrollAttempts) {
            log.info("ℹ️ Process stopped: reached maximum scroll attempts ({})", maxScrollAttempts);
        }
    }
    
    /**
     * ✅ НОВИЙ МЕТОД: Натискання кнопки Load More ОДИН раз
     */
    private boolean clickLoadMoreButtonOnce(WebDriver driver) {
        log.info("🔍 Looking for Load More button to click ONCE...");
        
        try {
            // Спробуємо різні варіанти кнопки Load More
            String[] loadMoreTexts = {"Load More", "Show More", "Load", "More", "See More"};
            WebElement loadMoreButton = null;
            
            for (String text : loadMoreTexts) {
                try {
                    // Шукаємо за текстом кнопки
                    String xpath = String.format("//button[contains(text(), '%s')] | //a[contains(text(), '%s')]", text, text);
                    List<WebElement> buttons = driver.findElements(By.xpath(xpath));
                    
                    for (WebElement button : buttons) {
                        if (button.isDisplayed() && button.isEnabled()) {
                            loadMoreButton = button;
                            log.info("✅ Found Load More button with text: '{}'", text);
                            break;
                        }
                    }
                    
                    if (loadMoreButton != null) break;
                    
                    // Шукаємо за CSS селекторами
                    String[] selectors = {
                        "[data-testid*='load-more']",
                        "[data-testid*='show-more']",
                        ".load-more",
                        ".show-more",
                        "button[class*='load']",
                        "button[class*='more']"
                    };
                    
                    for (String selector : selectors) {
                        try {
                            List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                            for (WebElement element : elements) {
                                if (element.isDisplayed() && element.isEnabled()) {
                                    loadMoreButton = element;
                                    log.info("✅ Found Load More button with selector: '{}'", selector);
                                    break;
                                }
                            }
                            if (loadMoreButton != null) break;
                        } catch (Exception e) {
                            // Ігноруємо помилки селекторів
                        }
                    }
                    
                    if (loadMoreButton != null) break;
                    
                } catch (Exception e) {
                    // Продовжуємо пошук
                }
            }
            
            if (loadMoreButton != null) {
                // ✅ Клікаємо на кнопку Load More ОДИН раз
                log.info("🖱️ Clicking Load More button ONCE...");
                
                // Скролимо до кнопки перед кліком
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", loadMoreButton);
                sleep(1000);
                
                // Клікаємо
                loadMoreButton.click();
                
                log.info("✅ Load More button clicked ONCE successfully");
                return true;
                
            } else {
                log.info("ℹ️ No Load More button found");
                return false;
            }
            
        } catch (Exception e) {
            log.warn("⚠️ Error clicking Load More button: {}", e.getMessage());
            return false;
        }
    }

    /**
     * ✅ ОНОВЛЕНА ВЕРСІЯ СКРАПІНГУ З НОВОЮ ЛОГІКОЮ ТА ГІБРИДНИМ ЗАВАНТАЖЕННЯМ
     * 
     * НОВА ЛОГІКА:
     * 1. Спочатку вибирається job Function (фільтрація за функціями)
     * 2. Потім натискається кнопка Load More ОДИН раз (якщо вона є)
     * 3. Далі запускається цикл нескінченної прокрутки з автоматичним завантаженням
     * 4. Виходимо з циклу тільки коли кількість вакансій перестає зростати
     * 5. І тільки потім зчитування URL та перевірка префіксу
     * 6. Якщо URL містить https://jobs.techstars.com/companies/ - зберігаємо БЕЗ перевірки тегів
     * 
     * ГІБРИДНИЙ ПІДХІД:
     * - Спочатку кнопка Load More (якщо є)
     * - Потім автоматичне завантаження при прокрутці
     * - Адаптивне завершення коли контент більше не завантажується
     */
    private List<Job> scrapeAllJobsWithImprovedLogic(WebDriver driver, List<String> requiredTags, List<String> jobFunctions) {
        log.info("🔍 Starting updated job scraping process with NEW LOGIC...");
        log.info("🔍 Job functions to filter by: {} (type: {})", jobFunctions, 
                jobFunctions != null ? jobFunctions.getClass().getSimpleName() : "null");
        log.info("🔍 Required tags to filter by: {} (type: {})", requiredTags, 
                requiredTags != null ? requiredTags.getClass().getSimpleName() : "null");
        
        if (jobFunctions != null) {
            for (int i = 0; i < jobFunctions.size(); i++) {
                String function = jobFunctions.get(i);
                log.info("🔍 Job function {}: '{}' (type: {})", i, function, 
                        function != null ? function.getClass().getSimpleName() : "null");
            }
        }
        
        // ✅ КРОК 1: Спочатку фільтрація за job functions
        log.info("🔍 КРОК 1: Застосовуємо фільтрацію за job functions...");
        
        // ✅ КРОК 2: Натискаємо кнопку Load More ОДИН раз
        log.info("🔍 КРОК 2: Натискаємо кнопку Load More ОДИН раз...");
        clickLoadMoreButton(driver);
        
        // ✅ КРОК 3: Скролимо сторінку до низу
        log.info("🔍 КРОК 3: Скролимо сторінку до низу...");
        scrollToBottom(driver);
        
        // ✅ КРОК 4: Тепер шукаємо всі картки вакансій
        log.info("🔍 КРОК 4: Шукаємо всі картки вакансій після завантаження...");
        List<WebElement> jobCards = findJobCardsWithMultipleStrategies(driver);
        List<Job> jobs = new ArrayList<>();
        
        log.info("📋 Found {} job cards to process", jobCards.size());
        
        if (jobCards.isEmpty()) {
            log.error("❌ CRITICAL: No job cards found with any strategy!");
            return jobs;
        }
        
        int passedTagFilter = 0;
        int passedFunctionFilter = 0;
        int foundUrls = 0;
        int savedWithCompanyPrefix = 0;
        int savedWithoutCompanyPrefix = 0;
        
        for (int i = 0; i < jobCards.size(); i++) {
            try {
                WebElement card = jobCards.get(i);
                
                // Логуємо тільки перші 5 карток для діагностики
                boolean isFirstCards = i < 5;
                if (isFirstCards) {
                    String cardText = card.getText();
                    String preview = cardText.length() > 200 ? cardText.substring(0, 200) + "..." : cardText;
                    log.info("🔍 Processing card {}: {}", i + 1, preview);
                }
                
                // ✅ КРОК 5: Фільтрація за job functions (ПЕРШИЙ КРОК ЗА НОВОЮ ЛОГІКОЮ)
                if (!hasRequiredJobFunction(card, jobFunctions)) {
                    if (isFirstCards) {
                        log.info("🔍 Card {} failed function filter", i + 1);
                        // Додаткова діагностика для перших карток
                        String cardText = card.getText().toLowerCase();
                        log.info("🔍 Card {} text preview: '{}'", i + 1, 
                                cardText.length() > 300 ? cardText.substring(0, 300) + "..." : cardText);
                    }
                    continue;
                }
                passedFunctionFilter++;
                
                if (isFirstCards) {
                    log.info("🔍 Card {} passed function filter", i + 1);
                }
                
                // ✅ КРОК 6: Пошук URL (ДРУГИЙ КРОК ЗА НОВОЮ ЛОГІКОЮ)
                String jobPageUrl = findDirectJobUrl(card);
                if (jobPageUrl == null) {
                    if (isFirstCards) {
                        log.info("🔍 Card {}: No URL found after passing function filter", i + 1);
                    }
                    continue;
                }
                
                foundUrls++;
                
                if (isFirstCards) {
                    log.info("🔍 Card {}: URL found: {}", i + 1, jobPageUrl);
                }
                
                // ✅ КРОК 7: Збереження вакансії (всі проходять однакову обробку)
                Job job = createJobFromCard(card, jobPageUrl, jobFunctions);
                if (job != null) {
                    jobs.add(job);
                    if (jobPageUrl.startsWith(REQUIRED_PREFIX)) {
                        savedWithCompanyPrefix++;
                        log.info("✅ Card {} saved (with company prefix)", i + 1);
                    } else {
                        savedWithoutCompanyPrefix++;
                        log.info("✅ Card {} saved (without company prefix)", i + 1);
                    }
                }
                
                if (i % 10 == 0) {
                    log.info("📊 Processed {}/{} job cards", i + 1, jobCards.size());
                }
                
            } catch (Exception e) {
                log.warn("⚠️ Error scraping job card {}: {}", i + 1, e.getMessage());
            }
        }
        
        // ✅ ОНОВЛЕНО: Розширений звіт з новою логікою
        printUpdatedFinalReport(jobCards.size(), passedTagFilter, passedFunctionFilter, foundUrls, 
                               jobs.size(), savedWithCompanyPrefix, savedWithoutCompanyPrefix, jobFunctions);
        return jobs;
    }

    /**
     * ✅ ОНОВЛЕНИЙ МЕТОД: Визначає тип сторінки та застосовує відповідну логіку скрапінгу
     * Всі методи тепер використовують нову гібридну логіку: 
     * 1) job functions → 2) Load More (ОДИН раз) → 3) нескінченна прокрутка → 4) URL → 5) префікс компанії → 6) збір тегів
     */
    private List<Job> scrapeJobsBasedOnPageType(WebDriver driver, List<String> requiredTags, List<String> jobFunctions) {
        String currentUrl = driver.getCurrentUrl();
        log.info("🔍 Current URL: {}", currentUrl);
        
        if (currentUrl.contains("/companies/") && currentUrl.contains("/jobs/")) {
            // Детальна сторінка вакансії
            log.info("🎯 Detected job detail page, applying new filtering logic...");
            return scrapeSingleJobFromDetailPage(driver, jobFunctions);
            
        } else if (currentUrl.contains("/companies/")) {
            // Сторінка компанії зі списком вакансій
            log.info("🏢 Detected company page, applying new filtering logic...");
            return scrapeJobsFromCompanyPage(driver, requiredTags, jobFunctions);
            
        } else if (currentUrl.contains("/jobs")) {
            // Головна сторінка зі списком вакансій
            log.info("📋 Detected main jobs page, applying new filtering logic...");
            return scrapeJobsFromMainPage(driver, requiredTags, jobFunctions);
            
        } else {
            // Невідома сторінка
            log.warn("⚠️ Unknown page type, trying default scraping with new logic...");
            return scrapeJobsFromMainPage(driver, requiredTags, jobFunctions);
        }
    }
    
    /**
     * ✅ ОНОВЛЕНИЙ МЕТОД: Скрапінг однієї вакансії з детальної сторінки з новою логікою
     */
    private List<Job> scrapeSingleJobFromDetailPage(WebDriver driver, List<String> jobFunctions) {
        List<Job> jobs = new ArrayList<>();
        
        try {
            String currentUrl = driver.getCurrentUrl();
            
            // ✅ КРОК 1: Перевіряємо, чи URL містить потрібний префікс компанії
            if (currentUrl.startsWith(REQUIRED_PREFIX)) {
                log.info("🔍 Detail page: URL contains company prefix '{}', applying new logic", REQUIRED_PREFIX);
                
                // ✅ КРОК 1: Фільтрація за функціями (ПЕРШИЙ КРОК ЗА НОВОЮ ЛОГІКОЮ)
                if (jobFunctions != null && !jobFunctions.isEmpty()) {
                    // Шукаємо заголовок вакансії для перевірки функції
                    String positionName = extractTitleFromDetailPage(driver);
                    if (positionName == null || positionName.trim().isEmpty()) {
                        log.warn("⚠️ Could not extract position name from detail page");
                        return jobs;
                    }
                    
                    String positionText = positionName.toLowerCase();
                    boolean hasRequiredFunction = jobFunctions.stream()
                        .anyMatch(function -> positionText.contains(function.toLowerCase()));
                    
                    if (!hasRequiredFunction) {
                        log.info("🔍 Detail page: Position '{}' does not match required functions: {}", positionName, jobFunctions);
                        return jobs; // Не зберігаємо, якщо не відповідає функціям
                    }
                }
                
                // ✅ КРОК 2: Збираємо всі дані та зберігаємо (всі проходять однакову обробку)
                log.info("🔍 Detail page: All filters passed, saving job (tags will be collected)");
                
                // Шукаємо заголовок вакансії
                String positionName = extractTitleFromDetailPage(driver);
                if (positionName == null || positionName.trim().isEmpty()) {
                    log.warn("⚠️ Could not extract position name from detail page");
                    return jobs;
                }
                
                // Шукаємо назву компанії
                String companyName = extractCompanyNameFromDetailPage(driver);
                
                // Шукаємо теги
                List<String> tags = extractTagsFromDetailPage(driver);
                
                // Шукаємо локацію
                String location = extractLocationFromDetailPage(driver);
                
                // Шукаємо дату публікації
                LocalDateTime postedDate = extractPostedDateFromDetailPage(driver);
                
                // ✅ ДОДАНО: Шукаємо опис вакансії
                String description = extractDescriptionFromDetailPage(driver);
                
                // ✅ ДОДАНО: Додаткова перевірка, щоб не зберігати назву вакансії як опис
                if (description != null && description.equals(positionName)) {
                    log.debug("📝 Skipping description as it matches position name: '{}'", description);
                    description = null;
                }
                
                // Створюємо Job об'єкт
                Job job = jobCreationService.createJobWithAllData(
                    currentUrl, positionName, companyName, null, location, tags, postedDate, jobFunctions, description
                );
                
                if (job != null) {
                    // ✅ ДОДАНО: Зберігаємо опис вакансії через DescriptionIngestService (тільки якщо це не заглушка)
                    if (description != null && !description.trim().isEmpty() && 
                        !description.equals("Job scraped from Techstars")) {
                        try {
                            boolean descriptionSaved = descriptionIngestService.saveDescription(job, description);
                            if (descriptionSaved) {
                                log.info("✅ Successfully saved description for job ID: {}", job.getId());
                            } else {
                                log.warn("⚠️ Failed to save description for job ID: {}", job.getId());
                            }
                        } catch (Exception e) {
                            log.error("❌ Error saving description for job ID: {}, error: {}", 
                                    job.getId(), e.getMessage(), e);
                        }
                    }
                    
                    jobs.add(job);
                    log.info("✅ Successfully scraped job: {}", positionName);
                }
                
            } else {
                // URL не містить префікс компанії - застосовуємо стандартну логіку
                log.info("🔍 Detail page: URL does not contain company prefix, applying standard filtering");
                
                // Шукаємо заголовок вакансії
                String positionName = extractTitleFromDetailPage(driver);
                if (positionName == null || positionName.trim().isEmpty()) {
                    log.warn("⚠️ Could not extract position name from detail page");
                    return jobs;
                }
                
                // ✅ КРОК 2: Фільтрація за функціями (ПЕРШИЙ КРОК ЗА НОВОЮ ЛОГІКОЮ)
                if (jobFunctions != null && !jobFunctions.isEmpty()) {
                    String positionText = positionName.toLowerCase();
                    boolean hasRequiredFunction = jobFunctions.stream()
                        .anyMatch(function -> positionText.contains(function.toLowerCase()));
                    
                    if (!hasRequiredFunction) {
                        log.info("🔍 Detail page: Position '{}' does not match required functions: {}", positionName, jobFunctions);
                        return jobs; // Не зберігаємо, якщо не відповідає функціям
                    }
                }
                
                // ✅ КРОК 3: Збираємо всі дані та зберігаємо
                log.info("🔍 Detail page: All filters passed, saving job with standard filtering (tags will be collected)");
                
                // Шукаємо назву компанії
                String companyName = extractCompanyNameFromDetailPage(driver);
                
                // Шукаємо теги
                List<String> tags = extractTagsFromDetailPage(driver);
                
                // Шукаємо локацію
                String location = extractLocationFromDetailPage(driver);
                
                // Шукаємо дату публікації
                LocalDateTime postedDate = extractPostedDateFromDetailPage(driver);
                
                // ✅ ДОДАНО: Шукаємо опис вакансії
                String description = extractDescriptionFromDetailPage(driver);
                
                // Створюємо Job об'єкт
                Job job = jobCreationService.createJobWithAllData(
                    currentUrl, positionName, companyName, null, location, tags, postedDate, jobFunctions, description
                );
                
                if (job != null) {
                    jobs.add(job);
                    log.info("✅ Successfully scraped job with standard filtering: {}", positionName);
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Error scraping job from detail page: {}", e.getMessage());
        }
        
        return jobs;
    }
    
    /**
     * ✅ ОНОВЛЕНИЙ МЕТОД: Скрапінг вакансій зі сторінки компанії з новою гібридною логікою
     * НЕ ПЕРЕВІРЯЄМО теги для URL з префіксом компанії
     * Використовує: 1) job functions → 2) URL → 3) префікс компанії → 4) збір тегів
     */
    private List<Job> scrapeJobsFromCompanyPage(WebDriver driver, List<String> requiredTags, List<String> jobFunctions) {
        List<Job> jobs = new ArrayList<>();
        
        try {
            // Шукаємо картки вакансій на сторінці компанії
            List<WebElement> jobCards = findJobCardsOnCompanyPage(driver);
            log.info("🔍 Found {} job cards on company page", jobCards.size());
            
            for (WebElement card : jobCards) {
                try {
                    // ✅ КРОК 1: Фільтрація за функціями (ПЕРШИЙ КРОК ЗА НОВОЮ ЛОГІКОЮ)
                    if (!hasRequiredJobFunction(card, jobFunctions)) {
                        continue;
                    }
                    
                    // ✅ КРОК 2: Пошук URL (ДРУГИЙ КРОК ЗА НОВОЮ ЛОГІКОЮ)
                    String jobPageUrl = findDirectJobUrl(card);
                    if (jobPageUrl == null) {
                        continue;
                    }
                    
                    // ✅ КРОК 3: Збереження вакансії (всі проходять однакову обробку)
                    Job job = createJobFromCard(card, jobPageUrl, jobFunctions);
                    if (job != null) {
                        jobs.add(job);
                    }
                    
                } catch (Exception e) {
                    log.warn("⚠️ Error processing job card on company page: {}", e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Error scraping jobs from company page: {}", e.getMessage());
        }
        
        return jobs;
    }
    
    /**
     * ✅ ОНОВЛЕНИЙ МЕТОД: Скрапінг вакансій з головної сторінки
     * Використовує нову гібридну логіку: 
     * 1) job functions → 2) Load More (ОДИН раз) → 3) нескінченна прокрутка → 4) URL → 5) префікс компанії → 6) збір тегів
     */
    private List<Job> scrapeJobsFromMainPage(WebDriver driver, List<String> requiredTags, List<String> jobFunctions) {
        // Використовуємо оновлену логіку з новим порядком фільтрації
        return scrapeAllJobsWithImprovedLogic(driver, requiredTags, jobFunctions);
    }

    /**
     * ✅ НОВИЙ МЕТОД: Знаходимо картки вакансій кількома стратегіями
     */
    private List<WebElement> findJobCardsWithMultipleStrategies(WebDriver driver) {
        log.info("🔍 Finding job cards with multiple strategies...");
        
        // ✅ ДОДАНО: Детальне логування для діагностики
        log.info("🔍 Testing {} specific selectors from ScrapingSelectors.JOB_CARD", ScrapingSelectors.JOB_CARD.length);
        
        // Спробуємо всі селектори з ScrapingSelectors.JOB_CARD
        for (int i = 0; i < ScrapingSelectors.JOB_CARD.length; i++) {
            String selector = ScrapingSelectors.JOB_CARD[i];
            try {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                log.info("🔍 Selector {}: '{}' -> found {} elements", i + 1, selector, elements.size());
                
                if (!elements.isEmpty()) {
                    // ✅ ДОДАНО: Валідація елементів - фільтруємо неправильні
                    List<WebElement> validElements = filterValidJobCards(elements);
                    log.info("🔍 After validation: {} valid elements out of {} total", validElements.size(), elements.size());
                    
                    if (!validElements.isEmpty()) {
                        log.info("✅ Found {} valid job cards with selector: '{}'", validElements.size(), selector);
                        return validElements;
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ Selector '{}' failed: {}", selector, e.getMessage());
            }
        }
        
        // Якщо нічого не знайшли, спробуємо загальні селектори
        log.warn("⚠️ No job cards found with specific selectors, trying general selectors...");
        
        String[] generalSelectors = {
            "div[class*='job']", "div[class*='position']", "div[class*='card']", 
            "div[class*='item']", "div[class*='listing']", "div[class*='posting']",
            "div[class*='sc-']", "div[class*='opportunity']"
        };
        
        for (String selector : generalSelectors) {
            try {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                log.info("🔍 General selector '{}' -> found {} elements", selector, elements.size());
                
                if (!elements.isEmpty()) {
                    log.info("✅ Found {} elements with general selector: '{}'", elements.size(), selector);
                    return elements;
                }
            } catch (Exception e) {
                log.debug("⚠️ General selector '{}' failed: {}", selector, e.getMessage());
            }
        }
        
        // Остання спроба - знайти будь-які div елементи
        log.warn("⚠️ No specific elements found, trying to find any div elements...");
        List<WebElement> allDivs = driver.findElements(By.tagName("div"));
        log.info("🔍 Found {} total div elements on page", allDivs.size());
        
        // ✅ ДОДАНО: Аналіз перших кількох div елементів для діагностики
        int sampleSize = Math.min(10, allDivs.size());
        for (int i = 0; i < sampleSize; i++) {
            try {
                WebElement div = allDivs.get(i);
                String className = div.getAttribute("class");
                String dataTestId = div.getAttribute("data-testid");
                String tagName = div.getTagName();
                String text = div.getText();
                log.info("🔍 Div {}: tag='{}', class='{}', data-testid='{}', text='{}'", 
                    i + 1, tagName, className, dataTestId, 
                    text.length() > 50 ? text.substring(0, 50) + "..." : text);
            } catch (Exception e) {
                log.warn("⚠️ Error analyzing div {}: {}", i + 1, e.getMessage());
            }
        }
        
        // Повертаємо перші 50 div елементів для аналізу
        return allDivs.subList(0, Math.min(50, allDivs.size()));
    }

    /**
     * ✅ НОВА, НАДІЙНА ВЕРСІЯ МЕТОДУ
     * Шукає пряме посилання на вакансію в картці, використовуючи кілька стратегій
     */
    private String findDirectJobUrl(WebElement jobCard) {
        try {
            // Стратегія 1: Шукаємо посилання за унікальним атрибутом data-testid
            try {
                WebElement specificLink = jobCard.findElement(By.cssSelector("a[data-testid='job-card-link']"));
                String url = specificLink.getAttribute("href");
                if (url != null && !url.isBlank()) {
                    log.debug("🔍 Стратегія 1: Знайдено URL за data-testid: {}", url);
                    return url;
                }
            } catch (Exception e) {
                // Стратегія 1 не спрацювала
            }

            // Стратегія 2: Перевіряємо, чи є батьківський елемент картки посиланням
            try {
                WebElement parent = jobCard.findElement(By.xpath(".."));
                if (parent != null && "a".equals(parent.getTagName())) {
                    String url = parent.getAttribute("href");
                    if (url != null && !url.isBlank()) {
                        log.debug("🔍 Стратегія 2: Знайдено URL у батьківського елемента: {}", url);
                        return url;
                    }
                }
            } catch (Exception e) {
                // Стратегія 2 не спрацювала
            }

            // Стратегія 3: Шукаємо перше посилання всередині картки
            try {
                List<WebElement> allLinks = jobCard.findElements(By.cssSelector("a[href]"));
                for (WebElement link : allLinks) {
                    String url = link.getAttribute("href");
                    if (url != null && (url.contains("/jobs/") || url.contains("/companies/"))) {
                        log.debug("🔍 Стратегія 3: Знайдено URL за вмістом href: {}", url);
                        return url;
                    }
                }
            } catch (Exception e) {
                // Стратегія 3 не спрацювала
            }

            log.debug("⚠️ Жодна стратегія пошуку URL не спрацювала для цієї картки");
            return null;
            
        } catch (Exception e) {
            log.warn("⚠️ Error in findDirectJobUrl: {}", e.getMessage());
            return null;
        }
    }

    private boolean hasRequiredTags(WebElement jobCard, List<String> requiredTags) {
        if (requiredTags == null || requiredTags.isEmpty()) {
            log.debug("🔍 No required tags specified, passing all cards");
            return true;
        }
        
        try {
            String cardText = jobCard.getText().toLowerCase();
            log.debug("🔍 Card text (first 200 chars): '{}'", 
                cardText.length() > 200 ? cardText.substring(0, 200) + "..." : cardText);
            
            boolean hasTags = requiredTags.stream()
                    .anyMatch(tag -> {
                        boolean contains = cardText.contains(tag.toLowerCase());
                        log.debug("🔍 Tag '{}' found: {}", tag, contains);
                        return contains;
                    });
            
            log.debug("🔍 Card passed tag filter: {}", hasTags);
            return hasTags;
        } catch (Exception e) {
            log.warn("⚠️ Error checking tags: {}", e.getMessage());
            return true; // В разі помилки пропускаємо
        }
    }

    private boolean hasRequiredJobFunction(WebElement jobCard, List<String> jobFunctions) {
        log.info("🔍 Checking job functions: {}", jobFunctions);
        
        if (jobFunctions == null || jobFunctions.isEmpty()) {
            log.info("🔍 No required job functions specified, passing all cards");
            return true;
        }
        
        try {
            String cardText = jobCard.getText().toLowerCase();
            log.info("🔍 Card text (first 200 chars): '{}'", 
                cardText.length() > 200 ? cardText.substring(0, 200) + "..." : cardText);
            
            boolean hasFunction = jobFunctions.stream()
                    .anyMatch(function -> {
                        String functionName = function.toLowerCase();
                        boolean contains = cardText.contains(functionName);
                        log.info("🔍 Job function '{}' found: {} in card text", functionName, contains);
                        if (!contains) {
                            log.info("🔍 Card text does not contain '{}'. Full card text: '{}'", functionName, cardText);
                        }
                        return contains;
                    });
            
            log.info("🔍 Card passed job function filter: {}", hasFunction);
            return hasFunction;
        } catch (Exception e) {
            log.warn("⚠️ Error checking job functions: {}", e.getMessage());
            return true; // В разі помилки пропускаємо
        }
    }

    private Job createJobFromCard(WebElement card, String jobPageUrl, List<String> jobFunctions) {
        try {
            log.debug("🔍 Creating Job object for URL: {}", jobPageUrl);
            
            // ✅ ВИПРАВЛЕНО: Використовуємо новий метод для витягування назви компанії
            String organizationTitle = extractCompanyNameFromCard(card, jobPageUrl);
            
            // ✅ ДОДАНО: Шукаємо назву позиції
            String positionName = extractTitleFromCard(card);
            
            // ✅ ДОДАНО: Витягуємо додаткову інформацію
            List<String> tags = extractTagsFromCard(card);
            String location = extractLocationFromCard(card);
            LocalDateTime postedDate = extractPostedDateFromCard(card);
            String logoUrl = extractLogoUrlFromCard(card);
            
            // ✅ ДОДАНО: Витягуємо опис вакансії
            String description = extractDescriptionFromCard(card);
            
            String defaultFunction = jobFunctions.isEmpty() ? 
                    "Software Engineering" : jobFunctions.get(0);
            
            log.info("🏢 Company name extracted: '{}' for URL: {}", organizationTitle, jobPageUrl);
            log.info("💼 Position name: '{}'", positionName);
            log.info("🏷️ Tags found: {}", tags);
            log.info("📍 Location: '{}'", location);
            log.info("📅 Posted date: '{}' (Unix: {})", postedDate, 
                    postedDate != null ? postedDate.toEpochSecond(java.time.ZoneOffset.UTC) : "null");
            log.info("🖼️ Logo URL: '{}'", logoUrl);
            log.info("📝 Description: '{}'", description != null ? description : "Not found");
            
            // ✅ ВИПРАВЛЕНО: Використовуємо JobCreationService для створення Job з усіма даними
            Job job = jobCreationService.createJobWithAllData(
                jobPageUrl, positionName, organizationTitle, logoUrl, location, tags, postedDate, jobFunctions, description
            );
            
            // ✅ ДОДАНО: Зберігаємо опис вакансії через DescriptionIngestService (тільки якщо це не заглушка)
            if (job != null && description != null && !description.trim().isEmpty() && 
                !description.equals("Job scraped from Techstars")) {
                try {
                    boolean descriptionSaved = descriptionIngestService.saveDescription(job, description);
                    if (descriptionSaved) {
                        log.info("✅ Successfully saved description for job ID: {}", job.getId());
                    } else {
                        log.warn("⚠️ Failed to save description for job ID: {}", job.getId());
                    }
                } catch (Exception e) {
                    log.error("❌ Error saving description for job ID: {}, error: {}", 
                            job.getId(), e.getMessage(), e);
                }
            }
            
            return job;
                    
        } catch (Exception e) {
            log.warn("⚠️ Error creating Job object: {}", e.getMessage());
            return null;
        }
    }

    private String getElementText(WebElement root, String selector) {
        try {
            log.debug("🔍 Searching for element with selector: '{}'", selector);
            WebElement element = root.findElement(By.cssSelector(selector));
            String text = element.getText();
            log.debug("🔍 Found element with selector '{}', text: '{}'", selector, text);
            return text;
        } catch (Exception e) {
            log.debug("⚠️ Element not found with selector '{}': {}", selector, e.getMessage());
            return null;
        }
    }

    private void printFinalReport(int totalCards, int passedTagFilter, int passedFunctionFilter, 
                                int foundUrls, int finalJobs, List<String> functions) {
        log.info("📊 ЗВІТ ПРО ФІЛЬТРАЦІЮ:");
        log.info("   • Всього карток: {}", totalCards);
        log.info("   • Пройшли фільтр тегів: {}", passedTagFilter);
        log.info("   • Пройшли фільтр функцій: {}", passedFunctionFilter);
        log.info("   • Знайдено URL: {}", foundUrls);
        log.info("   • Фінальних вакансій: {}", finalJobs);
        
        if (totalCards > 0) {
            log.info("   • Ефективність фільтрації тегів: {:.1f}%", (double) passedTagFilter / totalCards * 100);
            log.info("   • Ефективність фільтрації функцій: {:.1f}%", (double) passedFunctionFilter / totalCards * 100);
        }
        if (passedFunctionFilter > 0) {
            log.info("   • Конверсія в URL: {:.1f}%", (double) foundUrls / passedFunctionFilter * 100);
        }
        if (foundUrls > 0) {
            log.info("   • Конверсія в фінальні вакансії: {:.1f}%", (double) finalJobs / foundUrls * 100);
        }
        log.info("   • Застосовані функції: {}", functions);
        
        if (passedFunctionFilter > 0 && foundUrls == 0) {
            log.error("❌ КРИТИЧНА ПОМИЛКА: Всі {} відфільтрованих карток не дали URL!", passedFunctionFilter);
        }
        
        if (foundUrls > 0 && finalJobs == 0) {
            log.error("❌ КРИТИЧНА ПОМИЛКА: Всі {} знайдених URL не пройшли фінальну перевірку!", foundUrls);
        }
        
        log.info("🎯 Результат: {} з {} карток успішно оброблено", finalJobs, totalCards);
    }

    /**
     * ✅ НОВИЙ МЕТОД: Розширений звіт з новою логікою фільтрації
     */
    private void printUpdatedFinalReport(int totalCards, int passedTagFilter, int passedFunctionFilter, 
                                       int foundUrls, int finalJobs, int savedWithCompanyPrefix, 
                                       int savedWithoutCompanyPrefix, List<String> functions) {
        log.info("📊 ОНОВЛЕНИЙ ЗВІТ ПРО ФІЛЬТРАЦІЮ (НОВА ЛОГІКА):");
        log.info("   • Всього карток: {}", totalCards);
        log.info("   • Пройшли фільтр функцій: {}", passedFunctionFilter);
        log.info("   • Знайдено URL: {}", foundUrls);
        log.info("   • Збережено з префіксом компанії (БЕЗ перевірки тегів): {}", savedWithCompanyPrefix);
        log.info("   • Збережено без префіксу компанії (тільки фільтр функцій): {}", savedWithoutCompanyPrefix);
        log.info("   • Фінальних вакансій: {}", finalJobs);
        
        if (totalCards > 0) {
            log.info("   • Ефективність фільтрації функцій: {:.1f}%", (double) passedFunctionFilter / totalCards * 100);
        }
        if (passedFunctionFilter > 0) {
            log.info("   • Конверсія в URL: {:.1f}%", (double) foundUrls / passedFunctionFilter * 100);
        }
        if (foundUrls > 0) {
            log.info("   • Конверсія в фінальні вакансії: {:.1f}%", (double) finalJobs / foundUrls * 100);
            log.info("   • Частка збережених з префіксом компанії: {:.1f}%", (double) savedWithCompanyPrefix / foundUrls * 100);
            log.info("   • Частка збережених без префіксу компанії: {:.1f}%", (double) savedWithoutCompanyPrefix / foundUrls * 100);
        }
        log.info("   • Застосовані функції: {}", functions);
        
        // ✅ ДОДАНО: Перевірка нової логіки
        if (savedWithCompanyPrefix > 0) {
            log.info("✅ Нова логіка працює: {} вакансій збережено з префіксом компанії БЕЗ перевірки тегів", savedWithCompanyPrefix);
        }
        
        if (passedFunctionFilter > 0 && foundUrls == 0) {
            log.error("❌ КРИТИЧНА ПОМИЛКА: Всі {} відфільтрованих карток не дали URL!", passedFunctionFilter);
        }
        
        if (foundUrls > 0 && finalJobs == 0) {
            log.error("❌ КРИТИЧНА ПОМИЛКА: Всі {} знайдених URL не пройшли фінальну перевірку!", foundUrls);
        }
        
        log.info("🎯 Результат: {} з {} карток успішно оброблено", finalJobs, totalCards);
        log.info("🔍 НОВА ГІБРИДНА ЛОГІКА: 1) job functions → 2) Load More (ОДИН раз) → 3) нескінченна прокрутка → 4) URL → 5) префікс компанії → 6) збір тегів (без фільтрації)");
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("⚠️ Thread interrupted during sleep");
        }
    }

    /**
     * ✅ НОВИЙ МЕТОД: Витягує теги з картки вакансії
     */
    private List<String> extractTagsFromCard(WebElement card) {
        List<String> tags = new ArrayList<>();
        try {
            // Шукаємо всі елементи з data-testid="tag"
            List<WebElement> tagElements = card.findElements(By.cssSelector("[data-testid='tag']"));
            
            for (WebElement tagElement : tagElements) {
                try {
                    String tagText = tagElement.getText().trim();
                    if (!tagText.isEmpty()) {
                        tags.add(tagText);
                        log.debug("🏷️ Found tag: '{}'", tagText);
                    }
                } catch (Exception e) {
                    log.debug("⚠️ Error extracting tag text: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Error searching for tags: {}", e.getMessage());
        }
        
        log.debug("🏷️ Extracted {} tags from card", tags.size());
        return tags;
    }

    /**
     * ✅ НОВИЙ МЕТОД: Витягує локацію з картки вакансії
     */
    private String extractLocationFromCard(WebElement card) {
        try {
            // Спочатку шукаємо в meta тегах
            List<WebElement> metaElements = card.findElements(By.cssSelector("meta[itemprop='address']"));
            if (!metaElements.isEmpty()) {
                String location = metaElements.get(0).getAttribute("content");
                if (location != null && !location.trim().isEmpty()) {
                    log.debug("📍 Found location in meta: '{}'", location);
                    return location.trim();
                }
            }
            
            // Якщо meta не знайдено, шукаємо в звичайних елементах
            String location = getElementText(card, ScrapingSelectors.LOCATION[0]);
            if (location != null && !location.trim().isEmpty()) {
                log.debug("📍 Found location in element: '{}'", location);
                return location.trim();
            }
            
        } catch (Exception e) {
            log.debug("⚠️ Error extracting location: {}", e.getMessage());
        }
        
        log.debug("📍 No location found in card");
        return null;
    }

    /**
     * ✅ НОВИЙ МЕТОД: Витягує дату публікації з картки вакансії
     */
    private LocalDateTime extractPostedDateFromCard(WebElement card) {
        try {
            // ✅ ВИПРАВЛЕНО: Спочатку шукаємо в meta тегах
            List<WebElement> metaElements = card.findElements(By.cssSelector("meta[itemprop='datePosted']"));
            if (!metaElements.isEmpty()) {
                String dateStr = metaElements.get(0).getAttribute("content");
                if (dateStr != null && !dateStr.trim().isEmpty()) {
                    try {
                        // ✅ ВИПРАВЛЕНО: Правильний парсинг дати формату YYYY-MM-DD
                        if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            LocalDateTime date = LocalDateTime.parse(dateStr.trim() + "T00:00:00");
                            log.info("✅ Extracted posted date from meta tag: '{}' -> {} (Unix: {})", 
                                    dateStr, date, date.toEpochSecond(java.time.ZoneOffset.UTC));
                            return date;
                        } else {
                            // Спробуємо парсити як повну дату з часом
                            LocalDateTime date = LocalDateTime.parse(dateStr.trim());
                            log.info("✅ Extracted posted date from meta tag: '{}' -> {} (Unix: {})", 
                                    dateStr, date, date.toEpochSecond(java.time.ZoneOffset.UTC));
                            return date;
                        }
                    } catch (Exception e) {
                        log.debug("⚠️ Could not parse date from meta: '{}', error: {}", dateStr, e.getMessage());
                    }
                }
            }
            
            // ✅ ДОДАНО: Шукаємо в інших meta тегах з датою
            List<WebElement> allMetaElements = card.findElements(By.cssSelector("meta"));
            for (WebElement meta : allMetaElements) {
                String name = meta.getAttribute("name");
                String content = meta.getAttribute("content");
                if (content != null && content.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    try {
                        LocalDateTime postedDate = LocalDateTime.parse(content.trim() + "T00:00:00");
                        log.debug("📅 Found posted date in meta[name='{}']: '{}' -> {}", name, content, postedDate);
                        return postedDate;
                    } catch (Exception e) {
                        log.debug("⚠️ Could not parse date from meta[name='{}']: '{}'", name, content);
                    }
                }
            }
            
            // ✅ ДОДАНО: Шукаємо в time елементах
            List<WebElement> timeElements = card.findElements(By.cssSelector("time"));
            for (WebElement time : timeElements) {
                String datetime = time.getAttribute("datetime");
                if (datetime != null && !datetime.trim().isEmpty()) {
                    try {
                        LocalDateTime postedDate = LocalDateTime.parse(datetime.trim());
                        log.debug("📅 Found posted date in time[datetime]: '{}' -> {}", datetime, postedDate);
                        return postedDate;
                    } catch (Exception e) {
                        log.debug("⚠️ Could not parse date from time[datetime]: '{}'", datetime);
                    }
                }
            }
            
            // ✅ ДОДАНО: Шукаємо в елементах з атрибутом datetime
            List<WebElement> datetimeElements = card.findElements(By.cssSelector("[datetime]"));
            for (WebElement element : datetimeElements) {
                String datetime = element.getAttribute("datetime");
                if (datetime != null && !datetime.trim().isEmpty()) {
                    try {
                        LocalDateTime postedDate = LocalDateTime.parse(datetime.trim());
                        log.debug("📅 Found posted date in [datetime]: '{}' -> {}", datetime, postedDate);
                        return postedDate;
                    } catch (Exception e) {
                        log.debug("⚠️ Could not parse date from [datetime]: '{}'", datetime);
                    }
                }
            }
            
            // Якщо meta не знайдено, шукаємо в звичайних елементах
            String dateText = getElementText(card, ScrapingSelectors.POSTED_DATE[0]);
            if (dateText != null && !dateText.trim().isEmpty()) {
                log.debug("📅 Found posted date text: '{}'", dateText);
                // ✅ ВИПРАВЛЕНО: Покращена логіка парсингу тексту дати
                LocalDateTime parsedDate = parseDateText(dateText.trim());
                if (parsedDate != null) {
                    return parsedDate;
                }
                // Якщо не вдалося розпарсити, повертаємо поточну дату
                log.debug("⚠️ Could not parse date text: '{}', using current date", dateText);
                LocalDateTime fallbackDate = LocalDateTime.now();
                log.debug("📅 Using fallback date: {} (Unix: {})", fallbackDate, fallbackDate.toEpochSecond(java.time.ZoneOffset.UTC));
                return fallbackDate;
            }
            
            // ✅ ДОДАНО: Шукаємо в тексті всіх елементів картки
            String cardText = card.getText();
            if (cardText != null && !cardText.trim().isEmpty()) {
                // Шукаємо патерни дати в тексті
                String[] datePatterns = {
                    "\\d{4}-\\d{2}-\\d{2}", // YYYY-MM-DD
                    "\\d{1,2}/\\d{1,2}/\\d{4}", // MM/DD/YYYY
                    "\\d{1,2}\\.\\d{1,2}\\.\\d{4}", // DD.MM.YYYY
                    "\\d{1,2}\\s+\\w+\\s+\\d{4}" // DD Month YYYY
                };
                
                for (String pattern : datePatterns) {
                    if (cardText.matches(".*" + pattern + ".*")) {
                        log.debug("📅 Found date pattern '{}' in card text", pattern);
                        // Спробуємо розпарсити знайдену дату
                        LocalDateTime parsedDate = parseDateText(cardText);
                        if (parsedDate != null) {
                            return parsedDate;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.debug("⚠️ Error extracting posted date: {}", e.getMessage());
        }
        
        log.debug("📅 No posted date found in card, using current date");
        LocalDateTime currentDate = LocalDateTime.now();
        log.debug("📅 Using current date: {} (Unix: {})", currentDate, currentDate.toEpochSecond(java.time.ZoneOffset.UTC));
        return currentDate;
    }

    /**
     * ✅ НОВИЙ МЕТОД: Парсинг тексту дати (Today, Yesterday, X days ago, etc.)
     */
    private LocalDateTime parseDateText(String dateText) {
        try {
            String lowerText = dateText.toLowerCase().trim();
            LocalDateTime now = LocalDateTime.now();
            
            if (lowerText.contains("today") || lowerText.contains("сьогодні")) {
                LocalDateTime today = now;
                log.debug("📅 Parsed 'Today' as: {} (Unix: {})", today, today.toEpochSecond(java.time.ZoneOffset.UTC));
                return today;
            } else if (lowerText.contains("yesterday") || lowerText.contains("вчора")) {
                LocalDateTime yesterday = now.minusDays(1);
                log.debug("📅 Parsed 'Yesterday' as: {} (Unix: {})", yesterday, yesterday.toEpochSecond(java.time.ZoneOffset.UTC));
                return yesterday;
            } else if (lowerText.contains("days ago") || lowerText.contains("днів тому")) {
                // Шукаємо число перед "days ago"
                String numberMatch = dateText.replaceAll("(?i).*?(\\d+)\\s*(?:days?|днів?)\\s*ago.*", "$1");
                if (numberMatch.matches("\\d+")) {
                    int days = Integer.parseInt(numberMatch);
                    LocalDateTime daysAgo = now.minusDays(days);
                    log.debug("📅 Parsed '{} days ago' as: {} (Unix: {})", days, daysAgo, daysAgo.toEpochSecond(java.time.ZoneOffset.UTC));
                    return daysAgo;
                }
            } else if (lowerText.contains("hours ago") || lowerText.contains("годин тому")) {
                // Шукаємо число перед "hours ago"
                String numberMatch = dateText.replaceAll("(?i).*?(\\d+)\\s*(?:hours?|годин?)\\s*ago.*", "$1");
                if (numberMatch.matches("\\d+")) {
                    int hours = Integer.parseInt(numberMatch);
                    LocalDateTime hoursAgo = now.minusHours(hours);
                    log.debug("📅 Parsed '{} hours ago' as: {} (Unix: {})", hours, hoursAgo, hoursAgo.toEpochSecond(java.time.ZoneOffset.UTC));
                    return hoursAgo;
                }
            } else if (lowerText.contains("minutes ago") || lowerText.contains("хвилин тому")) {
                // Шукаємо число перед "minutes ago"
                String numberMatch = dateText.replaceAll("(?i).*?(\\d+)\\s*(?:minutes?|хвилин?)\\s*ago.*", "$1");
                if (numberMatch.matches("\\d+")) {
                    int minutes = Integer.parseInt(numberMatch);
                    LocalDateTime minutesAgo = now.minusMinutes(minutes);
                    log.debug("📅 Parsed '{} minutes ago' as: {} (Unix: {})", minutes, minutesAgo, minutesAgo.toEpochSecond(java.time.ZoneOffset.UTC));
                    return minutesAgo;
                }
            } else if (lowerText.contains("weeks ago") || lowerText.contains("тижнів тому")) {
                // Шукаємо число перед "weeks ago"
                String numberMatch = dateText.replaceAll("(?i).*?(\\d+)\\s*(?:weeks?|тижнів?)\\s*ago.*", "$1");
                if (numberMatch.matches("\\d+")) {
                    int weeks = Integer.parseInt(numberMatch);
                    LocalDateTime weeksAgo = now.minusWeeks(weeks);
                    log.debug("📅 Parsed '{} weeks ago' as: {} (Unix: {})", weeks, weeksAgo, weeksAgo.toEpochSecond(java.time.ZoneOffset.UTC));
                    return weeksAgo;
                }
            }
            
            // Спробуємо парсити як ISO дату
            if (dateText.matches("\\d{4}-\\d{2}-\\d{2}")) {
                LocalDateTime isoDate = LocalDateTime.parse(dateText + "T00:00:00");
                log.debug("📅 Parsed ISO date '{}' as: {} (Unix: {})", dateText, isoDate, isoDate.toEpochSecond(java.time.ZoneOffset.UTC));
                return isoDate;
            }
            
            // Спробуємо парсити як повну ISO дату з часом
            if (dateText.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) {
                LocalDateTime fullIsoDate = LocalDateTime.parse(dateText);
                log.debug("📅 Parsed full ISO date '{}' as: {} (Unix: {})", dateText, fullIsoDate, fullIsoDate.toEpochSecond(java.time.ZoneOffset.UTC));
                return fullIsoDate;
            }
            
        } catch (Exception e) {
            log.debug("⚠️ Error parsing date text '{}': {}", dateText, e.getMessage());
        }
        
        return null;
    }

    /**
     * ✅ НОВИЙ МЕТОД: Витягує URL логотипу з картки вакансії
     */
    private String extractLogoUrlFromCard(WebElement card) {
        try {
            // Шукаємо зображення з data-testid="image"
            List<WebElement> imageElements = card.findElements(By.cssSelector("img[data-testid='image']"));
            
            for (WebElement imageElement : imageElements) {
                try {
                    String src = imageElement.getAttribute("src");
                    if (src != null && !src.trim().isEmpty()) {
                        log.debug("🖼️ Found logo image with src: '{}'", src);
                        return src.trim();
                    }
                } catch (Exception e) {
                    log.debug("⚠️ Error extracting image src: {}", e.getMessage());
                }
            }
            
            // Якщо не знайдено за data-testid="image", шукаємо за іншими селекторами
            String logoUrl = getElementAttribute(card, ScrapingSelectors.ORG_LOGO[0], "src");
            if (logoUrl != null && !logoUrl.trim().isEmpty()) {
                log.debug("🖼️ Found logo using ORG_LOGO selector: '{}'", logoUrl);
                return logoUrl.trim();
            }
            
        } catch (Exception e) {
            log.debug("⚠️ Error searching for logo: {}", e.getMessage());
        }
        
        log.debug("🖼️ No logo found in card");
        return null;
    }

    /**
     * ✅ НОВИЙ МЕТОД: Витягує атрибут з елемента
     */
    private String getElementAttribute(WebElement root, String selector, String attribute) {
        try {
            log.debug("🔍 Searching for element with selector: '{}' and attribute: '{}'", selector, attribute);
            WebElement element = root.findElement(By.cssSelector(selector));
            String value = element.getAttribute(attribute);
            log.debug("🔍 Found element with selector '{}', {}: '{}'", selector, attribute, value);
            return value;
        } catch (Exception e) {
            log.debug("⚠️ Element not found with selector '{}': {}", selector, e.getMessage());
            return null;
        }
    }

    /**
     * ✅ НОВИЙ МЕТОД: Витягує назву компанії з картки вакансії
     */
    private String extractCompanyNameFromCard(WebElement card, String jobPageUrl) {
        log.debug("🔍 Starting company name extraction...");
        
        // ✅ Стратегія 1: Шукаємо в звичайних елементах
        String organizationTitle = getElementText(card, ScrapingSelectors.ORG_NAME[0]);
        log.debug("🔍 Strategy 1 - ORG_NAME selector result: '{}'", organizationTitle);
        
        if (organizationTitle != null && !organizationTitle.trim().isEmpty()) {
            return organizationTitle.trim();
        }
        
        // ✅ Стратегія 2: Шукаємо в meta тегах з itemprop="name"
        try {
            log.debug("🔍 Strategy 2 - Searching for meta[itemprop='name']...");
            List<WebElement> metaElements = card.findElements(By.cssSelector("meta[itemprop='name']"));
            log.debug("🔍 Found {} meta[itemprop='name'] elements", metaElements.size());
            
            for (WebElement metaElement : metaElements) {
                String content = metaElement.getAttribute("content");
                log.debug("🔍 Meta element content: '{}'", content);
                if (content != null && !content.trim().isEmpty()) {
                    log.info("🏢 Found company name in meta[itemprop='name']: '{}'", content.trim());
                    return content.trim();
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Strategy 2 failed: {}", e.getMessage());
        }
        
        // ✅ Стратегія 3: Шукаємо в звичайних елементах з itemprop="name"
        try {
            log.debug("🔍 Strategy 3 - Searching for [itemprop='name']...");
            List<WebElement> nameElements = card.findElements(By.cssSelector("[itemprop='name']"));
            log.debug("🔍 Found {} [itemprop='name'] elements", nameElements.size());
            
            for (WebElement nameElement : nameElements) {
                String content = nameElement.getAttribute("content");
                String text = nameElement.getText();
                log.debug("🔍 Name element - content: '{}', text: '{}'", content, text);
                
                if (content != null && !content.trim().isEmpty()) {
                    log.info("🏢 Found company name in [itemprop='name'] content: '{}'", content.trim());
                    return content.trim();
                }
                if (text != null && !text.trim().isEmpty()) {
                    log.info("🏢 Found company name in [itemprop='name'] text: '{}'", text.trim());
                    return text.trim();
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Strategy 3 failed: {}", e.getMessage());
        }
        
        // ✅ Стратегія 4: Шукаємо в батьківських елементах з itemprop="hiringOrganization"
        try {
            log.debug("🔍 Strategy 4 - Searching in parent elements for hiringOrganization...");
            WebElement parent = card.findElement(By.xpath("ancestor::div[@itemprop='hiringOrganization']"));
            if (parent != null) {
                log.debug("🔍 Found parent with hiringOrganization");
                
                // Шукаємо meta[itemprop='name'] в батьківському елементі
                List<WebElement> parentMetaElements = parent.findElements(By.cssSelector("meta[itemprop='name']"));
                log.debug("🔍 Found {} meta[itemprop='name'] in parent", parentMetaElements.size());
                
                for (WebElement metaElement : parentMetaElements) {
                    String content = metaElement.getAttribute("content");
                    log.debug("🔍 Parent meta element content: '{}'", content);
                    if (content != null && !content.trim().isEmpty()) {
                        log.info("🏢 Found company name in parent meta[itemprop='name']: '{}'", content.trim());
                        return content.trim();
                    }
                }
                
                // Шукаємо [itemprop='name'] в батьківському елементі
                List<WebElement> parentNameElements = parent.findElements(By.cssSelector("[itemprop='name']"));
                log.debug("🔍 Found {} [itemprop='name'] in parent", parentNameElements.size());
                
                for (WebElement nameElement : parentNameElements) {
                    String content = nameElement.getAttribute("content");
                    String text = nameElement.getText();
                    log.debug("🔍 Parent name element - content: '{}', text: '{}'", content, text);
                    
                    if (content != null && !content.trim().isEmpty()) {
                        log.info(" Found company name in parent [itemprop='name'] content: '{}'", content.trim());
                        return content.trim();
                    }
                    if (text != null && !text.trim().isEmpty()) {
                        log.info("🏢 Found company name in parent [itemprop='name'] text: '{}'", text.trim());
                        return text.trim();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Strategy 4 failed: {}", e.getMessage());
        }
        
        // ✅ Стратегія 5: Шукаємо за data-testid="organization-name"
        try {
            log.debug("🔍 Strategy 5 - Searching for data-testid='organization-name'...");
            WebElement orgElement = card.findElement(By.cssSelector("[data-testid='organization-name']"));
            String text = orgElement.getText();
            log.debug("🔍 data-testid='organization-name' text: '{}'", text);
            
            if (text != null && !text.trim().isEmpty()) {
                log.info("🏢 Found company name in data-testid='organization-name': '{}'", text.trim());
                return text.trim();
            }
        } catch (Exception e) {
            log.debug("⚠️ Strategy 5 failed: {}", e.getMessage());
        }
        
        // ✅ Стратегія 6: Шукаємо в посиланнях на компанії
        try {
            log.debug("🔍 Strategy 6 - Searching for company links...");
            List<WebElement> companyLinks = card.findElements(By.cssSelector("a[href*='/companies/']"));
            log.debug("🔍 Found {} company links", companyLinks.size());
            
            for (WebElement companyLink : companyLinks) {
                String text = companyLink.getText();
                log.debug("🔍 Company link text: '{}'", text);
                
                if (text != null && !text.trim().isEmpty()) {
                    log.info("🏢 Found company name in company link: '{}'", text.trim());
                    return text.trim();
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Strategy 6 failed: {}", e.getMessage());
        }
        
        // ✅ Стратегія 7: Fallback - витягуємо з URL
        try {
            log.debug("🔍 Strategy 7 - Extracting from URL...");
            // URL: https://jobs.techstars.com/companies/artera-2-45603da9-8558-41e0-8432-f493987a2c76
            String[] urlParts = jobPageUrl.split("/companies/");
            if (urlParts.length > 1) {
                String companyPart = urlParts[1].split("/")[0]; // artera-2-45603da9-8558-41e0-8432-f493987a2c76
                log.debug("🔍 Company part from URL: '{}'", companyPart);
                
                // Прибираємо UUID та замінюємо дефіси на пробіли
                String companyName = companyPart.replaceAll("-\\d{1,2}-[a-f0-9-]+$", ""); // artera-2
                companyName = companyName.replaceAll("-\\d+$", ""); // artera
                companyName = companyName.replace("-", " "); // artera -> artera
                
                // Капіталізуємо першу літеру
                if (!companyName.isEmpty()) {
                    String result = companyName.substring(0, 1).toUpperCase() + companyName.substring(1);
                    log.info("🏢 Extracted company name from URL: '{}'", result);
                    return result;
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Strategy 7 failed: {}", e.getMessage());
        }
        
        log.warn("⚠️ All strategies failed to find company name");
        return "Unknown Company";
    }

    /**
     * ✅ НОВИЙ МЕТОД: Витягує назву позиції з картки вакансії
     */
    private String extractTitleFromCard(WebElement card) {
        log.debug("🔍 Starting title extraction...");
        
        // ✅ Стратегія 1: Шукаємо за звичайними селекторами
        String title = getElementText(card, ScrapingSelectors.JOB_TITLE[0]);
        log.debug("🔍 Strategy 1 - JOB_TITLE selector result: '{}'", title);
        
        if (title != null && !title.trim().isEmpty()) {
            log.info("💼 Found title using JOB_TITLE selector: '{}'", title.trim());
            return title.trim();
        }
        
        // ✅ Стратегія 2: Шукаємо за data-testid="job-title"
        try {
            log.debug("🔍 Strategy 2 - Searching for data-testid='job-title'...");
            WebElement titleElement = card.findElement(By.cssSelector("[data-testid='job-title']"));
            String text = titleElement.getText();
            log.debug("🔍 data-testid='job-title' text: '{}'", text);
            
            if (text != null && !text.trim().isEmpty()) {
                log.info("💼 Found title using data-testid='job-title': '{}'", text.trim());
                return text.trim();
            }
        } catch (Exception e) {
            log.debug("⚠️ Strategy 2 failed: {}", e.getMessage());
        }
        
        // ✅ Стратегія 3: Шукаємо за itemprop="title"
        try {
            log.debug("🔍 Strategy 3 - Searching for [itemprop='title']...");
            List<WebElement> titleElements = card.findElements(By.cssSelector("[itemprop='title']"));
            log.debug("🔍 Found {} [itemprop='title'] elements", titleElements.size());
            
            for (WebElement titleElement : titleElements) {
                String content = titleElement.getAttribute("content");
                String text = titleElement.getText();
                log.debug("🔍 Title element - content: '{}', text: '{}'", content, text);
                
                if (content != null && !content.trim().isEmpty()) {
                    log.info("💼 Found title using [itemprop='title'] content: '{}'", content.trim());
                    return content.trim();
                }
                if (text != null && !text.trim().isEmpty()) {
                    log.info("💼 Found title using [itemprop='title'] text: '{}'", text.trim());
                    return text.trim();
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Strategy 3 failed: {}", e.getMessage());
        }
        
        // ✅ Стратегія 4: Шукаємо в заголовках (h1, h2, h3)
        try {
            log.debug("🔍 Strategy 4 - Searching for headings...");
            List<WebElement> headings = card.findElements(By.cssSelector("h1, h2, h3, h4, h5, h6"));
            log.debug("🔍 Found {} heading elements", headings.size());
            
            for (WebElement heading : headings) {
                String text = heading.getText();
                log.debug("🔍 Heading text: '{}'", text);
                
                if (text != null && !text.trim().isEmpty() && text.length() > 3) {
                    log.info("💼 Found title in heading: '{}'", text.trim());
                    return text.trim();
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Strategy 4 failed: {}", e.getMessage());
        }
        
        // ✅ Стратегія 5: Шукаємо в посиланнях з текстом що може бути назвою позиції
        try {
            log.debug("🔍 Strategy 5 - Searching for links that might contain title...");
            List<WebElement> links = card.findElements(By.cssSelector("a[href]"));
            log.debug("🔍 Found {} links", links.size());
            
            for (WebElement link : links) {
                String text = link.getText();
                String href = link.getAttribute("href");
                log.debug("🔍 Link - text: '{}', href: '{}'", text, href);
                
                // Перевіряємо чи посилання містить /jobs/ (може бути назва позиції)
                if (text != null && !text.trim().isEmpty() && 
                    href != null && href.contains("/jobs/") && 
                    text.length() > 3 && text.length() < 100) {
                    log.info("💼 Found title in job link: '{}'", text.trim());
                    return text.trim();
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Strategy 5 failed: {}", e.getMessage());
        }
        
        log.warn("⚠️ All strategies failed to find title");
        return "Unknown Position";
    }

    /**
     * ✅ НОВИЙ МЕТОД: Фільтрує елементи, щоб знайти тільки реальні картки вакансій
     */
    private List<WebElement> filterValidJobCards(List<WebElement> elements) {
        List<WebElement> validCards = new ArrayList<>();
        
        for (WebElement element : elements) {
            try {
                // Перевіряємо, чи це реальна картка вакансії
                if (isValidJobCard(element)) {
                    validCards.add(element);
                }
            } catch (Exception e) {
                log.debug("⚠️ Error validating element: {}", e.getMessage());
            }
        }
        
        return validCards;
    }
    
    /**
     * ✅ НОВИЙ МЕТОД: Перевіряє, чи є елемент реальною карткою вакансії
     */
    private boolean isValidJobCard(WebElement element) {
        try {
            String text = element.getText().toLowerCase();
            
            // ✅ Фільтруємо навігаційні елементи та фільтри
            String[] navigationKeywords = {
                "search", "explore", "join", "my", "job alerts", "on-site", "remote", 
                "job function", "seniority", "salary", "industry", "company stage", 
                "more filters", "create job alert", "powered by", "showing", "jobs",
                "companies", "talent network", "claim your profile"
            };
            
            for (String keyword : navigationKeywords) {
                if (text.contains(keyword.toLowerCase())) {
                    return false; // Це навігаційний елемент
                }
            }
            
            // ✅ Перевіряємо, чи містить елемент посилання на вакансію
            try {
                List<WebElement> links = element.findElements(By.cssSelector("a[href*='/jobs/'], a[href*='/companies/']"));
                if (links.isEmpty()) {
                    return false; // Немає посилань на вакансії
                }
            } catch (Exception e) {
                return false; // Помилка при пошуку посилань
            }
            
            // ✅ Перевіряємо, чи містить елемент інформацію про вакансію
            String[] jobKeywords = {
                "engineer", "designer", "manager", "developer", "analyst", "specialist",
                "coordinator", "director", "lead", "senior", "junior", "full-time",
                "part-time", "remote", "onsite", "hybrid", "salary", "experience"
            };
            
            boolean hasJobInfo = false;
            for (String keyword : jobKeywords) {
                if (text.contains(keyword.toLowerCase())) {
                    hasJobInfo = true;
                    break;
                }
            }
            
            if (!hasJobInfo) {
                return false; // Немає інформації про вакансію
            }
            
            // ✅ Перевіряємо розмір тексту (картка вакансії має бути достатньо великою)
            if (text.length() < 50) {
                return false; // Занадто короткий текст
            }
            
            return true; // Це схоже на реальну картку вакансії
            
        } catch (Exception e) {
            log.debug("⚠️ Error checking if element is valid job card: {}", e.getMessage());
            return false;
        }
    }

    /**
     * ✅ НОВИЙ МЕТОД: Екстракція заголовка з детальної сторінки
     */
    private String extractTitleFromDetailPage(WebDriver driver) {
        try {
            for (String selector : ScrapingSelectors.JOB_DETAIL_PAGE) {
                if (selector.contains("itemprop='title'")) {
                    List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                    if (!elements.isEmpty()) {
                        String title = elements.get(0).getText().trim();
                        log.info("✅ Extracted title from detail page: {}", title);
                        return title;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Error extracting title from detail page: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * ✅ НОВИЙ МЕТОД: Екстракція назви компанії з детальної сторінки
     */
    private String extractCompanyNameFromDetailPage(WebDriver driver) {
        try {
            for (String selector : ScrapingSelectors.JOB_DETAIL_PAGE) {
                if (selector.contains("itemprop='name'")) {
                    List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                    if (!elements.isEmpty()) {
                        String companyName = elements.get(0).getText().trim();
                        log.info("✅ Extracted company name from detail page: {}", companyName);
                        return companyName;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Error extracting company name from detail page: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * ✅ НОВИЙ МЕТОД: Екстракція тегів з детальної сторінки
     */
    private List<String> extractTagsFromDetailPage(WebDriver driver) {
        List<String> tags = new ArrayList<>();
        try {
            for (String selector : ScrapingSelectors.JOB_DETAIL_PAGE) {
                if (selector.contains("data-testid=tag")) {
                    List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                    for (WebElement element : elements) {
                        String tag = element.getText().trim();
                        if (!tag.isEmpty()) {
                            tags.add(tag);
                        }
                    }
                    if (!tags.isEmpty()) {
                        log.info("✅ Extracted {} tags from detail page: {}", tags.size(), tags);
                        return tags;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Error extracting tags from detail page: {}", e.getMessage());
        }
        return tags;
    }
    
    /**
     * ✅ НОВИЙ МЕТОД: Екстракція локації з детальної сторінки
     */
    private String extractLocationFromDetailPage(WebDriver driver) {
        try {
            // Спочатку шукаємо в meta тегах
            List<WebElement> metaElements = driver.findElements(By.cssSelector("meta[itemprop='address']"));
            if (!metaElements.isEmpty()) {
                String location = metaElements.get(0).getAttribute("content");
                if (location != null && !location.trim().isEmpty()) {
                    log.info("✅ Extracted location from meta tag: {}", location);
                    return location.trim();
                }
            }
            
            // Потім шукаємо в div елементах
            for (String selector : ScrapingSelectors.JOB_DETAIL_PAGE) {
                if (selector.contains("sc-beqWaB")) {
                    List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                    for (WebElement element : elements) {
                        String text = element.getText().trim();
                        if (text.contains(",") && (text.contains("USA") || text.contains("Remote") || text.contains("India"))) {
                            log.info("✅ Extracted location from div: {}", text);
                            return text;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Error extracting location from detail page: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * ✅ НОВИЙ МЕТОД: Екстракція дати публікації з детальної сторінки
     */
    private LocalDateTime extractPostedDateFromDetailPage(WebDriver driver) {
        try {
            // Спочатку шукаємо в meta тегах
            List<WebElement> metaElements = driver.findElements(By.cssSelector("meta[itemprop='datePosted']"));
            if (!metaElements.isEmpty()) {
                String dateStr = metaElements.get(0).getAttribute("content");
                if (dateStr != null && !dateStr.trim().isEmpty()) {
                    try {
                        // ✅ ВИПРАВЛЕНО: Правильний парсинг дати формату YYYY-MM-DD
                        if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            LocalDateTime date = LocalDateTime.parse(dateStr.trim() + "T00:00:00");
                            log.info("✅ Extracted posted date from meta tag: '{}' -> {} (Unix: {})", 
                                    dateStr, date, date.toEpochSecond(java.time.ZoneOffset.UTC));
                            return date;
                        } else {
                            // Спробуємо парсити як повну дату з часом
                            LocalDateTime date = LocalDateTime.parse(dateStr.trim());
                            log.info("✅ Extracted posted date from meta tag: '{}' -> {} (Unix: {})", 
                                    dateStr, date, date.toEpochSecond(java.time.ZoneOffset.UTC));
                            return date;
                        }
                    } catch (Exception e) {
                        log.warn("⚠️ Could not parse date from meta tag: '{}', error: {}", dateStr, e.getMessage());
                    }
                }
            }
            
            // Потім шукаємо в div елементах
            for (String selector : ScrapingSelectors.JOB_DETAIL_PAGE) {
                if (selector.contains("enQFes")) {
                    List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                    for (WebElement element : elements) {
                        String text = element.getText().trim();
                        if (text.contains("Posted") || text.contains("Today") || text.matches(".*\\d{4}-\\d{2}-\\d{2}.*")) {
                            // ✅ ВИПРАВЛЕНО: Використовуємо покращений парсинг тексту дати
                            LocalDateTime parsedDate = parseDateText(text);
                            if (parsedDate != null) {
                                log.info("✅ Extracted posted date from div: '{}' -> {}", text, parsedDate);
                                return parsedDate;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Error extracting posted date from detail page: {}", e.getMessage());
        }
        
        // Повертаємо поточну дату як запасний варіант
        log.info("⚠️ Using current date as fallback for posted date");
        LocalDateTime fallbackDate = LocalDateTime.now();
        log.info("📅 Using fallback date: {} (Unix: {})", fallbackDate, fallbackDate.toEpochSecond(java.time.ZoneOffset.UTC));
        return fallbackDate;
    }
    
    /**
     * ✅ НОВИЙ МЕТОД: Пошук карток вакансій на сторінці компанії
     */
    private List<WebElement> findJobCardsOnCompanyPage(WebDriver driver) {
        List<WebElement> jobCards = new ArrayList<>();
        
        try {
            for (String selector : ScrapingSelectors.COMPANY_PAGE_JOBS) {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                if (!elements.isEmpty()) {
                    jobCards.addAll(elements);
                    log.info("✅ Found {} job cards with selector: {}", elements.size(), selector);
                }
            }
            
            // Фільтруємо неправильні елементи
            jobCards = filterValidJobCards(jobCards);
            
        } catch (Exception e) {
            log.warn("⚠️ Error finding job cards on company page: {}", e.getMessage());
        }
        
        return jobCards;
    }

    /**
     * ✅ НОВИЙ МЕТОД: Витягує опис вакансії з картки вакансії
     */
    private String extractDescriptionFromCard(WebElement card) {
        try {
            // ✅ Шукаємо опис за селекторами з ScrapingSelectors (найточніші)
            for (String selector : ScrapingSelectors.DESCRIPTION) {
                try {
                    List<WebElement> elements = card.findElements(By.cssSelector(selector));
                    for (WebElement element : elements) {
                        String text = element.getText();
                        String content = element.getAttribute("content");
                        
                        // Перевіряємо content атрибут
                        if (content != null && !content.trim().isEmpty() && content.length() < 500) {
                            // ✅ ВИПРАВЛЕНО: Перевіряємо, чи це не назва вакансії
                            if (!content.contains(" at ") && !content.contains(" - ") && 
                                !content.contains("UX Designer") && !content.contains("Software Engineer")) {
                                log.debug("📝 Found description using selector '{}' content: '{}'", selector, content);
                                return content.trim();
                            }
                        }
                        
                        // Перевіряємо текст елемента
                        if (text != null && !text.trim().isEmpty() && text.length() < 500) {
                            // ✅ ВИПРАВЛЕНО: Перевіряємо, чи це не назва вакансії
                            if (!text.contains(" at ") && !text.contains(" - ") && 
                                !text.contains("UX Designer") && !text.contains("Software Engineer")) {
                                log.debug("📝 Found description using selector '{}' text: '{}'", selector, text);
                                return text.trim();
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("⚠️ Selector '{}' failed: {}", selector, e.getMessage());
                }
            }
            
            // ✅ Шукаємо короткий опис в meta тегах
            List<WebElement> metaElements = card.findElements(By.cssSelector("meta[itemprop='description']"));
            if (!metaElements.isEmpty()) {
                String description = metaElements.get(0).getAttribute("content");
                if (description != null && !description.trim().isEmpty()) {
                    // ✅ ВИПРАВЛЕНО: Перевіряємо, чи це не назва вакансії
                    if (!description.contains(" at ") && !description.contains(" - ") && description.length() < 200) {
                        log.debug("📝 Found description in meta[itemprop='description']: '{}'", description);
                        return description.trim();
                    }
                }
            }
            
            // ✅ Шукаємо в div з класом job-info (найбільш ймовірне місце для опису)
            List<WebElement> jobInfoElements = card.findElements(By.cssSelector("div[class*='job-info']"));
            for (WebElement jobInfo : jobInfoElements) {
                // Шукаємо в дочірніх елементах з описом
                List<WebElement> descElements = jobInfo.findElements(By.cssSelector("[data-testid*='description'], [data-testid*='about'], [data-testid*='summary']"));
                for (WebElement descElement : descElements) {
                    String text = descElement.getText();
                    if (text != null && !text.trim().isEmpty() && text.length() < 500) {
                        // ✅ ВИПРАВЛЕНО: Перевіряємо, чи це не назва вакансії
                        if (!text.contains(" at ") && !text.contains(" - ") && 
                            !text.contains("UX Designer") && !text.contains("Software Engineer")) {
                            log.debug("📝 Found description in job-info element: '{}'", text);
                            return text.trim();
                        }
                    }
                }
            }
            
            // ✅ ДОДАНО: Шукаємо в div з класом sc-beqWaB sc-gueYoa lpllVF MYFxR (на основі наданої HTML структури)
            List<WebElement> scElements = card.findElements(By.cssSelector("div.sc-beqWaB.sc-gueYoa.lpllVF.MYFxR"));
            for (WebElement scElement : scElements) {
                // Шукаємо в дочірніх елементах з описом
                List<WebElement> descElements = scElement.findElements(By.cssSelector("[data-testid*='description'], [data-testid*='about'], [data-testid*='summary']"));
                for (WebElement descElement : descElements) {
                    String text = descElement.getText();
                    if (text != null && !text.trim().isEmpty() && text.length() < 500) {
                        // ✅ ВИПРАВЛЕНО: Перевіряємо, чи це не назва вакансії
                        if (!text.contains(" at ") && !text.contains(" - ") && 
                            !text.contains("UX Designer") && !text.contains("Software Engineer")) {
                            log.debug("📝 Found description in sc-beqWaB element: '{}'", text);
                            return text.trim();
                        }
                    }
                }
                
                // Шукаємо в тексті самого елемента, якщо він містить опис
                String scText = scElement.getText();
                if (scText != null && !scText.trim().isEmpty() && scText.length() > 50 && scText.length() < 500) {
                    // ✅ ВИПРАВЛЕНО: Перевіряємо, чи це не назва вакансії
                    if (!scText.contains(" at ") && !scText.contains(" - ") && 
                        !scText.contains("UX Designer") && !scText.contains("Software Engineer") &&
                        !scText.contains("Chief of Staff")) {
                        log.debug("📝 Found description in sc-beqWaB text: '{}'", scText);
                        return scText.trim();
                    }
                }
            }
            
            // ✅ Шукаємо короткий опис в тегах (найбезпечніший fallback варіант)
            List<WebElement> tagElements = card.findElements(By.cssSelector("[data-testid='tag']"));
            if (!tagElements.isEmpty()) {
                List<String> tags = new ArrayList<>();
                for (WebElement tag : tagElements) {
                    String tagText = tag.getText();
                    if (tagText != null && !tagText.trim().isEmpty()) {
                        tags.add(tagText.trim());
                    }
                }
                if (!tags.isEmpty()) {
                    String tagsDescription = String.join(", ", tags);
                    log.debug("📝 Using tags as fallback description: '{}'", tagsDescription);
                    return tagsDescription;
                }
            }
            
        } catch (Exception e) {
            log.debug("⚠️ Error extracting description from card: {}", e.getMessage());
        }
        
        log.debug("📝 No valid description found in card, will try to get from detail page later");
        return null;
    }

    /**
     * ✅ НОВИЙ МЕТОД: Витягує опис вакансії з детальної сторінки
     */
    private String extractDescriptionFromDetailPage(WebDriver driver) {
        try {
            // ✅ Шукаємо опис в div з data-testid="careerPage"
            List<WebElement> careerPageElements = driver.findElements(By.cssSelector("div[data-testid='careerPage']"));
            if (!careerPageElements.isEmpty()) {
                WebElement careerPage = careerPageElements.get(0);
                String description = careerPage.getAttribute("innerHTML");
                if (description != null && !description.trim().isEmpty()) {
                    log.info("📝 Found description in careerPage div, length: {} characters", description.length());
                    return description.trim();
                }
            }
            
            // ✅ Шукаємо опис в div з класом sc-beqWaB fmCCHr
            List<WebElement> descriptionElements = driver.findElements(By.cssSelector("div.sc-beqWaB.fmCCHr"));
            if (!descriptionElements.isEmpty()) {
                WebElement descriptionDiv = descriptionElements.get(0);
                String description = descriptionDiv.getAttribute("innerHTML");
                if (description != null && !description.trim().isEmpty()) {
                    log.info("📝 Found description in sc-beqWaB.fmCCHr div, length: {} characters", description.length());
                    return description.trim();
                }
            }
            
            // ✅ Шукаємо опис за селекторами з ScrapingSelectors
            for (String selector : ScrapingSelectors.DESCRIPTION) {
                try {
                    List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                    for (WebElement element : elements) {
                        String elementHtml = element.getAttribute("innerHTML");
                        if (elementHtml != null && elementHtml.length() > 200) { // HTML має бути довгим
                            log.info("📝 Found description using selector '{}', length: {} characters", selector, elementHtml.length());
                            return elementHtml.trim();
                        }
                    }
                } catch (Exception e) {
                    log.debug("⚠️ Error with selector '{}': {}", selector, e.getMessage());
                }
            }
            
            // ✅ Шукаємо опис в всіх div елементах з класом sc-beqWaB
            List<WebElement> allScElements = driver.findElements(By.cssSelector("div[class*='sc-beqWaB']"));
            for (WebElement element : allScElements) {
                String elementText = element.getText();
                if (elementText != null && elementText.length() > 100) { // Шукаємо довгий текст
                    String elementHtml = element.getAttribute("innerHTML");
                    if (elementHtml != null && elementHtml.length() > 200) { // HTML має бути ще довшим
                        log.info("📝 Found potential description in sc-beqWaB div, length: {} characters", elementHtml.length());
                        return elementHtml.trim();
                    }
                }
            }
            
        } catch (Exception e) {
            log.warn("⚠️ Error extracting description from detail page: {}", e.getMessage());
        }
        
        log.info("📝 No description found on detail page");
        return null;
    }
}
