package io.github.mrdaios.multikafka.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@ConditionalOnClass({KafkaTemplate.class})
@ConditionalOnProperty(value = "spring.kafka.multi.enable", havingValue = "true")
@EnableConfigurationProperties(MultikafkaProperties.class)
public class MultikafkaAutoConfiguration {

    @Bean
    public MultiKafkaBeanDefinitionRegistryPostProcessor multiKafkaBeanDefinitionRegistryPostProcessor() {
        return new MultiKafkaBeanDefinitionRegistryPostProcessor();
    }
}
