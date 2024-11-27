package com.quartz.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class PropertiesLoader {
    public static Properties loadProperties(String filePath) throws IOException {
        Properties properties = new Properties();
        InputStream inStream = Files.newInputStream(Paths.get(filePath));
        properties.load(inStream);
        inStream.close();
        return properties;
    }
}