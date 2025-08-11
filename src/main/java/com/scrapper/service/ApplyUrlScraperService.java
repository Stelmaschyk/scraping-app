package com.scrapper.service;

import com.scrapper.model.Job;
import java.util.List;

public interface ApplyUrlScraperService {
    List<String> fetchApplyUrls(List<String> jobFunctions, List<String> requiredTags);
    
    /**
     * Скрапити та створити Job об'єкти з реальними даними
     */
    List<Job> scrapeAndCreateJobs(List<String> jobFunctions, List<String> requiredTags);
}


