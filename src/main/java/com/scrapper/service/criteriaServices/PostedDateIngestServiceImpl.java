package com.scrapper.service.criteriaServices;

import com.scrapper.model.Job;
import com.scrapper.repository.job.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostedDateIngestServiceImpl implements PostedDateIngestService {
    
    private final JobRepository jobRepository;
    
    @Override
    @Transactional
    public boolean savePostedDateForJob(Long jobId, LocalDateTime postedDate) {
        if (postedDate == null) {
            log.debug("ℹ️ No posted date to save for job {}", jobId);
            return false;
        }
        
        try {
            log.info("📅 Saving posted date '{}' (Unix: {}) for job {}", 
                    postedDate, postedDate.toEpochSecond(ZoneOffset.UTC), jobId);
            
            Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
            
            // ✅ ВИПРАВЛЕНО: Конвертуємо LocalDateTime в Unix Timestamp
            job.setPostedDateFromLocalDateTime(postedDate);
            jobRepository.save(job);
            
            log.info("✅ Posted date saved successfully for job {} (Unix: {})", 
                    jobId, job.getPostedDate());
            return true;
            
        } catch (Exception e) {
            log.error("❌ Error saving posted date '{}' for job {}: {}", postedDate, jobId, e.getMessage());
            return false;
        }
    }
}

