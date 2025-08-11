package com.scrapper.service;

import com.scrapper.model.Job;
import com.scrapper.service.criteriaServices.DataExtractionService;
import com.scrapper.service.criteriaServices.DescriptionIngestService;
import com.scrapper.util.ScrapingSelectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервіс для обробки карток вакансій
 * Відповідає за пошук, фільтрацію та створення вакансій з карток
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobCardProcessingService {

    private static final String REQUIRED_PREFIX = "https://jobs.techstars.com/companies/";
    private static final String JOB_CARD_SELECTOR = ScrapingSelectors.JOB_CARD[0];

    private final DataExtractionService dataExtractionService;
    private final JobCreationService jobCreationService;
    private final DescriptionIngestService descriptionIngestService;

    /**
     * Знаходить всі картки вакансій на сторінці
     */
    public List<WebElement> findJobCardsWithMultipleStrategies(WebDriver driver) {
        log.info("🔍 Searching for job cards using multiple strategies...");
        
        List<WebElement> jobCards = new ArrayList<>();
        
        // Основний селектор
        jobCards.addAll(findJobCardsByMainSelector(driver));
        
        // Альтернативні селектори
        if (jobCards.isEmpty()) {
            jobCards.addAll(findJobCardsByAlternativeSelectors(driver));
        }
        
        // Data-атрибути
        if (jobCards.isEmpty()) {
            jobCards.addAll(findJobCardsByDataAttributes(driver));
        }
        
        // Загальні класи
        if (jobCards.isEmpty()) {
            jobCards.addAll(findJobCardsByClasses(driver));
        }
        
        log.info("✅ Found {} job cards using multiple strategies", jobCards.size());
        return jobCards;
    }

    /**
     * Знаходить картки за основним селектором
     */
    private List<WebElement> findJobCardsByMainSelector(WebDriver driver) {
        try {
            List<WebElement> cards = driver.findElements(By.cssSelector(JOB_CARD_SELECTOR));
            log.debug("🔍 Main selector found {} job cards", cards.size());
            return cards;
        } catch (Exception e) {
            log.debug("⚠️ Main selector failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Знаходить картки за альтернативними селекторами
     */
    private List<WebElement> findJobCardsByAlternativeSelectors(WebDriver driver) {
        List<WebElement> cards = new ArrayList<>();
        String[] alternativeSelectors = ScrapingSelectors.JOB_CARD;
        
        for (int i = 1; i < alternativeSelectors.length; i++) {
            try {
                List<WebElement> foundCards = driver.findElements(By.cssSelector(alternativeSelectors[i]));
                if (!foundCards.isEmpty()) {
                    log.debug("🔍 Alternative selector {} found {} job cards", i, foundCards.size());
                    cards.addAll(foundCards);
                    break;
                }
            } catch (Exception e) {
                log.debug("⚠️ Alternative selector {} failed: {}", i, e.getMessage());
            }
        }
        
        return cards;
    }

    /**
     * Знаходить картки за data-атрибутами
     */
    private List<WebElement> findJobCardsByDataAttributes(WebDriver driver) {
        List<WebElement> cards = new ArrayList<>();
        
        try {
            String[] dataSelectors = {
                "[data-testid='job-card']", "[data-testid='job-item']", 
                "[data-testid='position-card']", "[data-testid='vacancy-card']"
            };
            
            for (String selector : dataSelectors) {
                cards.addAll(driver.findElements(By.cssSelector(selector)));
            }
            
            log.debug("🔍 Data attributes found {} job cards", cards.size());
        } catch (Exception e) {
            log.debug("⚠️ Data attributes search failed: {}", e.getMessage());
        }
        
        return cards;
    }

    /**
     * Знаходить картки за класами
     */
    private List<WebElement> findJobCardsByClasses(WebDriver driver) {
        List<WebElement> cards = new ArrayList<>();
        
        try {
            String[] classSelectors = {
                ".job-card", ".job-item", ".position-card", ".vacancy-card",
                ".career-card", ".job-listing", ".position-item", ".job-posting"
            };
            
            for (String selector : classSelectors) {
                try {
                    List<WebElement> foundCards = driver.findElements(By.cssSelector(selector));
                    if (!foundCards.isEmpty()) {
                        log.debug("🔍 Class selector '{}' found {} job cards", selector, foundCards.size());
                        cards.addAll(foundCards);
                        break;
                    }
                } catch (Exception e) {
                    log.debug("⚠️ Class selector '{}' failed: {}", selector, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.debug("⚠️ Class-based search failed: {}", e.getMessage());
        }
        
        return cards;
    }

    /**
     * Знаходить картки на сторінці компанії
     */
    public List<WebElement> findJobCardsOnCompanyPage(WebDriver driver) {
        log.info("🔍 Searching for job cards on company page...");
        
        List<WebElement> jobCards = new ArrayList<>();
        
        try {
            // Спочатку шукаємо за специфічними селекторами компанії
            jobCards.addAll(driver.findElements(By.cssSelector("[data-testid='company-job-card']")));
            
            // Якщо не знайшли, використовуємо загальні стратегії
            if (jobCards.isEmpty()) {
                jobCards.addAll(findJobCardsWithMultipleStrategies(driver));
            }
            
            log.info("✅ Found {} job cards on company page", jobCards.size());
        } catch (Exception e) {
            log.warn("⚠️ Error finding job cards on company page: {}", e.getMessage());
        }
        
        return jobCards;
    }

    /**
     * Фільтрує картки за функціями вакансій
     */
    public List<WebElement> filterByJobFunctions(List<WebElement> cards, List<String> jobFunctions) {
        if (cards == null || cards.isEmpty() || jobFunctions == null || jobFunctions.isEmpty()) {
            log.warn("⚠️ Empty cards or job functions, returning empty list");
            return List.of();
        }

        log.info("🔍 Filtering {} cards by {} job functions...", cards.size(), jobFunctions.size());
        
        List<WebElement> filteredCards = cards.stream()
                .filter(card -> hasRequiredJobFunction(card, jobFunctions))
                .collect(Collectors.toList());
        
        log.info("✅ Filtered to {} cards matching job functions", filteredCards.size());
        return filteredCards;
    }

    /**
     * Перевіряє чи картка має необхідну функцію
     */
    private boolean hasRequiredJobFunction(WebElement card, List<String> jobFunctions) {
        try {
            String cardText = card.getText().toLowerCase();
            
            for (String function : jobFunctions) {
                if (function != null && !function.trim().isEmpty()) {
                    String functionLower = function.toLowerCase().trim();
                    
                    if (cardText.contains(functionLower) ||
                        cardText.contains(functionLower.replace(" ", "")) ||
                        cardText.contains(functionLower.replace(" ", "-"))) {
                        
                        log.debug("✅ Card matches job function: '{}'", function);
                        return true;
                    }
                }
            }
            
            return false;
            
        } catch (Exception e) {
            log.debug("⚠️ Error checking job function: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Знаходить URL вакансії з картки
     */
    public String findDirectJobUrl(WebElement jobCard) {
        try {
            // Пошук за посиланням
            String url = findJobUrlByLink(jobCard);
            if (url != null) return url;
            
            // Пошук за кнопкою
            url = findJobUrlByButton(jobCard);
            if (url != null) return url;
            
            // Пошук за data-атрибутом
            url = findJobUrlByDataAttribute(jobCard);
            if (url != null) return url;
            
            log.debug("⚠️ No job URL found in card");
            return null;
            
        } catch (Exception e) {
            log.debug("⚠️ Error finding job URL: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Знаходить URL за посиланням
     */
    private String findJobUrlByLink(WebElement jobCard) {
        try {
            List<WebElement> links = jobCard.findElements(By.cssSelector("a[href]"));
            
            for (WebElement link : links) {
                String href = link.getAttribute("href");
                if (href != null && !href.trim().isEmpty()) {
                    log.debug("🔍 Found job URL by link: {}", href);
                    return href;
                }
            }
            
            return null;
        } catch (Exception e) {
            log.debug("⚠️ Error finding job URL by link: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Знаходить URL за кнопкою
     */
    private String findJobUrlByButton(WebElement jobCard) {
        try {
            List<WebElement> buttons = jobCard.findElements(By.cssSelector("button[data-url], button[data-href]"));
            
            for (WebElement button : buttons) {
                String url = button.getAttribute("data-url");
                if (url == null) {
                    url = button.getAttribute("data-href");
                }
                
                if (url != null && !url.trim().isEmpty()) {
                    log.debug("🔍 Found job URL by button: {}", url);
                    return url;
                }
            }
            
            return null;
        } catch (Exception e) {
            log.debug("⚠️ Error finding job URL by button: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Знаходить URL за data-атрибутом
     */
    private String findJobUrlByDataAttribute(WebElement jobCard) {
        try {
            String[] dataAttributes = {"data-job-url", "data-position-url", "data-vacancy-url", "data-href"};
            
            for (String attr : dataAttributes) {
                String url = jobCard.getAttribute(attr);
                if (url != null && !url.trim().isEmpty()) {
                    log.debug("🔍 Found job URL by data attribute '{}': {}", attr, url);
                    return url;
                }
            }
            
            return null;
        } catch (Exception e) {
            log.debug("⚠️ Error finding job URL by data attribute: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Створює вакансію з картки
     */
    public Job createJobFromCard(WebElement card, String jobPageUrl, List<String> jobFunctions) {
        try {
            log.debug("🔍 Creating Job object for URL: {}", jobPageUrl);
            
            // Витягуємо дані через DataExtractionService
            String organizationTitle = dataExtractionService.extractCompanyName(card);
            String positionName = dataExtractionService.extractTitle(card);
            List<String> tags = dataExtractionService.extractTags(card);
            String location = dataExtractionService.extractLocation(card);
            LocalDateTime postedDate = dataExtractionService.extractPostedDate(card);
            String logoUrl = dataExtractionService.extractLogoUrl(card);
            String description = dataExtractionService.extractDescription(card);
            
            log.info("🏢 Company: '{}', Position: '{}', Location: '{}'", 
                    organizationTitle, positionName, location);
            
            // Створюємо вакансію
            Job job = jobCreationService.createJobWithAllData(
                jobPageUrl, positionName, organizationTitle, logoUrl, 
                location, tags, postedDate, jobFunctions, description
            );
            
            // Зберігаємо опис (якщо це не заглушка)
            if (job != null && description != null && !description.trim().isEmpty() && 
                !description.equals("Job scraped from Techstars")) {
                try {
                    boolean descriptionSaved = descriptionIngestService.saveDescription(job, description);
                    if (descriptionSaved) {
                        log.info("✅ Description saved for job ID: {}", job.getId());
                    }
                } catch (Exception e) {
                    log.error("❌ Error saving description for job ID: {}", job.getId(), e);
                }
            }
            
            return job;
                    
        } catch (Exception e) {
            log.warn("⚠️ Error creating Job object: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Фільтрує вакансії за префіксом компанії
     */
    public List<Job> filterJobsByCompanyPrefix(List<Job> jobs) {
        log.info("🔍 Filtering {} jobs by company prefix...", jobs.size());
        
        List<Job> filteredJobs = jobs.stream()
                .filter(job -> {
                    String jobUrl = job.getJobPageUrl();
                    return jobUrl != null && jobUrl.contains(REQUIRED_PREFIX);
                })
                .collect(Collectors.toList());
        
        log.info("✅ Filtered to {} jobs with company prefix", filteredJobs.size());
        return filteredJobs;
    }

    /**
     * Перевіряє чи URL має необхідний префікс
     */
    public boolean hasRequiredCompanyPrefix(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        boolean hasPrefix = url.contains(REQUIRED_PREFIX);
        
        if (hasPrefix) {
            log.debug("✅ URL has required company prefix: {}", url);
        } else {
            log.debug("⚠️ URL missing required company prefix: {}", url);
        }
        
        return hasPrefix;
    }

    /**
     * Отримує префікс компанії
     */
    public String getCompanyPrefix() {
        return REQUIRED_PREFIX;
    }
}
