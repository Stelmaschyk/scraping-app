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

    @PostMapping("/scrape")
    public ScrapeResponseDto scrapeAndSaveJobs(@Valid @RequestBody ScrapeRequestDto request) {
            List<Job> jobs = scraperService.scrapeAndCreateJobs(
                    request.getJobFunctions()
            );
            if (jobs.isEmpty()) {
                return jobCreationService.createEmptyResponse("No jobs found during scraping");
            }
            int savedCount = jobIngestService.saveJobs(jobs);

            List<String> jobUrls = jobs.stream()
                    .map(Job::getJobPageUrl)
                    .collect(Collectors.toList());
            return jobCreationService.createSuccessResponse(jobUrls, savedCount);
    }
}
