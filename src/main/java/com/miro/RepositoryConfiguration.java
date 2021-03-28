package com.miro;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoryConfiguration {

    @Bean
    public WidgetRepository repository() {
        return new WidgetMainRepository();
    }
}
