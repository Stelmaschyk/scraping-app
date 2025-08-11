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
public class LocationIngestServiceImpl implements LocationIngestService {

    private final JobRepository jobRepository;

    @Override
    @Transactional
    public boolean saveLocationForJob(Long jobId, String location) {
        if (location == null || location.trim().isEmpty()) {
            log.debug("ℹ️ No location to save for job {}", jobId);
            return false;
        }

        try {
            log.info("📍 Saving location '{}' for job {}", location.trim(), jobId);

            Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));

            job.addLocation(location.trim());
            jobRepository.save(job);

            log.info("✅ Location saved successfully for job {}", jobId);
            return true;

        } catch (Exception e) {
            log.error("❌ Error saving location '{}' for job {}: {}", location, jobId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public void clearLocationForJob(Long jobId) {
        try {
            Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
            
            job.getLocations().clear();
            jobRepository.save(job);
            
            log.info("🗑️ Cleared location for job {}", jobId);
        } catch (Exception e) {
            log.error("❌ Error clearing location for job {}: {}", jobId, e.getMessage());
        }
    }
}

