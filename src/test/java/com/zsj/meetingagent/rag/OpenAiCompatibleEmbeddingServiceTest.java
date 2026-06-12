package com.zsj.meetingagent.rag;

import com.zsj.meetingagent.rag.service.EmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.rag.vector.dimension=8",
        "app.rag.vector.embedding-mock-enabled=true"
})
class OpenAiCompatibleEmbeddingServiceTest {

    @Autowired
    private EmbeddingService embeddingService;

    @Test
    void mockEmbeddingIsDeterministicAndNormalized() {
        var first = embeddingService.embed("Spring Boot Redis 项目经历");
        var second = embeddingService.embed("Spring Boot Redis 项目经历");

        assertThat(first).hasSize(8);
        assertThat(first).isEqualTo(second);
        double norm = Math.sqrt(first.stream().mapToDouble(value -> value * value).sum());
        assertThat(norm).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
    }
}
