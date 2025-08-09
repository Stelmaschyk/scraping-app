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

    public static String buildFilterUrl(String baseUrl, List<JobFunction> jobFunctions) {
        if (jobFunctions == null || jobFunctions.isEmpty()) return baseUrl;
        try {
            Map<String, Object> filter = new HashMap<>();
            filter.put("job_functions", jobFunctions.stream().map(JobFunction::getDisplayName).collect(Collectors.toList()));
            String json = MAPPER.writeValueAsString(filter);
            String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
            return baseUrl + (baseUrl.contains("?") ? "&" : "?") + "filter=" + encoded;
        } catch (Exception e) {
            throw new IllegalStateException("Could`t build filter URL", e);
        }
    }
}
