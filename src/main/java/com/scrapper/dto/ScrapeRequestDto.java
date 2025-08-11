package com.scrapper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapeRequestDto {
    @NotEmpty(message = "Job functions cannot be empty")
    private List<@NotBlank(message = "Job function cannot be blank") String> jobFunctions;
    private List<String> tags;
}
