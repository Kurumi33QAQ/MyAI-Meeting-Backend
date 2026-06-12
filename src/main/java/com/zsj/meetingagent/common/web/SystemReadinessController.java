package com.zsj.meetingagent.common.web;

import com.zsj.meetingagent.ai.config.AiModelProperties;
import com.zsj.meetingagent.common.result.ApiResponse;
import com.zsj.meetingagent.evaluation.config.EvaluationProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 项目运行环境自检接口。
 * 用于在本地联调或部署前快速确认数据库、缓存、AI 配置和评测数据集是否可用。
 */
@RestController
public class SystemReadinessController {

    private final DataSource dataSource;
    private final MongoTemplate mongoTemplate;
    private final StringRedisTemplate redisTemplate;
    private final AiModelProperties aiModelProperties;
    private final Environment environment;
    private final EvaluationProperties evaluationProperties;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public SystemReadinessController(DataSource dataSource,
                                     MongoTemplate mongoTemplate,
                                     StringRedisTemplate redisTemplate,
                                     AiModelProperties aiModelProperties,
                                     Environment environment,
                                     EvaluationProperties evaluationProperties,
                                     ResourceLoader resourceLoader,
                                     ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.mongoTemplate = mongoTemplate;
        this.redisTemplate = redisTemplate;
        this.aiModelProperties = aiModelProperties;
        this.environment = environment;
        this.evaluationProperties = evaluationProperties;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/api/system/readiness")
    public ApiResponse<Map<String, Object>> readiness() {
        Map<String, Object> dependencies = new LinkedHashMap<>();
        DependencyCheck mysql = checkMysql();
        DependencyCheck mongo = checkMongo();
        DependencyCheck redis = checkRedis();
        DependencyCheck ai = checkAiConfig();
        DependencyCheck evaluation = checkEvaluationDataset();

        dependencies.put("mysql", mysql.body());
        dependencies.put("mongodb", mongo.body());
        dependencies.put("redis", redis.body());
        dependencies.put("ai", ai.body());
        dependencies.put("evaluation", evaluation.body());

        boolean coreReady = mysql.up() && mongo.up() && redis.up() && ai.up() && evaluation.up();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", coreReady ? "UP" : "DEGRADED");
        body.put("service", "meeting-agent-backend");
        body.put("checkedAt", Instant.now().toString());
        body.put("dependencies", dependencies);
        return ApiResponse.success(body);
    }

    private DependencyCheck checkMysql() {
        try (Connection connection = dataSource.getConnection()) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("databaseProduct", connection.getMetaData().getDatabaseProductName());
            detail.put("url", maskJdbcUrl(connection.getMetaData().getURL()));
            return DependencyCheck.up("结构化主数据连接正常", detail);
        } catch (Exception e) {
            return DependencyCheck.down("结构化主数据连接失败：" + rootMessage(e));
        }
    }

    private DependencyCheck checkMongo() {
        try {
            mongoTemplate.executeCommand(new Document("ping", 1));
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("databaseName", mongoTemplate.getDb().getName());
            return DependencyCheck.up("AI 会话和运行快照存储连接正常", detail);
        } catch (Exception e) {
            return DependencyCheck.down("MongoDB 连接失败：" + rootMessage(e));
        }
    }

    private DependencyCheck checkRedis() {
        RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
        if (connectionFactory == null) {
            return DependencyCheck.down("Redis 连接工厂未初始化");
        }
        try (RedisConnection connection = connectionFactory.getConnection()) {
            String pong = connection.ping();
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("ping", pong);
            detail.put("usage", "Sa-Token 登录态、AI 限流、Single-flight 和面试热态缓存");
            return DependencyCheck.up("Redis 缓存和限流组件连接正常", detail);
        } catch (Exception e) {
            return DependencyCheck.down("Redis 连接失败：" + rootMessage(e));
        }
    }

    private DependencyCheck checkAiConfig() {
        String apiKey = environment.getProperty("spring.ai.openai.api-key", "");
        String baseUrl = environment.getProperty("spring.ai.openai.base-url", "");
        boolean mockEnabled = aiModelProperties.isMockEnabled();
        boolean apiKeyConfigured = StringUtils.hasText(apiKey)
                && !apiKey.contains("dummy")
                && !apiKey.contains("test");
        boolean baseUrlConfigured = StringUtils.hasText(baseUrl);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("provider", aiModelProperties.getProvider());
        detail.put("defaultModel", aiModelProperties.getDefaultModel());
        detail.put("mockEnabled", mockEnabled);
        detail.put("apiKeyConfigured", apiKeyConfigured);
        detail.put("baseUrlConfigured", baseUrlConfigured);

        /*
         * Mock 模式下不强制要求真实 Key；真实模型模式下必须先有 Key 和 Base URL。
         * 这里不做实际大模型调用，避免自检接口本身消耗额度。
         */
        if (mockEnabled || (apiKeyConfigured && baseUrlConfigured)) {
            return DependencyCheck.up(mockEnabled ? "AI mock 模式可用" : "真实 AI 调用配置完整", detail);
        }
        return DependencyCheck.down("真实 AI 调用缺少 API Key 或 Base URL", detail);
    }

    private DependencyCheck checkEvaluationDataset() {
        try {
            Resource resource = resourceLoader.getResource(evaluationProperties.getDefaultDatasetPath());
            int caseCount;
            try (InputStream inputStream = resource.getInputStream()) {
                caseCount = objectMapper.readValue(inputStream, new TypeReference<List<Map<String, Object>>>() {
                }).size();
            }
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("datasetPath", evaluationProperties.getDefaultDatasetPath());
            detail.put("caseCount", caseCount);
            return caseCount > 0
                    ? DependencyCheck.up("评测数据集可加载", detail)
                    : DependencyCheck.down("评测数据集为空", detail);
        } catch (Exception e) {
            return DependencyCheck.down("评测数据集加载失败：" + rootMessage(e));
        }
    }

    private String maskJdbcUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }
        int credentialIndex = url.indexOf('@');
        if (credentialIndex < 0) {
            return url;
        }
        return "jdbc:***" + url.substring(credentialIndex);
    }

    private String rootMessage(Exception e) {
        Throwable current = e;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return StringUtils.hasText(current.getMessage())
                ? current.getMessage()
                : current.getClass().getSimpleName();
    }

    private record DependencyCheck(boolean up, Map<String, Object> body) {

        static DependencyCheck up(String message, Map<String, Object> detail) {
            Map<String, Object> body = base("UP", message);
            body.putAll(detail);
            return new DependencyCheck(true, body);
        }

        static DependencyCheck down(String message) {
            return down(message, Map.of());
        }

        static DependencyCheck down(String message, Map<String, Object> detail) {
            Map<String, Object> body = base("DOWN", message);
            body.putAll(detail);
            return new DependencyCheck(false, body);
        }

        private static Map<String, Object> base(String status, String message) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", status);
            body.put("message", message);
            return body;
        }
    }
}
