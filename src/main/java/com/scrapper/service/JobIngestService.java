package com.scrapper.service;

import com.scrapper.model.Job;
import java.util.List;

public interface JobIngestService {
    int saveJobs(List<Job> jobs);
}
