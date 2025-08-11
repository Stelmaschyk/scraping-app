package com.scrapper.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrapper.model.JobFunction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TechstarsFilterUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TechstarsFilterUtil() {}

    public static String buildFilterUrl(String baseUrl, List<String> jobFunctions) {
        if (jobFunctions == null || jobFunctions.isEmpty()) return baseUrl;
        try {
            Map<String, Object> filter = new HashMap<>();
            filter.put("job_functions", jobFunctions);
            String json = MAPPER.writeValueAsString(filter);
            String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
            return baseUrl + (baseUrl.contains("?") ? "&" : "?") + "filter=" + encoded;
        } catch (Exception e) {
            throw new IllegalStateException("Could`t build filter URL", e);
        }
    }
    
    /**
     * Конвертує список рядків у список JobFunction enum значень
     */
    public static List<JobFunction> convertToJobFunctions(List<String> jobFunctionStrings) {
        if (jobFunctionStrings == null || jobFunctionStrings.isEmpty()) {
            return List.of();
        }
        
        return jobFunctionStrings.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .map(name -> {
                    try {
                        return JobFunction.valueOf(name);
                    } catch (IllegalArgumentException e) {
                        // Якщо рядок не відповідає enum значенню, спробуємо знайти по displayName
                        return JobFunction.fromDisplayName(name).orElse(null);
                    }
                })
                .filter(jobFunction -> jobFunction != null)
                .collect(Collectors.toList());
    }
    
    /**
     * Конвертує список JobFunction enum значень у список рядків
     */
    public static List<String> convertToStrings(List<JobFunction> jobFunctions) {
        if (jobFunctions == null || jobFunctions.isEmpty()) {
            return List.of();
        }
        
        return jobFunctions.stream()
                .map(JobFunction::getDisplayName)
                .collect(Collectors.toList());
    }
}
