package com.scrapper.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyUrlsResponseDto {
    private boolean success;
    private long fetchedAtEpochMs;
    private List<String> jobFunctions;
    private List<String> tags;
    private int count;
    private List<String> urls;

    public static ApplyUrlsResponseDto of(List<String> urls, List<String> jobFunctions,
                                        List<String> tags) {
        return ApplyUrlsResponseDto.builder()
            .success(true)
            .fetchedAtEpochMs(Instant.now().toEpochMilli())
            .jobFunctions(jobFunctions)
            .tags(tags)
            .count(urls != null ? urls.size() : 0)
            .urls(urls)
            .build();
    }
}
