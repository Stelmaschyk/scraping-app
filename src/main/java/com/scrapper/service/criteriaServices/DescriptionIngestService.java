package com.scrapper.service.criteriaServices;

import com.scrapper.model.Job;

/**
 * ✅ НОВИЙ СЕРВІС: Сервіс для збереження опису вакансії
 * 
 * Цей сервіс відповідає за:
 * 1. Скрапінг опису вакансії з картки вакансії
 * 2. Збереження опису з HTML форматуванням
 * 3. Зв'язування опису з основною вакансією
 */
public interface DescriptionIngestService {
    
    /**
     * Зберігає опис вакансії
     * 
     * @param job основна вакансія
     * @param description опис вакансії з HTML форматуванням
     * @return true якщо опис успішно збережено
     */
    boolean saveDescription(Job job, String description);
    
    /**
     * Знаходить опис вакансії за ID вакансії
     * 
     * @param jobId ID вакансії
     * @return опис вакансії або null якщо не знайдено
     */
    String findDescriptionByJobId(Long jobId);
    
    /**
     * Видаляє опис вакансії
     * 
     * @param jobId ID вакансії
     * @return true якщо опис успішно видалено
     */
    boolean deleteDescription(Long jobId);
}
