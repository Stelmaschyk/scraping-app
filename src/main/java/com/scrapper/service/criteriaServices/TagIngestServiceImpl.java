package com.scrapper.service.criteriaServices;

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
public class TagIngestServiceImpl implements TagIngestService {
    
    private final JobRepository jobRepository;
    
    @Override
    @Transactional
    public int saveTagsForJob(Long jobId, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            log.debug("ℹ️ No tags to save for job {}", jobId);
            return 0;
        }
        
        log.info("🏷️ Starting to save {} tags for job {}", tags.size(), jobId);
        
        int savedCount = 0;
        for (String tag : tags) {
            try {
                if (tag != null && !tag.trim().isEmpty()) {
                    Job job = jobRepository.findById(jobId)
                        .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
                    
                    job.addTag(tag.trim());
                    jobRepository.save(job);
                    savedCount++;
                }
            } catch (Exception e) {
                log.error("❌ Error saving tag '{}' for job {}: {}", tag, jobId, e.getMessage());
            }
        }
        
        log.info("✅ Tags saving completed for job {}. Saved: {}/{}", jobId, savedCount, tags.size());
        return savedCount;
    }
}

