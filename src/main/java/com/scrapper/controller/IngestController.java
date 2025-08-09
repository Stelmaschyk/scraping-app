package com.scrapper.controller;

import com.scrapper.model.JobFunction;
import com.scrapper.service.JobIngestService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/scrap")
@RequiredArgsConstructor
public class IngestController {
    private final JobIngestService jobIngestService;

    @PostMapping("/ingest")
    public Map<String, Object> ingest(@RequestBody(required = false) Map<String, Object> body) {
        List<String> jf = body != null && body.get("jobFunctions") instanceof List<?> l1
            ? l1.stream().map(Object::toString).collect(Collectors.toList()) : List.of();
        List<String> tags = body != null && body.get("tags") instanceof List<?> l2
            ? l2.stream().map(Object::toString).collect(Collectors.toList()) : List.of();

        List<JobFunction> functions = jf.isEmpty() ? List.of() : jf.stream()
            .map(JobFunction::fromDisplayName)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

        int saved = jobIngestService.scrapeAndSave(functions, tags);
        return Map.of(
            "success", true,
            "saved", saved,
            "jobFunctions", functions.stream().map(JobFunction::getDisplayName).toList(),
            "tags", tags
        );
    }
}
