package com.quartz.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigProperties {

    @Value("${app.readFromExternalProperties:false}") //default is false
    private boolean readFromExternalProperties;

    public boolean isReadFromExternalProperties() {
        return readFromExternalProperties;
    }
}

