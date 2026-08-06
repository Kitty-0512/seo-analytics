package com.example.seoanalytics.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Platform {
    GSC("GSC"),
    BING("BING");

    @EnumValue
    @JsonValue
    private final String value;

    Platform(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
