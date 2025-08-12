package com.scrapper.service.criteriaServices;

import com.scrapper.model.Job;
import com.scrapper.repository.job.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ✅ РЕАЛІЗАЦІЯ: Сервіс для збереження опису вакансії
 * 
 * Цей сервіс реалізує логіку:
 * 1. Збереження опису вакансії в базу даних
 * 2. Валідація даних перед збереженням
 * 3. Логування результатів операцій
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DescriptionIngestServiceImpl implements DescriptionIngestService {

    private final JobRepository jobRepository;

    @Override
    public boolean saveDescription(Job job, String description) {
        if (job == null || job.getId() == 0 || description == null || description.trim().isEmpty()) {
            return false;
        }
        
        try {
            job.setDescription(description.trim());
            jobRepository.save(job);
            return true;
        } catch (Exception e) {
            log.error("❌ Error saving description for job ID: {}, error: {}", job.getId(), e.getMessage());
            return false;
        }
    }
}
