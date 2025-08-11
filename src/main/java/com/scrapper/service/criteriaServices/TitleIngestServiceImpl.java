package com.scrapper.service.criteriaServices;

import com.scrapper.model.Job;
import com.scrapper.repository.job.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TitleIngestServiceImpl implements TitleIngestService {
    
    private final JobRepository jobRepository;
    
    @Override
    @Transactional
    public boolean saveTitleForJob(Long jobId, String title) {
        if (title == null || title.trim().isEmpty()) {
            log.debug("ℹ️ No title to save for job {}", jobId);
            return false;
        }
        
        try {
            log.info("💼 Saving title '{}' for job {}", title.trim(), jobId);
            
            Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
            
            job.setPositionName(title.trim());
            jobRepository.save(job);
            
            log.info("✅ Title saved successfully for job {}", jobId);
            return true;
            
        } catch (Exception e) {
            log.error("❌ Error saving title '{}' for job {}: {}", title, jobId, e.getMessage());
            return false;
        }
    }
    
    @Override
    @Transactional
    public void clearTitleForJob(Long jobId) {
        try {
            Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
            
            job.setPositionName(null);
            jobRepository.save(job);
            
            log.info("🗑️ Cleared title for job {}", jobId);
        } catch (Exception e) {
            log.error("❌ Error clearing title for job {}: {}", jobId, e.getMessage());
        }
    }
}

