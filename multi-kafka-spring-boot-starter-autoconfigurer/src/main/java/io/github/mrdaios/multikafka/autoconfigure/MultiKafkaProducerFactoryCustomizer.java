package io.github.mrdaios.multikafka.autoconfigure;

import org.springframework.kafka.core.DefaultKafkaProducerFactory;

@FunctionalInterface
public interface MultiKafkaProducerFactoryCustomizer {
    void customize(String propertyKey, DefaultKafkaProducerFactory<?, ?> producerFactory);
}