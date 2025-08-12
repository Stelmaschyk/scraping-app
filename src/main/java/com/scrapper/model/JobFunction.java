package com.scrapper.model;

import com.scrapper.validation.Validation;
import java.util.Arrays;
import java.util.Optional;

public enum JobFunction {
    DESIGN("Design"),
    IT("IT"),
    LEGAL("Legal"),
    MARKETING_COMMUNICATIONS("Marketing & Communications"),
    OPERATIONS("Operations"),
    OTHER_ENGINEERING("Other Engineering"),
    PEOPLE_HR("People & HR"),
    PRODUCT("Product"),
    QUALITY_ASSURANCE("Quality Assurance"),
    SALES_BUSINESS_DEVELOPMENT("Sales & Business Development"),
    SOFTWARE_ENGINEERING("Software Engineering");

    private final String displayName;

    JobFunction(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }


}
