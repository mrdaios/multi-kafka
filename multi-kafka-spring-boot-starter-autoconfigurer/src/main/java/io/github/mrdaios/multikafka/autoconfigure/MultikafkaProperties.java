package io.github.mrdaios.multikafka.autoconfigure;


import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "spring.kafka.multi")
public class MultikafkaProperties {
    private boolean enable;
    private Map<String, KafkaProperties> kafka;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public Map<String, KafkaProperties> getKafka() {
        return kafka;
    }

    public void setKafka(Map<String, KafkaProperties> kafka) {
        this.kafka = kafka;
    }
}
