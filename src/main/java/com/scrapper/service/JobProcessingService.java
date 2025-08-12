package com.scrapper.service;

import com.scrapper.model.Job;
import com.scrapper.service.criteriaServices.DataExtractionService;
import com.scrapper.service.criteriaServices.DescriptionIngestService;
import com.scrapper.validation.Validation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервіс для обробки карток вакансій
 * Відповідає за створення об'єктів Job та обробку карток
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobProcessingService {

    private final JobCreationService jobCreationService;
    private final DataExtractionService dataExtractionService;
    private final DescriptionIngestService descriptionIngestService;
    private final JobReportingService jobReportingService;
    private final PageInteractionService pageInteractionService;

    /**
     * Обробляє всі вакансії з покращеною логікою
     */
    public List<Job> scrapeAllJobsWithImprovedLogic(WebDriver driver, List<String> jobFunctions, String requiredPrefix) {
        log.info("🔍 Job functions to filter by: {} (type: {})", jobFunctions,
                jobFunctions != null ? jobFunctions.getClass().getSimpleName() : "null");
        
        if (jobFunctions != null) {
            for (int i = 0; i < jobFunctions.size(); i++) {
                String function = jobFunctions.get(i);
                log.info("🔍 Job function {}: '{}' (type: {})", i, function, 
                        function != null ? function.getClass().getSimpleName() : "null");
            }
        }
        
        // КРОК 1: Спочатку фільтрація за job functions
        log.info("🔍 КРОК 1: Застосовуємо фільтрацію за job functions...");
        
        // КРОК 2: Натискаємо кнопку Load More ОДИН раз
        log.info("🔍 КРОК 2: Натискаємо кнопку Load More ОДИН раз...");
        pageInteractionService.clickLoadMoreButton(driver);
        
        // КРОК 3: Скролимо сторінку до низу
        log.info("🔍 КРОК 3: Скролимо сторінку до низу...");
        pageInteractionService.scrollToBottom(driver);
        
        // КРОК 4: Тепер шукаємо всі картки вакансій
        log.info("🔍 КРОК 4: Шукаємо всі картки вакансій після завантаження...");
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
                
                // Логуємо тільки перші 5 карток для діагностики
                boolean isFirstCards = i < 5;
                if (isFirstCards) {
                    String cardText = card.getText();
                    String preview = cardText.length() > 200 ? cardText.substring(0, 200) + "..." : cardText;
                    log.info("Processing card {}: {}", i + 1, preview);
                }
                
                // КРОК 5: Пропускаємо фільтрацію за job functions - обробляємо всі картки
                passedFunctionFilter++;
                
                if (isFirstCards) {
                    log.info("Card {} processing (no function filter)", i + 1);
                }
                
                // КРОК 6: Пошук URL (ДРУГИЙ КРОК ЗА НОВОЮ ЛОГІКОЮ)
                String jobPageUrl = pageInteractionService.findDirectJobUrl(card);
                if (jobPageUrl == null) {
                    if (isFirstCards) {
                        log.info("Card {}: No URL found after passing function filter", i + 1);
                    }
                    continue;
                }
                
                foundUrls++;
                
                if (isFirstCards) {
                    log.info("Card {}: URL found: {}", i + 1, jobPageUrl);
                }
                
                // КРОК 7: Збереження вакансії (всі проходять однакову обробку)
                Job job = createJobFromCard(card, jobPageUrl, jobFunctions);
                if (job != null) {
                    jobs.add(job);
                    if (jobPageUrl.startsWith(requiredPrefix)) {
                        savedWithCompanyPrefix++;
                        log.info("Card {} saved (with company prefix)", i + 1);
                    } else {
                        savedWithoutCompanyPrefix++;
                        log.info("Card {} saved (without company prefix)", i + 1);
                    }
                }
                
                if (i % 10 == 0) {
                    log.info("📊 Processed {}/{} job cards", i + 1, jobCards.size());
                }
                
            } catch (Exception e) {
                log.warn("⚠️ Error scraping job card {}: {}", i + 1, e.getMessage());
            }
        }
        // Оновлений звіт з новою логікою
        jobReportingService.printUpdatedFinalReport(jobCards.size(), passedFunctionFilter, foundUrls, 
                                                   jobs.size(), savedWithCompanyPrefix, savedWithoutCompanyPrefix, jobFunctions);
        return jobs;
    }

    /**
     * Створює об'єкт Job з картки вакансії
     */
    public Job createJobFromCard(WebElement jobCard, String jobPageUrl, List<String> jobFunctions) {
        try {
            log.info("🔧 Creating Job object from card for URL: {}", jobPageUrl);
            
            // Екстракція даних з картки
            String positionName = dataExtractionService.extractTitle(jobCard);
            String organizationTitle = dataExtractionService.extractCompanyName(jobCard);

            if (positionName == null || positionName.trim().isEmpty()) {
                log.warn("⚠️ Could not extract position name from card");
                return null;
            }

            if (organizationTitle == null || organizationTitle.trim().isEmpty()) {
                log.warn("⚠️ Could not extract company name from card");
                return null;
            }
            
            // Екстракція додаткових даних
            String logoUrl = dataExtractionService.extractLogoUrl(jobCard);
            String location = dataExtractionService.extractLocation(jobCard);
            List<String> tags = dataExtractionService.extractTags(jobCard);
            
            log.info("📋 Job extracted: '{}' at '{}' | Location: '{}' | Tags: {} | Logo: {}",
                positionName, organizationTitle, location, tags, logoUrl != null ? "Found" : "Not found");
            
            // Створення Job об'єкта
            Job job = jobCreationService.createJobWithAllData(
                jobPageUrl, positionName, organizationTitle, logoUrl, location, tags, null, jobFunctions, null
            );
            
            return job;
                    
        } catch (Exception e) {
            log.warn("⚠️ Error creating Job object: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Обробляє вакансії з головної сторінки
     */
    public List<Job> scrapeJobsFromMainPage(WebDriver driver, List<String> jobFunctions, String requiredPrefix) {
        log.info("📋 Scraping jobs from main page with new logic");
        return scrapeAllJobsWithImprovedLogic(driver, jobFunctions, requiredPrefix);
    }

    /**
     * Обробляє вакансії зі сторінки компанії
     */
    public List<Job> scrapeJobsFromCompanyPage(WebDriver driver, List<String> jobFunctions, String requiredPrefix) {
        log.info("🏢 Scraping jobs from company page with new logic");
        
        // Знаходимо картки вакансій на сторінці компанії
        List<WebElement> jobCards = pageInteractionService.findJobCardsOnCompanyPage(driver);
        List<Job> jobs = new ArrayList<>();
        
        log.info("📋 Found {} job cards on company page", jobCards.size());
        
        if (jobCards.isEmpty()) {
            log.warn("⚠️ No job cards found on company page");
            return jobs;
        }
        
        int passedFunctionFilter = 0;
        int foundUrls = 0;
        int savedJobs = 0;
        
        for (int i = 0; i < jobCards.size(); i++) {
            try {
                WebElement card = jobCards.get(i);
                
                // Пропускаємо фільтрацію за функціями - обробляємо всі картки
                passedFunctionFilter++;
                
                // Пошук URL
                String jobPageUrl = pageInteractionService.findDirectJobUrl(card);
                if (jobPageUrl == null) {
                    continue;
                }
                foundUrls++;
                
                // Створення Job об'єкта
                Job job = createJobFromCard(card, jobPageUrl, jobFunctions);
                if (job != null) {
                    jobs.add(job);
                    savedJobs++;
                }
                
                if (i % 10 == 0) {
                    log.info("📊 Processed {}/{} company page job cards", i + 1, jobCards.size());
                }
                
            } catch (Exception e) {
                log.warn("⚠️ Error processing company page job card {}: {}", i + 1, e.getMessage());
            }
        }
        
        log.info("✅ Company page scraping completed: {} jobs saved from {} cards", savedJobs, jobCards.size());
        return jobs;
    }

    /**
     * Обробляє одну вакансію з детальної сторінки
     */
    public List<Job> scrapeSingleJobFromDetailPage(WebDriver driver, List<String> jobFunctions, String requiredPrefix) {
        List<Job> jobs = new ArrayList<>();
        
        try {
            String currentUrl = driver.getCurrentUrl();
            
            // Перевіряємо, чи URL містить потрібний префікс компанії
            if (currentUrl.startsWith(requiredPrefix)) {
                log.info("🔍 Detail page: URL contains company prefix '{}', applying new logic", requiredPrefix);
                
                // Фільтрація за функціями
                if (jobFunctions != null && !jobFunctions.isEmpty()) {
                    String positionName = dataExtractionService.extractTitle(driver);
                    if (positionName == null || positionName.trim().isEmpty()) {
                        log.warn("⚠️ Could not extract position name from detail page");
                        return jobs;
                    }
                    
                    String positionText = positionName.toLowerCase();
                    boolean hasRequiredFunction = jobFunctions.stream()
                        .anyMatch(function -> positionText.contains(function.toLowerCase()));
                    
                    if (!hasRequiredFunction) {
                        log.info("🔍 Detail page: Position '{}' does not match required functions: {}", positionName, jobFunctions);
                        return jobs;
                    }
                }
                
                // Збираємо всі дані та зберігаємо
                log.info("🔍 Detail page: All filters passed, saving job");
                
                String positionName = dataExtractionService.extractTitle(driver);
                String companyName = dataExtractionService.extractCompanyName(driver);
                List<String> tags = dataExtractionService.extractTags(driver);
                String location = dataExtractionService.extractLocation(driver);
                String logoUrl = null; // Logo URL не доступний на детальній сторінці
                String description = dataExtractionService.extractDescription(driver);
                
                if (positionName != null && companyName != null) {
                    Job job = jobCreationService.createJobWithAllData(
                        currentUrl, positionName, companyName, logoUrl, location, tags, null, jobFunctions, description
                    );
                    
                    if (job != null) {
                        jobs.add(job);
                        
                        // Зберігаємо опис вакансії
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
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Error scraping single job from detail page: {}", e.getMessage(), e);
        }
        
        return jobs;
    }


}
