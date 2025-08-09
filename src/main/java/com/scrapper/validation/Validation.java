package com.scrapper.validation;

import com.scrapper.model.Job;
import java.util.function.Predicate;

public final class Validation {
    public static final Predicate<String> NOT_BLANK = value -> value != null && !value.trim().isEmpty();
    public static final Predicate<Job> IS_VALID = job ->
        NOT_BLANK.test(job.getPositionName()) &&
            NOT_BLANK.test(job.getJobPageUrl()) &&
            NOT_BLANK.test(job.getOrganizationTitle()) &&
            NOT_BLANK.test(job.getLaborFunction()) &&
            job.getPostedDate() != null;
}
