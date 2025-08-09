package com.scrapper.controller;

import com.scrapper.dto.ApplyUrlsResponseDto;
import com.scrapper.model.JobFunction;
import com.scrapper.service.ApplyUrlScraperService;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ScrapeController {
    private final ApplyUrlScraperService scraperService;

    @GetMapping("/apply-urls")
    public ApplyUrlsResponseDto applyUrls(
        @RequestParam(name = "jobFunctions", required = false) String jobFunctions,
        @RequestParam(name = "tags", required = false) String tags
    ) {
        List<JobFunction> functions = parseFunctions(jobFunctions);
        List<String> requiredTags = parseTags(tags);
        List<String> urls = scraperService.fetchApplyUrls(functions, requiredTags);
        List<String> fn = functions.stream().map(JobFunction::getDisplayName).toList();
        return ApplyUrlsResponseDto.of(urls, fn, requiredTags);
    }

    private List<JobFunction> parseFunctions(String param) {
        if (param == null || param.isBlank()) return List.of();
        return Arrays.stream(param.split(","))
            .map(String::trim)
            .map(JobFunction::fromDisplayName)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    private List<String> parseTags(String param) {
        if (param == null || param.isBlank()) return List.of();
        return Arrays.stream(param.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toList());
    }
}
