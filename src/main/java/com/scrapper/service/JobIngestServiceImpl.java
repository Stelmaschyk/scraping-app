package com.scrapper.service;

import com.scrapper.model.Job;
import com.scrapper.repository.job.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobIngestServiceImpl implements JobIngestService {

    private final JobRepository jobRepository;

    @Override
    @Transactional
    public int saveJobs(List<Job> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return 0;
        }
        
        int savedCount = 0;
        for (Job job : jobs) {
            try {
                if (!jobRepository.existsByJobPageUrl(job.getJobPageUrl())) {
                    jobRepository.save(job);
                    savedCount++;
                }
            } catch (Exception e) {
                log.error("❌ Error saving job {}: {}", job.getPositionName(), e.getMessage());
            }
        }
        log.info("✅ Saved {}/{} jobs", savedCount, jobs.size());
        return savedCount;
    }
}
