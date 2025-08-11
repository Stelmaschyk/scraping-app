package com.scrapper.service.criteriaServices;

import java.util.List;

public interface TagIngestService {
    
    /**
     * Зберігає теги для вакансії
     * @param jobId ID вакансії
     * @param tags список тегів
     * @return кількість збережених тегів
     */
    int saveTagsForJob(Long jobId, List<String> tags);
}

