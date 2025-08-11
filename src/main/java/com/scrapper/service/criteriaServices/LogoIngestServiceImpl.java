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
public class LogoIngestServiceImpl implements LogoIngestService {
    
    private final JobRepository jobRepository;
    
    @Override
    @Transactional
    public boolean saveLogoForJob(Long jobId, String logoUrl) {
        if (logoUrl == null || logoUrl.trim().isEmpty()) {
            log.debug("ℹ️ No logo URL to save for job {}", jobId);
            return false;
        }
        
        try {
            log.info("🖼️ Saving logo URL '{}' for job {}", logoUrl.trim(), jobId);
            
            Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
            
            job.setLogoUrl(logoUrl.trim());
            jobRepository.save(job);
            
            log.info("✅ Logo saved successfully for job {}", jobId);
            return true;
            
        } catch (Exception e) {
            log.error("❌ Error saving logo URL '{}' for job {}: {}", logoUrl, jobId, e.getMessage());
            return false;
        }
    }
    
    @Override
    @Transactional
    public void clearLogoForJob(Long jobId) {
        try {
            Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
            
            job.setLogoUrl(null);
            jobRepository.save(job);
            
            log.info("🗑️ Cleared logo for job {}", jobId);
        } catch (Exception e) {
            log.error("❌ Error clearing logo for job {}: {}", jobId, e.getMessage());
        }
    }
}

