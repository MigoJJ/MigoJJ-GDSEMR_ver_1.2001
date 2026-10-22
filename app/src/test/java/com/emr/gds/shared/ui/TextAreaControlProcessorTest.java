package com.emr.gds.shared.ui;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextAreaControlProcessorTest {

    @Test
    void expandsKnownAbbreviations() {
        Map<String, String> abbrevs = Map.of(
                "bp", "BP 120/80 mmHg",
                "dm", "Diabetes Mellitus",
                "htn", "Hypertension"
        );

        String input = "Patient with :htn and :dm. Checked :bp.";
        String result = TextAreaControlProcessor.expandAbbreviations(input, abbrevs);

        assertEquals("Patient with Hypertension and Diabetes Mellitus. Checked BP 120/80 mmHg.", result);
    }

    @Test
    void expandsCurrentDateMacro() {
        String input = "Visit date: :cd";
        String expectedDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        String result = TextAreaControlProcessor.expandAbbreviations(input, Map.of());

        assertEquals("Visit date: " + expectedDate, result);
    }

    @Test
    void leavesUnknownAbbreviationsUntouched() {
        String input = "Unknown code: :xyz123";
        String result = TextAreaControlProcessor.expandAbbreviations(input, Map.of("bp", "Blood Pressure"));

        assertEquals("Unknown code: :xyz123", result);
    }

    @Test
    void handlesNullOrEmptyStringsGracefully() {
        assertEquals("", TextAreaControlProcessor.expandAbbreviations(null, Map.of()));
        assertEquals("", TextAreaControlProcessor.expandAbbreviations("", Map.of()));
    }
}
