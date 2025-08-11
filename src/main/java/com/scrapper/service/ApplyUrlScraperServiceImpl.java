package com.scrapper.service;

import com.scrapper.model.Job;
import com.scrapper.service.criteriaServices.DescriptionIngestService;
import com.scrapper.util.ScrapingSelectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.scrapper.service.criteriaServices.DataExtractionService;
import com.scrapper.service.criteriaServices.DateParsingService;
import com.scrapper.service.webdriver.WebDriverService;


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

    private final DescriptionIngestService descriptionIngestService;
    private final JobCreationService jobCreationService;
    private final DateParsingService dateParsingService;
    private final DataExtractionService dataExtractionService;
    private final WebDriverService webDriverService;
    private final PageInteractionService pageInteractionService;

    /**
     * ✅ ОНОВЛЕНО: Використовуємо новий WebDriverService
     */
    private WebDriver initializeWebDriver() {
        log.info("🔧 Initializing Chrome WebDriver using WebDriverService...");
        return webDriverService.createWebDriver();
    }

    @Override
    public List<String> fetchApplyUrls(List<String> jobFunctions) {
        Objects.requireNonNull(jobFunctions, "jobFunctions cannot be null");
        
        log.info("🚀 Starting Selenium scraping with NEW LOGIC: jobFunctions={}", 
                jobFunctions);

        WebDriver driver = null;
        try {
            driver = initializeWebDriver();
            log.info("📍 Navigating to base URL: {}", baseUrl);
            driver.get(baseUrl);
            log.info("⏳ Quick page load...");
            pageInteractionService.sleep(3000);
            String pageTitle = driver.getTitle();
            String currentUrl = driver.getCurrentUrl();
            log.info("📄 Page loaded - Title: '{}', URL: '{}'", pageTitle, currentUrl);
            int initialElements = driver.findElements(By.cssSelector("*")).size();
            log.info("🔍 Total elements on page: {}", initialElements);
            if (initialElements < 50) {
                log.warn("⚠️ Page seems to be empty! Only {} elements found", initialElements);
                pageInteractionService.sleep(2000);
            }
            
            // ✅ ОНОВЛЕНО: Використовуємо нову гібридну логіку з правильним порядком
            log.info("🔍 Applying NEW HYBRID LOGIC: 1) job functions → 2) Load More (ОДИН раз) → 3) нескінченна прокрутка → 4) URL → 5) префікс компанії → 6) збір тегів");
            List<Job> jobs = scrapeAllJobsWithImprovedLogic(driver, jobFunctions);
            
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
                webDriverService.closeWebDriver(driver);
            }
        }
    }
    
    @Override
    public List<Job> scrapeAndCreateJobs(List<String> jobFunctions) {
        log.info("🚀 Starting job scraping and creation with NEW LOGIC for job functions: {}", 
                jobFunctions);
        
        WebDriver driver = null;
        try {
            driver = initializeWebDriver();
            log.info("🌐 WebDriver initialized successfully");
            
            driver.get(baseUrl);
            log.info("🌐 Navigated to: {}", baseUrl);
            
            // ✅ ОПТИМІЗОВАНО: Швидке очікування завантаження сторінки
            log.info("🔍 Quick page load check...");
            
            // Чекаємо тільки 5 секунд на завантаження
            pageInteractionService.sleep(5000);
            
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
            List<Job> jobs = scrapeJobsBasedOnPageType(driver, jobFunctions);
            
            log.info("🎯 Job scraping completed with NEW LOGIC. Created {} Job objects with real data", jobs.size());
            return jobs;
            
        } catch (Exception e) {
            log.error("❌ Error during job scraping: {}", e.getMessage(), e);
            return new ArrayList<>();
        } finally {
            if (driver != null) {
                webDriverService.closeWebDriver(driver);
            }
        }
    }

    private void clickLoadMoreButton(WebDriver driver) {
        // ✅ ВИКОРИСТОВУЄМО PageInteractionService
        pageInteractionService.clickLoadMoreButton(driver);
    }

    private void scrollToBottom(WebDriver driver) {
        // ✅ ВИКОРИСТОВУЄМО PageInteractionService
        pageInteractionService.scrollToBottom(driver);
    }

    /**
     * ✅ НОВИЙ МЕТОД: Спробуємо альтернативні селектори
     */
    private void tryAlternativeSelectors(WebDriver driver) {
        // ✅ ВИКОРИСТОВУЄМО PageInteractionService
        pageInteractionService.tryAlternativeSelectors(driver);
        
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
        // ✅ ВИКОРИСТОВУЄМО PageInteractionService
        pageInteractionService.loadContentWithHybridApproach(driver);
    }
    
    /**
     * ✅ НОВИЙ МЕТОД: Натискання кнопки Load More ОДИН раз
     */
    private boolean clickLoadMoreButtonOnce(WebDriver driver) {
        // ✅ ВИКОРИСТОВУЄМО PageInteractionService
        pageInteractionService.clickLoadMoreButton(driver);
        return true; // Припускаємо, що кнопка була знайдена та натиснута
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
    private List<Job> scrapeAllJobsWithImprovedLogic(WebDriver driver, List<String> jobFunctions) {
        log.info("🔍 Job functions to filter by: {} (type: {})", jobFunctions,
                jobFunctions != null ? jobFunctions.getClass().getSimpleName() : "null");
        
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
        printUpdatedFinalReport(jobCards.size(), passedFunctionFilter, foundUrls, 
                               jobs.size(), savedWithCompanyPrefix, savedWithoutCompanyPrefix, jobFunctions);
        return jobs;
    }

    /**
     * ✅ ОНОВЛЕНИЙ МЕТОД: Визначає тип сторінки та застосовує відповідну логіку скрапінгу
     * Всі методи тепер використовують нову гібридну логіку:
     * 1) job functions → 2) Load More (ОДИН раз) → 3) нескінченна прокрутка → 4) URL → 5) префікс компанії → 6) збір тегів
     */
        private List<Job> scrapeJobsBasedOnPageType(WebDriver driver, List<String> jobFunctions) {
        String currentUrl = driver.getCurrentUrl();
        log.info("🔍 Current URL: {}", currentUrl);

        if (currentUrl.contains("/companies/") && currentUrl.contains("/jobs/")) {
            // Детальна сторінка вакансії
            log.info("🎯 Detected job detail page, applying new filtering logic...");
            return scrapeSingleJobFromDetailPage(driver, jobFunctions);

        } else if (currentUrl.contains("/companies/")) {
            // Сторінка компанії зі списком вакансій
            log.info("🏢 Detected company page, applying new filtering logic...");
            return scrapeJobsFromCompanyPage(driver, jobFunctions);

        } else if (currentUrl.contains("/jobs")) {
            // Головна сторінка зі списком вакансій
            log.info("📋 Detected main jobs page, applying new filtering logic...");
            return scrapeJobsFromMainPage(driver, jobFunctions);

        } else {
            // Невідома сторінка
            log.warn("⚠️ Unknown page type, trying default scraping with new logic...");
            return scrapeJobsFromMainPage(driver, jobFunctions);
        }
    }
    
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
    private List<Job> scrapeJobsFromCompanyPage(WebDriver driver, List<String> jobFunctions) {
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
    private List<Job> scrapeJobsFromMainPage(WebDriver driver, List<String> jobFunctions) {
        // Використовуємо оновлену логіку з новим порядком фільтрації
        return scrapeAllJobsWithImprovedLogic(driver, jobFunctions);
    }

    /**
     * ✅ НОВИЙ МЕТОД: Знаходимо картки вакансій кількома стратегіями
     */
    private List<WebElement> findJobCardsWithMultipleStrategies(WebDriver driver) {
        // ✅ ВИКОРИСТОВУЄМО PageInteractionService
        return pageInteractionService.findJobCardsWithMultipleStrategies(driver);
    }

    /**
     * ✅ НОВА, НАДІЙНА ВЕРСІЯ МЕТОДУ
     * Шукає пряме посилання на вакансію в картці, використовуючи кілька стратегій
     */
    private String findDirectJobUrl(WebElement jobCard) {
        // ✅ ВИКОРИСТОВУЄМО PageInteractionService
        return pageInteractionService.findDirectJobUrl(jobCard);
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
            
            // ✅ ВИПРАВЛЕНО: Використовуємо DataExtractionService для витягування всіх даних
            String organizationTitle = dataExtractionService.extractCompanyName(card);
            String positionName = dataExtractionService.extractTitle(card);
            List<String> tags = dataExtractionService.extractTags(card);
            String location = dataExtractionService.extractLocation(card);
            LocalDateTime postedDate = dataExtractionService.extractPostedDate(card);
            String logoUrl = dataExtractionService.extractLogoUrl(card);
            String description = dataExtractionService.extractDescription(card);
            
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


    private void printUpdatedFinalReport(int totalCards, int passedFunctionFilter,
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
        // ✅ ВИКОРИСТОВУЄМО PageInteractionService
        pageInteractionService.sleep(milliseconds);
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
            // ✅ ВИПРАВЛЕНО: Спочатку шукаємо в meta тегах
            List<WebElement> metaElements = driver.findElements(By.cssSelector("meta[itemprop='datePosted']"));
            if (!metaElements.isEmpty()) {
                String dateStr = metaElements.get(0).getAttribute("content");
                if (dateStr != null && !dateStr.trim().isEmpty()) {
                    // ✅ ВИПРАВЛЕНО: Використовуємо DateParsingService для парсингу дати з meta тегу
                    LocalDateTime date = dateParsingService.parseMetaDate(dateStr);
                    if (date != null) {
                        log.info("✅ Extracted posted date from meta tag: '{}' -> {} (Unix: {})", 
                                dateStr, date, date.toEpochSecond(java.time.ZoneOffset.UTC));
                        return date;
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
        // ✅ ВИКОРИСТОВУЄМО PageInteractionService
        return pageInteractionService.findJobCardsOnCompanyPage(driver);
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
