package com.scrapper.service;

import com.scrapper.model.JobFunction;
import java.util.List;

public interface JobIngestService {
    int scrapeAndSave(List<JobFunction> jobFunctions, List<String> requiredTags);
}
