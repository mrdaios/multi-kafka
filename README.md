# multi-kafka

项目中不同的业务可能会使用多个kafka，按默认的Kafka配置，最多是支持消费者和生产者使用不同的Kafka，如果两个生产者使用不同的Kafka则需要自定义配置，生成对应的bean。

## 使用配置

### 引入依赖

```xml

<dependency>
    <groupId>io.github.mrdaios</groupId>
    <artifactId>multi-kafka-spring-boot-starter</artifactId>
    <version>0.0.3</version>
</dependency>
```

配置如下:

```yml
spring:
  kafka:
    bootstrap-servers: xxx.xxx.xxx
    multi:
      enable: true  # 开启多套kafka配置
      kafka:
        test: # 指定配置key
          bootstrap-servers: ${spring.kafka.bootstrap-servers}
```

使用如下:

```java

@Component
public class TestService {

    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    @Qualifier("testKafkaTemplate") // bean名称规则为Key+ClassName
    public void setKafkaTemplate(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
}
```

## 自定义配置

### MultiKafkaProducerFactoryCustomizer

```java

@Component
public class TestKafkaProducerFactoryCustomizer implements MultiKafkaProducerFactoryCustomizer {

    @Override
    public void customize(String propertyKey, DefaultKafkaProducerFactory<?, ?> producerFactory) {
        Map<String, Object> properties = producerFactory.getConfigurationProperties();
        // 可根据propertyKey统一配置默认参数
        if (!properties.containsKey(ProducerConfig.PARTITIONER_CLASS_CONFIG) && StringUtils.equals("test", propertyKey)) {
            Map<String, Object> updateConfigs = new HashMap<>();
            updateConfigs.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, "xxx");
            producerFactory.updateConfigs(updateConfigs);
        }
    }
}
```

### 参考链接

- [Kafka多生产者消费者自动配置](https://juejin.cn/post/7169910747810496543)