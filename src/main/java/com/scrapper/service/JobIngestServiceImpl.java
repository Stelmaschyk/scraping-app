package com.scrapper.service;

import com.scrapper.model.Job;
import com.scrapper.model.JobFunction;
import com.scrapper.repository.job.JobRepository;
import com.scrapper.util.ScrapingSelectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobIngestServiceImpl implements JobIngestService {
    private final ApplyUrlScraperService applyUrlScraperService;
    private final OkHttpClient httpClient;
    private final JobRepository jobRepository;

    @Override
    public int scrapeAndSave(List<JobFunction> jobFunctions, List<String> requiredTags) {
        List<String> urls = applyUrlScraperService.fetchApplyUrls(jobFunctions, requiredTags);
        int saved = 0;
        for (String url : urls) {
            Document doc = fetch(url);
            if (doc == null) continue;
            Job job = parseJob(doc, url, firstFunctionName(jobFunctions));
            if (job != null) {
                try {
                    jobRepository.save(job);
                    saved++;
                } catch (Exception e) {
                    log.warn("Skip duplicate or error for {}: {}", url, e.getMessage());
                }
            }
        }
        return saved;
    }

    private Document fetch(String url) {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            String body = response.body() != null ? response.body().string() : null;
            if (body == null) return null;
            return Jsoup.parse(body, url);
        } catch (Exception e) {
            return null;
        }
    }

    private Job parseJob(Document doc, String jobUrl, String defaultFunction) {
        String title = firstText(doc, ScrapingSelectors.JOB_TITLE).orElse("N/A");
        String orgTitle = firstText(doc, ScrapingSelectors.ORG_NAME).orElse("N/A");
        String orgLink = firstAttr(doc, ScrapingSelectors.ORG_LINK, "href").orElse("");
        String logo = firstAttr(doc, ScrapingSelectors.ORG_LOGO, "src").orElse(null);
        String labor = firstText(doc, ScrapingSelectors.JOB_FUNCTION).orElse(defaultFunction);
        List<String> tags = allTexts(doc, ScrapingSelectors.TAGS);
        List<String> locations = allTexts(doc, ScrapingSelectors.LOCATION);
        String address = String.join(", ", new HashSet<>(locations));
        LocalDateTime posted = parsePosted(doc).orElse(LocalDateTime.now());
        String description = firstOuterHtml(doc, ScrapingSelectors.DESCRIPTION).orElse("");

        Job job = Job.builder()
            .positionName(title)
            .jobPageUrl(jobUrl)
            .organizationUrl(orgLink)
            .logoUrl(logo)
            .organizationTitle(orgTitle)
            .laborFunction(labor)
            .address(address)
            .postedDate(posted)
            .description(description)
            .build();
        tags.forEach(job::addTag);
        new ArrayList<>(new HashSet<>(locations)).forEach(job::addLocation);
        return job;
    }

    private Optional<String> firstText(Element root, String[] selectors) {
        for (String css : selectors) {
            Element el = root.selectFirst(css);
            if (el != null && !el.text().isBlank()) return Optional.of(el.text().trim());
        }
        return Optional.empty();
    }

    private Optional<String> firstAttr(Element root, String[] selectors, String attr) {
        for (String css : selectors) {
            Element el = root.selectFirst(css);
            if (el != null) {
                String abs = el.absUrl(attr);
                if (abs != null && !abs.isBlank()) return Optional.of(abs);
                String val = el.attr(attr);
                if (val != null && !val.isBlank()) return Optional.of(val);
            }
        }
        return Optional.empty();
    }

    private Optional<String> firstOuterHtml(Element root, String[] selectors) {
        for (String css : selectors) {
            Element el = root.selectFirst(css);
            if (el != null) return Optional.of(el.outerHtml());
        }
        return Optional.empty();
    }

    private List<String> allTexts(Element root, String[] selectors) {
        List<String> out = new ArrayList<>();
        for (String css : selectors) {
            root.select(css).eachText().forEach(t -> {
                if (t != null && !t.isBlank()) out.add(t.trim());
            });
        }
        return out;
    }

    private Optional<LocalDateTime> parsePosted(Document doc) {
        Element time = doc.selectFirst("time[datetime]");
        if (time != null) {
            try {
                return Optional.of(LocalDateTime.parse(time.attr("datetime"), DateTimeFormatter.ISO_DATE_TIME));
            } catch (DateTimeParseException ignored) {}
        }
        for (String css : ScrapingSelectors.POSTED_DATE) {
            Element el = doc.selectFirst(css);
            if (el != null) {
                String text = el.text().replace("on", "").trim();
                try {
                    LocalDate d = LocalDate.parse(text, DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH));
                    return Optional.of(d.atStartOfDay());
                } catch (DateTimeParseException ignored) {}
            }
        }
        return Optional.empty();
    }

    private String firstFunctionName(List<JobFunction> list) {
        return (list == null || list.isEmpty()) ? "Unknown" : list.get(0).getDisplayName();
    }
}
