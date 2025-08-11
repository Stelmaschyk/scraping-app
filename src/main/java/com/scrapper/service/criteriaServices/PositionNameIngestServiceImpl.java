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
public class PositionNameIngestServiceImpl implements PositionNameIngestService {
    
    private final JobRepository jobRepository;
    
    @Override
    @Transactional
    public boolean savePositionNameForJob(Long jobId, String positionName) {
        if (positionName == null || positionName.trim().isEmpty()) {
            log.debug("ℹ️ No position name to save for job {}", jobId);
            return false;
        }
        
        try {
            log.info("💼 Saving position name '{}' for job {}", positionName.trim(), jobId);
            
            Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
            
            job.setPositionName(positionName.trim());
            jobRepository.save(job);
            
            log.info("✅ Position name saved successfully for job {}", jobId);
            return true;
            
        } catch (Exception e) {
            log.error("❌ Error saving position name '{}' for job {}: {}", positionName, jobId, e.getMessage());
            return false;
        }
    }
    
    @Override
    @Transactional
    public void clearPositionNameForJob(Long jobId) {
        try {
            Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
            
            job.setPositionName(null);
            jobRepository.save(job);
            
            log.info("🗑️ Cleared position name for job {}", jobId);
        } catch (Exception e) {
            log.error("❌ Error clearing position name for job {}: {}", jobId, e.getMessage());
        }
    }
}
