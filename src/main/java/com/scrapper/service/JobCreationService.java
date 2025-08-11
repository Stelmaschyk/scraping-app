package com.scrapper.service;

import com.scrapper.dto.ScrapeResponseDto;
import com.scrapper.model.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobCreationService {

    public List<Job> createJobsFromUrls(List<String> jobUrls, List<String> jobFunctions) {
        String defaultFunction = jobFunctions.isEmpty() ? 
                "Software Engineering" : jobFunctions.get(0);

        return jobUrls.stream()
                .map(url -> {
                    Job job = Job.builder()
                            .positionName("Job from " + url)
                            .jobPageUrl(url)
                            .organizationUrl(url)
                            .organizationTitle("Company from Techstars")
                            .laborFunction(defaultFunction)
                            .address("Remote")
                            .description("Job scraped from Techstars")
                            .build();
                    
                    // ✅ ОНОВЛЕНО: Використовуємо новий метод для встановлення Unix Timestamp
                    job.setCurrentTimeAsPostedDate();
                    
                    return job;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Створити Job з усіма доступними даними
     */
    public Job createJobWithAllData(String jobPageUrl, String positionName, String organizationTitle, 
                                   String logoUrl, String location, List<String> tags, 
                                   LocalDateTime postedDate, List<String> jobFunctions, String description) {
        String defaultFunction = jobFunctions.isEmpty() ? 
                "Software Engineering" : jobFunctions.get(0);
                
        Job job = Job.builder()
                .positionName(positionName != null ? positionName : "Job from " + jobPageUrl)
                .jobPageUrl(jobPageUrl)
                .organizationUrl(jobPageUrl)
                .organizationTitle(organizationTitle != null ? organizationTitle : "Company from Techstars")
                .laborFunction(defaultFunction)
                .address(location != null && !location.trim().isEmpty() ? location : "Remote")
                .description(description != null && !description.trim().isEmpty() ? description : "Job scraped from Techstars")
                .logoUrl(logoUrl)
                .build();
        
        // ✅ ОНОВЛЕНО: Використовуємо новий метод для конвертації LocalDateTime в Unix Timestamp
        job.setPostedDateFromLocalDateTime(postedDate);
        
        // Додаємо теги
        if (tags != null && !tags.isEmpty()) {
            tags.stream()
                .filter(tag -> tag != null && !tag.trim().isEmpty())
                .forEach(tag -> job.addTag(tag.trim()));
        }
        
        // Додаємо локацію
        if (location != null && !location.trim().isEmpty()) {
            job.addLocation(location.trim());
        }
        
        return job;
    }

    public ScrapeResponseDto createEmptyResponse(String message) {
        log.info("ℹ️ Creating empty response: {}", message);
        return ScrapeResponseDto.builder()
                .success(true)
                .message(message)
                .totalJobsFound(0)
                .jobsSaved(0)
                .jobUrls(List.of())
                .build();
    }

    public ScrapeResponseDto createSuccessResponse(List<String> jobUrls, int savedCount) {
        return ScrapeResponseDto.builder()
                .success(true)
                .message("Scraping and saving completed successfully")
                .totalJobsFound(jobUrls.size())
                .jobsSaved(savedCount)
                .jobUrls(jobUrls)
                .build();
    }

    public ScrapeResponseDto createErrorResponse(String errorMessage) {
        return ScrapeResponseDto.builder()
                .success(false)
                .message("Error during scraping: " + errorMessage)
                .totalJobsFound(0)
                .jobsSaved(0)
                .jobUrls(List.of())
                .build();
    }
}
