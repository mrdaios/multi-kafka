# multi-kafka
支持配置多套kafka

配置如下:
```yml
spring:
  kafka:
    bootstrap-servers: xxx.xxx.xxx
    multi:
      enable: true  # 开启kafka配置
      kafka:
        test:
          bootstrap-servers: xxx.xxx.xxx
```

使用如下:
```java
public class TestService {
    
    @Autowired
    @Qualifier("testKafkaTemplate")
    public void setKafkaTemplate(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
}
```

### 参考链接
- [Kafka多生产者消费者自动配置](https://juejin.cn/post/7169910747810496543)