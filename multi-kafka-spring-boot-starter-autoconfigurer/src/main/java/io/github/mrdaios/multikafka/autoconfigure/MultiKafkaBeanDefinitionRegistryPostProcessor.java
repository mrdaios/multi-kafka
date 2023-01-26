package io.github.mrdaios.multikafka.autoconfigure;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MultiKafkaBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private static final String SPRING_KAFKA_MULTI_PREFIX = "spring.kafka.multi";
    private Environment environment;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        markPrimaryKafkaConfig(registry);
        registerDynamicKafkaConfig(registry);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    private void markPrimaryBeanDefinition(BeanDefinitionRegistry registry, Class<?> clazz) {
        String[] beanDefinitionNames = registry.getBeanDefinitionNames();
        // search kafka config
        boolean hasPrimary = false;
        List<BeanDefinition> beanDefinitions = new ArrayList<>();
        for (String beanDefinitionName : beanDefinitionNames) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanDefinitionName);
            // check type
            Class<?> resolvableTypeClass = beanDefinition.getResolvableType().getRawClass();
            if (resolvableTypeClass == clazz) {
                beanDefinitions.add(beanDefinition);
                if (beanDefinition.isPrimary()) {
                    hasPrimary = true;
                    break;
                }
            }
            // check annotation config
            else if (beanDefinition instanceof AnnotatedBeanDefinition) {
                Optional<Boolean> hasBeanDefinition = Optional.ofNullable(((AnnotatedBeanDefinition) beanDefinition).getFactoryMethodMetadata()).map(value -> value.getReturnTypeName().equals(clazz.getName()));
                if (hasBeanDefinition.isPresent() && hasBeanDefinition.get()) {
                    beanDefinitions.add(beanDefinition);
                    if (beanDefinition.isPrimary()) {
                        hasPrimary = true;
                        break;
                    }
                }
            }
        }
        // if not existed setPrimary
        if (!hasPrimary && beanDefinitions.size() > 0) {
            beanDefinitions.stream().findFirst().get().setPrimary(true);
        }
    }

    private void markPrimaryKafkaConfig(BeanDefinitionRegistry registry) {
        markPrimaryBeanDefinition(registry, KafkaTemplate.class);
        markPrimaryBeanDefinition(registry, KafkaProperties.class);
        markPrimaryBeanDefinition(registry, ProducerFactory.class);
        markPrimaryBeanDefinition(registry, ConsumerFactory.class);
    }

    private void registerDynamicKafkaConfig(BeanDefinitionRegistry registry) {
        final ObjectProvider<MultiKafkaProducerFactoryCustomizer> kafkaProducerFactoryCustomizers;
        if (registry instanceof DefaultListableBeanFactory) {
            kafkaProducerFactoryCustomizers = ((DefaultListableBeanFactory) registry).getBeanProvider(MultiKafkaProducerFactoryCustomizer.class);
        } else {
            kafkaProducerFactoryCustomizers = null;
        }
        Binder binder = Binder.get(environment);
        // parse Kafka config
        BindResult<MultikafkaProperties> propertiesBindResult = binder.bind(SPRING_KAFKA_MULTI_PREFIX, Bindable.of(MultikafkaProperties.class));
        if (propertiesBindResult.isBound()) {
            MultikafkaProperties multikafkaProperties = propertiesBindResult.get();
            for (Map.Entry<String, KafkaProperties> kafkaPropertiesEntry : multikafkaProperties.getKafka().entrySet()) {
                //register KafkaProperties
                String propertiesBeanName = kafkaPropertiesEntry.getKey() + "KafkaProperties";
                KafkaProperties kafkaProperties = kafkaPropertiesEntry.getValue();
                if (!registry.containsBeanDefinition(propertiesBeanName)) {
                    BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(KafkaProperties.class, () -> kafkaProperties);
                    registry.registerBeanDefinition(propertiesBeanName, beanDefinitionBuilder.getBeanDefinition());
                }
                //register KafkaTemplate
                String templateBeanName = kafkaPropertiesEntry.getKey() + "KafkaTemplate";
                if (!registry.containsBeanDefinition(templateBeanName)) {
                    BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(KafkaTemplate.class, () -> new KafkaTemplate<>(kafkaProducerFactory(kafkaProducerFactoryCustomizers, kafkaPropertiesEntry)));
                    registry.registerBeanDefinition(templateBeanName, beanDefinitionBuilder.getBeanDefinition());
                }
                //register KafkaListenerContainerFactory
                String listenerContainerFactoryBeanName = kafkaPropertiesEntry.getKey() + "KafkaListenerContainerFactory";
                if (!registry.containsBeanDefinition(listenerContainerFactoryBeanName)) {
                    BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(KafkaListenerContainerFactory.class, () -> {
                        ConcurrentKafkaListenerContainerFactory<Integer, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
                        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties()));
                        factory.setConcurrency(10);
                        factory.getContainerProperties().setPollTimeout(3000);
                        return factory;
                    });
                    registry.registerBeanDefinition(listenerContainerFactoryBeanName, beanDefinitionBuilder.getBeanDefinition());
                }
            }
        }
    }

    private ProducerFactory<?, ?> kafkaProducerFactory(ObjectProvider<MultiKafkaProducerFactoryCustomizer> customizers, Map.Entry<String, KafkaProperties> kafkaPropertiesEntry) {
        DefaultKafkaProducerFactory<?, ?> factory = new DefaultKafkaProducerFactory<>(kafkaPropertiesEntry.getValue().buildProducerProperties());
        String transactionIdPrefix = kafkaPropertiesEntry.getValue().getProducer().getTransactionIdPrefix();
        if (transactionIdPrefix != null) {
            factory.setTransactionIdPrefix(transactionIdPrefix);
        }
        customizers.orderedStream().forEach((customizer) -> customizer.customize(kafkaPropertiesEntry.getKey(), factory));
        return factory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
