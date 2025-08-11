package com.scrapper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapeResponseDto {
    private boolean success;
    private String message;
    private int totalJobsFound;
    private int jobsSaved;
    private List<String> jobUrls;
}
