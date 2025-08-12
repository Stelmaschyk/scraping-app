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


/** ЛОГІКА ФІЛЬТРАЦІЇ:
 * 1. Спочатку вибирається job Function (фільтрація за функціями)
 * 2. Потім натискається кнопка Load More ОДИН раз (якщо вона є)
 * 3. Далі запускається цикл нескінченної прокрутки з автоматичним завантаженням
 * 4. Виходимо з циклу тільки коли кількість вакансій перестає зростати
 * 5. І тільки потім зчитування URL та перевірка префіксу
 * 6. Якщо URL містить https://jobs.techstars.com/companies/ то вакансія зберігається
 * 8. Теги збираються для всіх збережених вакансій
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
     * <p>
     * Якщо URL вакансії містить цей префікс, то вакансія зберігається
     * БЕЗ перевірки тегів (тільки фільтрація за функціями).
     * <p>
     * Теги збираються для всіх збережених вакансій.
     * <p>
     * Це дозволяє зберігати всі вакансії компаній Techstars,
     * незалежно від тегів, після застосування нової гібридної логіки
     * завантаження (Load More + нескінченна прокрутка).
     */
    private static final String REQUIRED_PREFIX = "https://jobs.techstars.com/companies/";

    private final DescriptionIngestService descriptionIngestService;
    private final JobCreationService jobCreationService;
    private final DateParsingService dateParsingService;
    private final DataExtractionService dataExtractionService;
    private final WebDriverService webDriverService;
    private final PageInteractionService pageInteractionService;

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

            // ✅ Використовуємо логіку з правильним порядком
            log.info("🔍 Applying NEW HYBRID LOGIC: 1) job functions → 2) Load More (ОДИН раз) → "
                + "3) нескінченна прокрутка → 4) URL → 5) префікс компанії → 6) збір тегів");
            List<Job> jobs = scrapeAllJobsWithImprovedLogic(driver, jobFunctions);

            log.info("✅ Scraping completed with NEW LOGIC. Found {} jobs matching criteria.",
                jobs.size());

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
            log.info("🌐 Moving to: {}", baseUrl);

            log.info("🔍 Waiting for load page...");
            pageInteractionService.sleep(5000);

            log.info("🔍 Quick job cards searching...");
            boolean pageLoaded = false;

            // Спробуємо тільки основні селектори з коротким таймаутом
            for (String selector : ScrapingSelectors.JOB_CARD) {
                try {
                    // Зменшуємо таймаут до 3 секунд
                    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(selector)));

                    int elementCount = driver.findElements(By.cssSelector(selector)).size();
                    if (elementCount > 0) {
                        log.info("✅ Found {} job cards with selector: '{}'", elementCount,
                            selector);
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
            log.info("🔍 Applying NEW HYBRID LOGIC: 1) job functions → 2) Load More (ОДИН раз) → "
                + "3) нескінченна прокрутка → 4) URL → 5) префікс компанії → 6) збір тегів");
            List<Job> jobs = scrapeJobsBasedOnPageType(driver, jobFunctions);

            log.info("🎯 Job scraping completed with NEW LOGIC. Created {} Job objects with real "
                + "data", jobs.size());
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
        log.info("🔍 ApplyUrlScraperServiceImpl: Викликаємо clickLoadMoreButton...");
        pageInteractionService.clickLoadMoreButton(driver);
        log.info("🔍 ApplyUrlScraperServiceImpl: clickLoadMoreButton завершено");
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
     * <p>
     * НОВА ЛОГІКА:
     * 1. Спочатку натискаємо кнопку Load More ОДИН раз (якщо вона є)
     * 2. Потім запускаємо цикл нескінченної прокрутки
     * 3. Виходимо з циклу тільки коли кількість вакансій перестає зростати
     * <p>
     * Це дозволяє адаптуватися до гібридного підходу сайту:
     * - Спочатку кнопка Load More
     * - Потім автоматичне завантаження при прокрутці
     */
    private void scrollToLoadMore(WebDriver driver, List<String> jobFunctions) {
        // ✅ ВИКОРИСТОВУЄМО PageInteractionService
        log.info("🔍 ApplyUrlScraperServiceImpl: Викликаємо scrollToLoadMore...");
        pageInteractionService.loadContentWithHybridApproach(driver, jobFunctions);
        log.info("🔍 ApplyUrlScraperServiceImpl: scrollToLoadMore завершено");
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
     * <p>
     * НОВА ЛОГІКА:
     * 1. Спочатку вибирається job Function (фільтрація за функціями)
     * 2. Потім натискається кнопка Load More ОДИН раз (якщо вона є)
     * 3. Далі запускається цикл нескінченної прокрутки з автоматичним завантаженням
     * 4. Виходимо з циклу тільки коли кількість вакансій перестає зростати
     * 5. І тільки потім зчитування URL та перевірка префіксу
     * 6. Якщо URL містить https://jobs.techstars.com/companies/ - зберігаємо БЕЗ перевірки тегів
     * <p>
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
        
        // ✅ КРОК 1: Послідовно застосовуємо ВСІ фільтри
        log.info("🔍 КРОК 1: Застосування всіх job function фільтрів...");
        boolean anyFilterApplied = false;
        
        if (jobFunctions != null && !jobFunctions.isEmpty()) {
            log.info("🔍 JobFunctions не пустий, починаємо застосування фільтрів...");
            for (String function : jobFunctions) {
                log.info("🚀 Спроба застосування фільтра для: '{}'", function);
                boolean filterApplied = pageInteractionService.clickJobFunctionFilter(driver, function);
                log.info("🔍 Результат застосування фільтра '{}': {}", function, filterApplied);
                
                if (filterApplied) {
                    anyFilterApplied = true;
                    log.info("✅ Фільтр '{}' успішно застосовано. Чекаємо на оновлення сторінки...", function);
                    // Збільшена пауза, щоб інтерфейс встиг відреагувати на застосування фільтра
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    // Додаткова перевірка, чи фільтр дійсно застосовано
                    log.info("🔍 Перевіряємо, чи фільтр '{}' дійсно застосовано...", function);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    log.warn("⚠️ Не вдалося застосувати фільтр '{}'", function);
                }
            }
        }

        if (anyFilterApplied) {
            log.info("✅ Всі фільтри застосовано. Даємо сторінці фінально завантажитись...");
            try {
                Thread.sleep(5000); // Збільшена пауза після застосування всіх фільтрів
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log.info("🔍 Сторінка готова для зчитування кількості вакансій...");
        } else {
            log.info("ℹ️ Жоден фільтр не було застосовано, продовжуємо без фільтрації.");
        }

        // ✅ КРОК 2: Отримуємо загальну кількість вакансій ПІСЛЯ застосування ВСІХ фільтрів
        log.info("🔍 КРОК 2: Отримуємо загальну кількість вакансій...");
        int totalJobsExpected = pageInteractionService.getTotalJobCountFromTextAfterFiltering(driver);

        // ✅ КРОК 3: Завантажуємо всі доступні вакансії
        log.info("🔍 КРОК 3: Завантажуємо всі доступні вакансії (очікується: {})...", totalJobsExpected);
        pageInteractionService.loadAllAvailableJobs(driver, totalJobsExpected);
        log.info("🔍 Завантаження вакансій завершено");

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
                    String preview = cardText.length() > 200 ?
                        cardText.substring(0, 200) + "..." : cardText;
                    log.info("🔍 Processing card {}: {}", i + 1, preview);
                }

                // ✅ КРОК 5: Пропускаємо фільтрацію за job functions - обробляємо всі картки
                passedFunctionFilter++;

                if (isFirstCards) {
                    log.info("🔍 Card {} processing (no function filter)", i + 1);
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
                        log.info("Card {} saved (with company prefix)", i + 1);
                    } else {
                        savedWithoutCompanyPrefix++;
                        log.info("Card {} saved (without company prefix)", i + 1);
                    }
                }

                if (i % 10 == 0) {
                    log.info("Processed {}/{} job cards", i + 1, jobCards.size());
                }

            } catch (Exception e) {
                log.warn("Error scraping job card {}: {}", i + 1, e.getMessage());
            }
        }
        // Оновлений фінальний звіт
        printUpdatedFinalReport(jobCards.size(), jobCards.size(), jobs.size(), 
            jobs.size(), savedWithCompanyPrefix, savedWithoutCompanyPrefix, jobFunctions);

        log.info("🎯 Job scraping completed with MULTIPLE FILTERS LOGIC. Created {} Job objects with real data", jobs.size());
        return jobs;
    }



    private List<Job> scrapeJobsBasedOnPageType(WebDriver driver, List<String> jobFunctions) {
        String currentUrl = driver.getCurrentUrl();
        log.info("🔍 Current URL: {}", currentUrl);

        if (currentUrl.contains("/companies/")) {
            log.info("🎯 Detected job detail page, applying new filtering logic...");
            return scrapeSingleJobFromDetailPage(driver, jobFunctions);
        } else {
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
                log.info("🔍 Detail page: URL contains company prefix '{}', applying new logic",
                    REQUIRED_PREFIX);

                // ✅ КРОК 1: Фільтрація за функціями (ПЕРШИЙ КРОК ЗА НОВОЮ ЛОГІКОЮ)
                if (jobFunctions != null && !jobFunctions.isEmpty()) {
                    // Шукаємо заголовок вакансії для перевірки функції
                    String positionName = dataExtractionService.extractTitle(driver);
                    if (positionName == null || positionName.trim().isEmpty()) {
                        log.warn("⚠️ Could not extract position name from detail page");
                        return jobs;
                    }

                    String positionText = positionName.toLowerCase();
                    boolean hasRequiredFunction = jobFunctions.stream()
                        .anyMatch(function -> positionText.contains(function.toLowerCase()));

                    if (!hasRequiredFunction) {
                        log.info("🔍 Detail page: Position '{}' does not match required "
                            + "functions: {}", positionName, jobFunctions);
                        return jobs; // Не зберігаємо, якщо не відповідає функціям
                    }
                }

                // ✅ КРОК 2: Збираємо всі дані та зберігаємо (всі проходять однакову обробку)
                log.info("🔍 Detail page: All filters passed, saving job (tags will be collected)");

                // Шукаємо заголовок вакансії
                String positionName = dataExtractionService.extractTitle(driver);
                if (positionName == null || positionName.trim().isEmpty()) {
                    log.warn("⚠️ Could not extract position name from detail page");
                    return jobs;
                }

                // Шукаємо назву компанії
                String companyName = dataExtractionService.extractCompanyName(driver);

                // Шукаємо теги
                List<String> tags = dataExtractionService.extractTags(driver);

                // Шукаємо локацію
                String location = dataExtractionService.extractLocation(driver);

                // Шукаємо дату публікації
                LocalDateTime postedDate = dataExtractionService.extractPostedDate(driver);

                // ✅ ДОДАНО: Шукаємо опис вакансії
                String description = dataExtractionService.extractDescription(driver);

                // ✅ ДОДАНО: Додаткова перевірка, щоб не зберігати назву вакансії як опис
                if (description != null && description.equals(positionName)) {
                    log.debug("📝 Skipping description as it matches position name: '{}'",
                        description);
                    description = null;
                }

                // Створюємо Job об'єкт
                Job job = jobCreationService.createJobWithAllData(
                    currentUrl, positionName, companyName, null, location, tags, postedDate,
                    jobFunctions, description
                );

                if (job != null) {
                    // ✅ ДОДАНО: Зберігаємо опис вакансії через DescriptionIngestService (тільки
                    // якщо це не заглушка)
                    if (description != null && !description.trim().isEmpty() &&
                        !description.equals("Job scraped from Techstars")) {
                        try {
                            boolean descriptionSaved =
                                descriptionIngestService.saveDescription(job, description);
                            if (descriptionSaved) {
                                log.info("✅ Successfully saved description for job ID: {}",
                                    job.getId());
                            } else {
                                log.warn("⚠️ Failed to save description for job ID: {}",
                                    job.getId());
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
                log.info("🔍 Detail page: URL does not contain company prefix, applying standard "
                    + "filtering");

                // Шукаємо заголовок вакансії
                String positionName = dataExtractionService.extractTitle(driver);
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
                        log.info("🔍 Detail page: Position '{}' does not match required "
                            + "functions: {}", positionName, jobFunctions);
                        return jobs; // Не зберігаємо, якщо не відповідає функціям
                    }
                }

                // ✅ КРОК 3: Збираємо всі дані та зберігаємо
                log.info("🔍 Detail page: All filters passed, saving job with standard filtering "
                    + "(tags will be collected)");

                // Шукаємо назву компанії
                String companyName = dataExtractionService.extractCompanyName(driver);

                // Шукаємо теги
                List<String> tags = dataExtractionService.extractTags(driver);

                // Шукаємо локацію
                String location = dataExtractionService.extractLocation(driver);

                // Шукаємо дату публікації
                LocalDateTime postedDate = dataExtractionService.extractPostedDate(driver);

                // ✅ ДОДАНО: Шукаємо опис вакансії
                String description = dataExtractionService.extractDescription(driver);

                // Створюємо Job об'єкт
                Job job = jobCreationService.createJobWithAllData(
                    currentUrl, positionName, companyName, null, location, tags, postedDate,
                    jobFunctions, description
                );

                if (job != null) {
                    jobs.add(job);
                    log.info("✅ Successfully scraped job with standard filtering: {}",
                        positionName);
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
            List<WebElement> jobCards = pageInteractionService.findJobCardsOnCompanyPage(driver);
            log.info("🔍 Found {} job cards on company page", jobCards.size());

            for (WebElement card : jobCards) {
                try {
                                    // ✅ КРОК 1: Пропускаємо фільтрацію за функціями - обробляємо всі картки

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
     * 1) job functions → 2) Load More (ОДИН раз) → 3) нескінченна прокрутка → 4) URL → 5)
     * префікс компанії → 6) збір тегів
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

    private Job createJobFromCard(WebElement card, String jobPageUrl, List<String> jobFunctions) {
        try {
            log.debug("🔍 Creating Job object for URL: {}", jobPageUrl);
            String organizationTitle = dataExtractionService.extractCompanyName(card);
            String positionName = dataExtractionService.extractTitle(card);
            List<String> tags = dataExtractionService.extractTags(card);
            String location = dataExtractionService.extractLocation(card);
            LocalDateTime postedDate = dataExtractionService.extractPostedDate(card);
            String logoUrl = dataExtractionService.extractLogoUrl(card);
            String description = dataExtractionService.extractDescription(card);

            String defaultFunction = jobFunctions.isEmpty() ?
                "Software Engineering" : jobFunctions.get(0);

            log.info("📋 Job extracted: '{}' at '{}' | Location: '{}' | Tags: {} | Posted: {} | Logo: {} | Description: {}",
                positionName, organizationTitle, location, tags,
                postedDate != null ? postedDate.toEpochSecond(java.time.ZoneOffset.UTC) : "null",
                logoUrl != null ? "Found" : "Not found",
                description != null && !description.trim().isEmpty() ? "Found" : "Not found");

            // ✅ ВИПРАВЛЕНО: Використовуємо JobCreationService для створення Job з усіма даними
            Job job = jobCreationService.createJobWithAllData(
                jobPageUrl, positionName, organizationTitle, logoUrl, location, tags, postedDate,
                jobFunctions, description
            );

            // Зберігаємо опис вакансії (тільки якщо це
            // не заглушка)
            if (job != null && description != null && !description.trim().isEmpty() &&
                !description.equals("Job scraped from Techstars")) {
                try {
                    boolean descriptionSaved = descriptionIngestService.saveDescription(job,
                        description);
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
        log.info("📊 ОНОВЛЕНИЙ ЗВІТ ПРО ФІЛЬТРАЦІЮ:");
        log.info("   • Всього карток: {}", totalCards);
        log.info("   • Пройшли фільтр функцій: {}", passedFunctionFilter);
        log.info("   • Знайдено URL: {}", foundUrls);
        log.info("   • Збережено з префіксом компанії: {}",
            savedWithCompanyPrefix);
        log.info("   • Збережено без префіксу компанії (тільки фільтр функцій): {}",
            savedWithoutCompanyPrefix);
        log.info("   • Фінальних вакансій: {}", finalJobs);
        log.info("   • Застосовані функції: {}", functions);
        log.info("🎯 Результат: {} з {} карток успішно оброблено", finalJobs, totalCards);
    }
}
