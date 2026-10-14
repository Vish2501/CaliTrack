package com.calitrack.support;

import com.calitrack.service.OpenAIChatClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestPropertySource(properties = "calitrack.coach.async-enabled=true")
public abstract class AbstractKafkaIntegrationTest extends AbstractIntegrationTest {

    @Container
    protected static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));

    @DynamicPropertySource
    static void configureKafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    // The consumer runs in every Kafka test context; never let it call the real OpenAI API.
    @MockBean
    protected OpenAIChatClient openAIChatClient;
}
