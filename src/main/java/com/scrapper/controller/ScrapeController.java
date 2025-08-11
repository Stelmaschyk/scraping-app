package com.scrapper.controller;

import com.scrapper.dto.ScrapeRequestDto;
import com.scrapper.dto.ScrapeResponseDto;
import com.scrapper.model.Job;
import com.scrapper.service.ApplyUrlScraperService;
import com.scrapper.service.JobCreationService;
import com.scrapper.service.JobIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ScrapeController {

    private final ApplyUrlScraperService scraperService;
    private final JobIngestService jobIngestService;
    private final JobCreationService jobCreationService;

    @PostMapping("/test-json")
    public String testJsonProcessing(@RequestBody ScrapeRequestDto request) {
        log.info("🧪 Test JSON processing:");
        log.info("🧪 Job functions: {} (type: {})", request.getJobFunctions(), 
                request.getJobFunctions() != null ? request.getJobFunctions().getClass().getSimpleName() : "null");
        log.info("🧪 Tags: {} (type: {})", request.getTags(), 
                request.getTags() != null ? request.getTags().getClass().getSimpleName() : "null");
        
        if (request.getJobFunctions() != null) {
            for (int i = 0; i < request.getJobFunctions().size(); i++) {
                String function = request.getJobFunctions().get(i);
                log.info("🧪 Job function {}: '{}' (type: {})", i, function, 
                        function != null ? function.getClass().getSimpleName() : "null");
            }
        }
        
        return "JSON processed successfully. Check logs for details.";
    }

    @PostMapping("/test-filter")
    public String testFilterLogic(@RequestBody ScrapeRequestDto request) {
        log.info("🧪 Testing filter logic:");
        log.info("🧪 Job functions: {}", request.getJobFunctions());
        log.info("🧪 Tags: {}", request.getTags());
        
        // Тестуємо логіку фільтрації
        StringBuilder result = new StringBuilder();
        result.append("Filter Logic Test Results:\n\n");
        
        // Тестуємо різні варіанти тексту карток
        String[] testCardTexts = {
            "Software Engineer at Tech Company - IT, Design, Remote",
            "Product Manager - Marketing, Operations",
            "UX Designer - Design, IT, Full-time",
            "Data Scientist - Software Engineering, Remote",
            "Marketing Specialist - Marketing & Communications"
        };
        
        result.append("Testing job function filtering:\n");
        for (String cardText : testCardTexts) {
            boolean hasFunction = request.getJobFunctions().stream()
                    .anyMatch(function -> cardText.toLowerCase().contains(function.toLowerCase()));
            
            result.append(String.format("Card: '%s' -> Has function: %s\n", 
                    cardText, hasFunction ? "YES" : "NO"));
        }
        
        result.append("\nTesting tag filtering:\n");
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            for (String cardText : testCardTexts) {
                boolean hasTag = request.getTags().stream()
                        .anyMatch(tag -> cardText.toLowerCase().contains(tag.toLowerCase()));
                
                result.append(String.format("Card: '%s' -> Has tag: %s\n", 
                        cardText, hasTag ? "YES" : "NO"));
            }
        } else {
            result.append("No tags specified\n");
        }
        
        log.info("🧪 Filter test completed");
        return result.toString();
    }

    @PostMapping("/scrape")
    public ScrapeResponseDto scrapeAndSaveJobs(@Valid @RequestBody ScrapeRequestDto request) {
        long startTime = System.currentTimeMillis();

        log.info("🚀 Starting scrape and save operation: jobFunctions={}, tags={}",
                request.getJobFunctions(),
                request.getTags());

        try {
            // Крок 1: Скрапінг та створення Job об'єктів з реальними даними
            log.info("🔍 Step 1: Starting Selenium scraping and job creation...");
            List<Job> jobs = scraperService.scrapeAndCreateJobs(
                    request.getJobFunctions(),
                    request.getTags()
            );

            log.info("✅ Scraping completed. Created {} Job objects with real data", jobs.size());

            if (jobs.isEmpty()) {
                log.warn("⚠️ No jobs found during scraping");
                return jobCreationService.createEmptyResponse("No jobs found during scraping");
            }

            // Крок 2: Збереження в базу даних
            log.info("💾 Step 2: Saving jobs to database...");
            int savedCount = jobIngestService.saveJobs(jobs);
            log.info("✅ Database operation completed. Saved {} jobs", savedCount);

            // Крок 3: Формування відповіді
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("🎉 Scrape and save operation completed in {} ms", executionTime);

            // Витягуємо URL з Job об'єктів для відповіді
            List<String> jobUrls = jobs.stream()
                    .map(Job::getJobPageUrl)
                    .collect(Collectors.toList());

            return jobCreationService.createSuccessResponse(jobUrls, savedCount);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("❌ Scrape and save operation failed after {} ms", executionTime, e);

            return jobCreationService.createErrorResponse(e.getMessage());
        }
    }
}
