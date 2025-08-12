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

            // ✅ СПРОЩЕНА ЛОГІКА: Використовуємо тільки головну сторінку
            log.info("🔍 Using main page scraping logic: 1) job functions → 2) Load More → 3) scrolling → 4) URL → 5) company prefix");
            List<Job> jobs = scrapeAllJobsWithImprovedLogic(driver, jobFunctions);

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



    /**
     * Основна логіка скрапінгу з головної сторінки:
     * 1. Застосування фільтрів job functions
     * 2. Завантаження всіх вакансій (Load More + прокрутка)
     * 3. Обробка карток та збереження вакансій
     */
    private List<Job> scrapeAllJobsWithImprovedLogic(WebDriver driver, List<String> jobFunctions) {
        log.info("🔍 Застосовуємо фільтри для job functions: {}", jobFunctions);
        
        boolean anyFilterApplied = false;
        
        if (jobFunctions != null && !jobFunctions.isEmpty()) {
            for (String function : jobFunctions) {
                boolean filterApplied = pageInteractionService.clickJobFunctionFilter(driver, function);
                
                if (filterApplied) {
                    anyFilterApplied = true;
                    log.info("✅ Фільтр '{}' застосовано", function);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    log.warn("⚠️ Не вдалося застосувати фільтр '{}'", function);
                }
            }
        }

        if (anyFilterApplied) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Отримуємо загальну кількість вакансій
        log.info("🔍 Отримуємо загальну кількість вакансій...");
        int totalJobsExpected = pageInteractionService.getTotalJobCountFromTextAfterFiltering(driver);

        // Завантажуємо всі доступні вакансії
        log.info("🔍 Завантажуємо всі доступні вакансії (очікується: {})...", totalJobsExpected);
        pageInteractionService.loadAllAvailableJobs(driver, totalJobsExpected);
        log.info("🔍 Завантаження вакансій завершено");

        // Шукаємо всі картки вакансій
        log.info("🔍 Шукаємо всі картки вакансій після завантаження...");
        List<WebElement> jobCards = pageInteractionService.findJobCardsWithMultipleStrategies(driver);
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

                // Обробляємо всі картки
                passedFunctionFilter++;

                // Пошук URL
                String jobPageUrl = pageInteractionService.findDirectJobUrl(card);
                if (jobPageUrl == null) {
                    continue;
                }

                foundUrls++;

                // Збереження вакансії
                Job job = createJobFromCard(card, jobPageUrl, jobFunctions);
                if (job != null) {
                    jobs.add(job);
                    if (jobPageUrl.startsWith(REQUIRED_PREFIX)) {
                        savedWithCompanyPrefix++;
                    } else {
                        savedWithoutCompanyPrefix++;
                    }
                }

                // Логуємо прогрес рідше - кожні 50 карток
                if ((i + 1) % 50 == 0) {
                    log.info("Processed {}/{} job cards", i + 1, jobCards.size());
                }

            } catch (Exception e) {
                log.warn("Error scraping job card {}: {}", i + 1, e.getMessage());
            }
        }
        // Фінальний звіт
        log.info("📊 ЗВІТ: {} з {} карток оброблено | URL: {} | Збережено: {} (з префіксом: {}, без префіксу: {}) | Функції: {}",
            jobs.size(), jobCards.size(), foundUrls, jobs.size(), savedWithCompanyPrefix, savedWithoutCompanyPrefix, jobFunctions);

        log.info("🎯 Job scraping completed with MULTIPLE FILTERS LOGIC. Created {} Job objects with real data", jobs.size());
        return jobs;
    }

    private Job createJobFromCard(WebElement card, String jobPageUrl, List<String> jobFunctions) {
        try {
            String organizationTitle = dataExtractionService.extractCompanyName(card);
            String positionName = dataExtractionService.extractTitle(card);
            List<String> tags = dataExtractionService.extractTags(card);
            String location = dataExtractionService.extractLocation(card);
            LocalDateTime postedDate = dataExtractionService.extractPostedDate(card);
            String logoUrl = dataExtractionService.extractLogoUrl(card);
            String description = dataExtractionService.extractDescription(card);

            String defaultFunction = jobFunctions.isEmpty() ?
                "Software Engineering" : jobFunctions.get(0);

            // ✅ ВИПРАВЛЕНО: Використовуємо JobCreationService для створення Job з усіма даними
            Job job = jobCreationService.createJobWithAllData(
                jobPageUrl, positionName, organizationTitle, logoUrl, location, tags, postedDate,
                jobFunctions, description
            );

            // Зберігаємо опис вакансії (тільки якщо це не заглушка)
            if (job != null && description != null && !description.trim().isEmpty() &&
                !description.equals("Job scraped from Techstars")) {
                try {
                    descriptionIngestService.saveDescription(job, description);
                } catch (Exception e) {
                    log.warn("⚠️ Error saving description for job ID: {}: {}", job.getId(), e.getMessage());
                }
            }

            return job;

        } catch (Exception e) {
            log.warn("⚠️ Error creating Job object: {}", e.getMessage());
            return null;
        }
    }



}
