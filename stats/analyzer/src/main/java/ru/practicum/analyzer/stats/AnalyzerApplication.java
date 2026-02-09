package ru.practicum.analyzer.stats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AnalyzerApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AnalyzerApplication.class, args);
        EventSimilarityProcessor eventSimilarityProcessor = context.getBean(EventSimilarityProcessor.class);
        UserActionProcessor userActionProcessor = context.getBean(UserActionProcessor.class);

        Runtime.getRuntime().addShutdownHook(new Thread(eventSimilarityProcessor::stop));
        Runtime.getRuntime().addShutdownHook(new Thread(userActionProcessor::stop));

        Thread eventSimilarityProcessorThread = new Thread(eventSimilarityProcessor);
        eventSimilarityProcessorThread.setName("EventSimilarityProcessorThread");
        eventSimilarityProcessorThread.start();

        Thread userActionProcessorThread = new Thread(userActionProcessor);
        userActionProcessorThread.setName("UserActionProcessorThread");
        userActionProcessorThread.start();
    }
}
