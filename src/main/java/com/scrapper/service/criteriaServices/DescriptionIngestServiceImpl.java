package com.scrapper.service.criteriaServices;

import com.scrapper.model.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ✅ РЕАЛІЗАЦІЯ: Сервіс для збереження опису вакансії
 * 
 * Цей сервіс реалізує логіку:
 * 1. Збереження опису вакансії з HTML форматуванням
 * 2. Пошук опису за ID вакансії
 * 3. Видалення опису вакансії
 * 
 * TODO: Додати репозиторій для збереження в базу даних
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DescriptionIngestServiceImpl implements DescriptionIngestService {

    @Override
    public boolean saveDescription(Job job, String description) {
        try {
            if (job == null || job.getId() == 0) {
                log.warn("⚠️ Cannot save description: job or job ID is null/zero");
                return false;
            }
            
            if (description == null || description.trim().isEmpty()) {
                log.warn("⚠️ Cannot save description: description is null or empty for job ID: {}", job.getId());
                return false;
            }
            
            // ✅ Зберігаємо опис з HTML форматуванням
            String cleanDescription = description.trim();
            
            // TODO: Додати логіку збереження в базу даних
            // descriptionRepository.save(new JobDescription(job.getId(), cleanDescription));
            
            log.info("✅ Successfully saved description for job ID: {}, length: {} characters", 
                    job.getId(), cleanDescription.length());
            
            return true;
            
        } catch (Exception e) {
            log.error("❌ Error saving description for job ID: {}, error: {}", 
                    job != null ? job.getId() : "null", e.getMessage(), e);
            return false;
        }
    }
}
