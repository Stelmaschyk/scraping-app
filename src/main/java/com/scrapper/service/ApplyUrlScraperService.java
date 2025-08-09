package com.scrapper.service;

import com.scrapper.model.JobFunction;
import java.util.List;

public interface ApplyUrlScraperService {
    List<String> fetchApplyUrls(List<JobFunction> jobFunctions, List<String> requiredTags);
}