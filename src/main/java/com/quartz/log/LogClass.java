package com.quartz.log;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

public class LogClass {

//    public static void initializeYourLogger(String fileName, String pattern) {
//
//        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
//
//        builder.setStatusLevel(Level.DEBUG);
//        builder.setConfigurationName("DefaultLogger");
//
//        // create a console appender
//        AppenderComponentBuilder appenderBuilder = builder.newAppender("Console", "CONSOLE").addAttribute("target",
//                ConsoleAppender.Target.SYSTEM_OUT);
//        appenderBuilder.add(builder.newLayout("PatternLayout")
//                .addAttribute("pattern", pattern));
//        RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.DEBUG);
//        rootLogger.add(builder.newAppenderRef("Console"));
//
//        builder.add(appenderBuilder);
//
//        // create a rolling file appender
//        LayoutComponentBuilder layoutBuilder = builder.newLayout("PatternLayout")
//                .addAttribute("pattern", pattern);
//        ComponentBuilder triggeringPolicy = builder.newComponent("Policies")
//                .addComponent(builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", "1KB"));
//        appenderBuilder = builder.newAppender("LogToRollingFile", "RollingFile")
//                .addAttribute("fileName", fileName)
//                .addAttribute("filePattern", fileName+"-%d{MM-dd-yy-HH-mm-ss}.log.")
//                .add(layoutBuilder)
//                .addComponent(triggeringPolicy);
//        builder.add(appenderBuilder);
//        rootLogger.add(builder.newAppenderRef("LogToRollingFile"));
//        builder.add(rootLogger);
//        Configurator.reconfigure(builder.build());
//    }
}