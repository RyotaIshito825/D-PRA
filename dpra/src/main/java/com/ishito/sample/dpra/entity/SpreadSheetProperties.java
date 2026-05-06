package com.ishito.sample.dpra.entity;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "google.spreadsheet")
public class SpreadSheetProperties {
    private String keyfile;
}
