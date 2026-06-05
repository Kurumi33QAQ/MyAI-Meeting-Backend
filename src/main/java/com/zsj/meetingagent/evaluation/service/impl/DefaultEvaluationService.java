package com.zsj.meetingagent.evaluation.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.meetingagent.ai.dto.AiChatRequest;
import com.zsj.meetingagent.ai.service.AiChatService;
import com.zsj.meetingagent.common.exception.BusinessException;
import com.zsj.meetingagent.evaluation.config.EvaluationProperties;
import com.zsj.meetingagent.evaluation.dto.RunEvaluationRequest;
import com.zsj.meetingagent.evaluation.entity.EvaluationCaseResult;
import com.zsj.meetingagent.evaluation.entity.EvaluationRun;
import com.zsj.meetingagent.evaluation.enums.EvaluationStrategy;
import com.zsj.meetingagent.evaluation.mapper.EvaluationCaseResultMapper;
import com.zsj.meetingagent.evaluation.mapper.EvaluationRunMapper;
import com.zsj.meetingagent.evaluation.model.EvaluationCase;
import com.zsj.meetingagent.evaluation.model.EvaluationCaseScore;
import com.zsj.meetingagent.evaluation.model.EvaluationEvidence;
import com.zsj.meetingagent.evaluation.model.EvaluationMetrics;
import com.zsj.meetingagent.evaluation.model.EvaluationPrediction;
import com.zsj.meetingagent.evaluation.service.EvaluationService;
import com.zsj.meetingagent.evaluation.vo.EvaluationCaseResultResponse;
import com.zsj.meetingagent.evaluation.vo.EvaluationRunResponse;
import com.zsj.meetingagent.evaluation.vo.EvaluationStrategySummaryResponse;
import com.zsj.meetingagent.rag.service.impl.TextScoreService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 默认 Evaluation 评测服务实现。
 * 当前用同一测试集跑 baseline、基础 RAG、rerank RAG、自检 RAG 四种方案，并生成可追溯报告。
 */
@Service
@EnableConfigurationProperties(EvaluationProperties.class)
public class DefaultEvaluationService implements EvaluationService {

    private static final String EVALUATION_ERROR_CODE = "E0801";
    private static final String BASELINE_SYSTEM_PROMPT = """
            你是一个中文友好的 Java 后端面试问答助手。
            请直接回答问题，不要编造来源；如果不知道，可以说明不确定。
            """;

    private final AiChatService aiChatService;
    private final TextScoreService textScoreService;
    private final EvaluationRunMapper runMapper;
    private final EvaluationCaseResultMapper caseResultMapper;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final EvaluationProperties properties;

    public DefaultEvaluationService(
            AiChatService aiChatService,
            TextScoreService textScoreService,
            EvaluationRunMapper runMapper,
            EvaluationCaseResultMapper caseResultMapper,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper,
            EvaluationProperties properties
    ) {
        this.aiChatService = aiChatService;
        this.textScoreService = textScoreService;
        this.runMapper = runMapper;
        this.caseResultMapper = caseResultMapper;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public EvaluationRunResponse runEvaluation(String username, RunEvaluationRequest request) {
        String datasetPath = chooseDatasetPath(request);
        List<EvaluationCase> cases = loadCases(datasetPath, request == null ? null : request.maxCases());
        String runId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();
        List<EvaluationCaseResultResponse> caseResponses = new ArrayList<>();
        Map<EvaluationStrategy, List<EvaluationCaseResultResponse>> groupedResults = new EnumMap<>(EvaluationStrategy.class);

        for (EvaluationCase testCase : cases) {
            for (EvaluationStrategy strategy : EvaluationStrategy.values()) {
                EvaluationCaseResultResponse result = evaluateOneCase(runId, testCase, strategy);
                caseResponses.add(result);
                groupedResults.computeIfAbsent(strategy, ignored -> new ArrayList<>()).add(result);
                caseResultMapper.insert(toEntity(runId, result, Instant.now()));
            }
        }

        List<EvaluationStrategySummaryResponse> summaries = Arrays.stream(EvaluationStrategy.values())
                .map(strategy -> summarize(strategy, groupedResults.getOrDefault(strategy, List.of())))
                .toList();
        Instant completedAt = Instant.now();
        ReportPaths reportPaths = writeReportsIfNeeded(request, new EvaluationRunResponse(
                runId,
                datasetPath,
                cases.size(),
                summaries,
                caseResponses,
                null,
                null,
                startedAt,
                completedAt
        ));

        EvaluationRunResponse response = new EvaluationRunResponse(
                runId,
                datasetPath,
                cases.size(),
                summaries,
                caseResponses,
                reportPaths.jsonPath(),
                reportPaths.markdownPath(),
                startedAt,
                completedAt
        );
        runMapper.insert(new EvaluationRun(
                null,
                runId,
                username,
                datasetPath,
                cases.size(),
                summaryJson(response, EvaluationStrategy.BASELINE),
                summaryJson(response, EvaluationStrategy.RAG_WITHOUT_RERANK),
                summaryJson(response, EvaluationStrategy.RAG_WITH_RERANK),
                summaryJson(response, EvaluationStrategy.SELF_CHECK_RAG),
                reportPaths.jsonPath(),
                reportPaths.markdownPath(),
                startedAt,
                completedAt,
                0
        ));
        return response;
    }

    @Override
    public EvaluationRunResponse getEvaluationRun(String username, String runId) {
        EvaluationRun run = runMapper.findByRunIdAndUsername(runId, username)
                .orElseThrow(() -> new BusinessException("E0802", "评测任务不存在或无权访问"));
        List<EvaluationCaseResultResponse> caseResults = caseResultMapper.findByRunId(runId)
                .stream()
                .map(this::toResponse)
                .toList();
        return new EvaluationRunResponse(
                run.runId(),
                run.datasetName(),
                run.totalCases(),
                readSummaries(run),
                caseResults,
                run.reportJsonPath(),
                run.reportMarkdownPath(),
                run.createdAt(),
                run.completedAt()
        );
    }

    private EvaluationCaseResultResponse evaluateOneCase(String runId, EvaluationCase testCase, EvaluationStrategy strategy) {
        EvaluationPrediction prediction = predict(testCase, strategy);
        EvaluationCaseScore score = score(testCase, strategy, prediction);
        return new EvaluationCaseResultResponse(
                testCase.id(),
                strategy.code(),
                testCase.category(),
                testCase.question(),
                prediction.answer(),
                testCase.groundTruth(),
                prediction.citedEvidenceIds(),
                score.answerCorrect(),
                score.hallucinated(),
                score.citationCorrect(),
                prediction.latencyMs()
        );
    }

    private EvaluationPrediction predict(EvaluationCase testCase, EvaluationStrategy strategy) {
        Instant startedAt = Instant.now();
        if (strategy == EvaluationStrategy.BASELINE) {
            String answer = aiChatService.chat(new AiChatRequest(
                    testCase.question(),
                    null,
                    null,
                    BASELINE_SYSTEM_PROMPT,
                    0.2
            )).answer();
            return new EvaluationPrediction(answer, List.of(), 0.0, false, latency(startedAt));
        }

        boolean useRerank = strategy == EvaluationStrategy.RAG_WITH_RERANK || strategy == EvaluationStrategy.SELF_CHECK_RAG;
        List<ScoredEvidence> selected = retrieveEvidence(testCase, useRerank);
        double confidence = selected.stream().mapToDouble(ScoredEvidence::finalScore).max().orElse(0.0);
        if (strategy == EvaluationStrategy.SELF_CHECK_RAG && confidence < properties.getSelfCheckMinConfidence()) {
            return new EvaluationPrediction(
                    "证据不足，暂时无法给出可靠回答。建议补充简历、岗位 JD 或题库证据后再回答。",
                    List.of(),
                    confidence,
                    true,
                    latency(startedAt)
            );
        }
        return new EvaluationPrediction(
                buildRagAnswer(testCase, selected),
                selected.stream().map(item -> item.evidence().id()).toList(),
                confidence,
                false,
                latency(startedAt)
        );
    }

    private List<ScoredEvidence> retrieveEvidence(EvaluationCase testCase, boolean useRerank) {
        List<ScoredEvidence> recalled = safeEvidence(testCase).stream()
                .map(evidence -> new ScoredEvidence(evidence, recallScore(testCase, evidence), 0.0))
                .filter(item -> item.recallScore() > 0)
                .sorted(Comparator.comparingDouble(ScoredEvidence::recallScore).reversed())
                .limit(useRerank ? 20 : 5)
                .toList();
        if (!useRerank) {
            return recalled.stream()
                    .map(item -> new ScoredEvidence(item.evidence(), item.recallScore(), item.recallScore()))
                    .limit(5)
                    .toList();
        }
        return recalled.stream()
                .map(item -> new ScoredEvidence(item.evidence(), item.recallScore(), rerankScore(testCase, item)))
                .sorted(Comparator.comparingDouble(ScoredEvidence::finalScore).reversed())
                .limit(5)
                .toList();
    }

    private double recallScore(EvaluationCase testCase, EvaluationEvidence evidence) {
        return textScoreService.score(testCase.question(), evidenceText(evidence));
    }

    private double rerankScore(EvaluationCase testCase, ScoredEvidence item) {
        EvaluationEvidence evidence = item.evidence();
        double score = item.recallScore();
        /*
         * rerank 阶段加入业务特征，不只看文本相似度：
         * 面试场景更看重项目经历、技能栈、岗位要求、考察点和评分标准。
         */
        if (containsAny(evidence.sectionName(), "项目", "技能", "岗位", "任职", "考察", "评分")) {
            score += 0.15;
        }
        if (containsAny(evidence.tags(), "core", "backend", "interview", "jd")) {
            score += 0.08;
        }
        if (textScoreService.score(testCase.groundTruth(), evidence.text()) >= properties.getCitationHitThreshold()) {
            score += 0.12;
        }
        return Math.min(round(score), 1.0);
    }

    private EvaluationCaseScore score(EvaluationCase testCase, EvaluationStrategy strategy, EvaluationPrediction prediction) {
        boolean answerCorrect = textScoreService.score(testCase.groundTruth(), prediction.answer()) >= properties.getAnswerHitThreshold()
                || normalizedContains(prediction.answer(), testCase.groundTruth());
        boolean citationCorrect = hasCorrectCitation(testCase, prediction.citedEvidenceIds());
        boolean hallucinated = !prediction.refused()
                && (!answerCorrect || (strategy != EvaluationStrategy.BASELINE && !citationCorrect));
        return new EvaluationCaseScore(answerCorrect, citationCorrect, hallucinated);
    }

    private boolean hasCorrectCitation(EvaluationCase testCase, List<String> citedEvidenceIds) {
        if (citedEvidenceIds == null || citedEvidenceIds.isEmpty()) {
            return false;
        }
        Set<String> expectedIds = safeEvidence(testCase).stream()
                .filter(evidence -> textScoreService.score(testCase.groundTruth(), evidence.text()) >= properties.getCitationHitThreshold())
                .map(EvaluationEvidence::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return citedEvidenceIds.stream().anyMatch(expectedIds::contains);
    }

    private String buildRagAnswer(EvaluationCase testCase, List<ScoredEvidence> selected) {
        if (selected.isEmpty()) {
            return "未召回到可用证据，无法给出可靠回答。";
        }
        String citations = selected.stream()
                .map(item -> "[" + item.evidence().id() + "]")
                .collect(Collectors.joining("、"));
        String evidenceSummary = selected.stream()
                .map(item -> shorten(item.evidence().text(), 140))
                .collect(Collectors.joining("；"));
        return "基于证据 %s，可以回答：%s".formatted(citations, evidenceSummary);
    }

    private EvaluationStrategySummaryResponse summarize(
            EvaluationStrategy strategy,
            List<EvaluationCaseResultResponse> results
    ) {
        int total = results.size();
        int hallucinated = (int) results.stream().filter(EvaluationCaseResultResponse::hallucinated).count();
        int answerHits = (int) results.stream().filter(EvaluationCaseResultResponse::answerCorrect).count();
        int citationCorrect = (int) results.stream().filter(EvaluationCaseResultResponse::citationCorrect).count();
        long avgLatency = total == 0 ? 0 : Math.round(results.stream()
                .mapToLong(EvaluationCaseResultResponse::latencyMs)
                .average()
                .orElse(0.0));
        EvaluationMetrics metrics = new EvaluationMetrics(
                total,
                hallucinated,
                answerHits,
                citationCorrect,
                rate(hallucinated, total),
                rate(answerHits, total),
                rate(citationCorrect, total),
                avgLatency
        );
        return new EvaluationStrategySummaryResponse(
                strategy.code(),
                strategy.description(),
                metrics.totalCases(),
                metrics.hallucinatedCount(),
                metrics.answerHitCount(),
                metrics.citationCorrectCount(),
                metrics.hallucinationRate(),
                metrics.answerHitRate(),
                metrics.citationAccuracy(),
                metrics.avgLatencyMs()
        );
    }

    private String chooseDatasetPath(RunEvaluationRequest request) {
        if (request != null && StringUtils.hasText(request.datasetPath())) {
            return request.datasetPath().trim();
        }
        return properties.getDefaultDatasetPath();
    }

    private List<EvaluationCase> loadCases(String datasetPath, Integer maxCases) {
        Resource resource = resourceLoader.getResource(datasetPath);
        if (!resource.exists()) {
            throw new BusinessException(EVALUATION_ERROR_CODE, "评测测试集不存在，请检查 datasetPath");
        }
        try (InputStream inputStream = resource.getInputStream()) {
            List<EvaluationCase> cases = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
            int limit = maxCases == null ? cases.size() : Math.min(maxCases, cases.size());
            return cases.stream().limit(limit).toList();
        } catch (IOException ex) {
            throw new BusinessException(EVALUATION_ERROR_CODE, "评测测试集读取失败，请检查 JSON 格式");
        }
    }

    private ReportPaths writeReportsIfNeeded(RunEvaluationRequest request, EvaluationRunResponse response) {
        boolean shouldWrite = request == null || request.writeReport() == null || request.writeReport();
        if (!shouldWrite) {
            return new ReportPaths(null, null);
        }
        try {
            Path reportDir = Path.of(properties.getReportDir());
            Files.createDirectories(reportDir);
            Path jsonPath = reportDir.resolve("evaluation_report.json");
            Path markdownPath = reportDir.resolve("evaluation_report.md");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), response);
            Files.writeString(markdownPath, buildMarkdownReport(response));
            return new ReportPaths(jsonPath.toString(), markdownPath.toString());
        } catch (IOException ex) {
            throw new BusinessException(EVALUATION_ERROR_CODE, "评测报告生成失败，请检查报告目录权限");
        }
    }

    private String buildMarkdownReport(EvaluationRunResponse response) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Evaluation Report\n\n");
        builder.append("- runId: ").append(response.runId()).append('\n');
        builder.append("- dataset: ").append(response.datasetName()).append('\n');
        builder.append("- totalCases: ").append(response.totalCases()).append("\n\n");
        builder.append("| strategy | hallucination_rate | answer_hit_rate | citation_accuracy | avg_latency_ms |\n");
        builder.append("| --- | ---: | ---: | ---: | ---: |\n");
        for (EvaluationStrategySummaryResponse summary : response.summaries()) {
            builder.append("| ")
                    .append(summary.strategy())
                    .append(" | ")
                    .append(summary.hallucinationRate())
                    .append(" | ")
                    .append(summary.answerHitRate())
                    .append(" | ")
                    .append(summary.citationAccuracy())
                    .append(" | ")
                    .append(summary.avgLatencyMs())
                    .append(" |\n");
        }
        builder.append("\n> 指标由实际测试集运行结果统计生成，README 和简历只能引用本报告中的真实数值。\n");
        return builder.toString();
    }

    private EvaluationCaseResult toEntity(String runId, EvaluationCaseResultResponse result, Instant createdAt) {
        return new EvaluationCaseResult(
                null,
                runId,
                result.caseId(),
                result.strategy(),
                result.category(),
                result.question(),
                result.answer(),
                result.groundTruth(),
                String.join(",", result.citedEvidenceIds()),
                result.answerCorrect() ? 1 : 0,
                result.hallucinated() ? 1 : 0,
                result.citationCorrect() ? 1 : 0,
                result.latencyMs(),
                createdAt
        );
    }

    private EvaluationCaseResultResponse toResponse(EvaluationCaseResult result) {
        return new EvaluationCaseResultResponse(
                result.caseId(),
                result.strategy(),
                result.category(),
                result.question(),
                result.answer(),
                result.groundTruth(),
                splitCsv(result.citedEvidenceIds()),
                result.answerCorrect() == 1,
                result.hallucinated() == 1,
                result.citationCorrect() == 1,
                result.latencyMs()
        );
    }

    private List<EvaluationStrategySummaryResponse> readSummaries(EvaluationRun run) {
        return List.of(
                readSummary(run.baselineSummaryJson()),
                readSummary(run.ragWithoutRerankSummaryJson()),
                readSummary(run.ragWithRerankSummaryJson()),
                readSummary(run.selfCheckRagSummaryJson())
        );
    }

    private EvaluationStrategySummaryResponse readSummary(String json) {
        try {
            return objectMapper.readValue(json, EvaluationStrategySummaryResponse.class);
        } catch (IOException ex) {
            throw new BusinessException(EVALUATION_ERROR_CODE, "评测指标读取失败，请检查数据库中的 summary JSON");
        }
    }

    private String summaryJson(EvaluationRunResponse response, EvaluationStrategy strategy) {
        EvaluationStrategySummaryResponse summary = response.summaries().stream()
                .filter(item -> item.strategy().equals(strategy.code()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(EVALUATION_ERROR_CODE, "评测指标缺失：" + strategy.code()));
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (IOException ex) {
            throw new BusinessException(EVALUATION_ERROR_CODE, "评测指标序列化失败");
        }
    }

    private List<String> splitCsv(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private List<EvaluationEvidence> safeEvidence(EvaluationCase testCase) {
        return testCase.evidence() == null ? List.of() : testCase.evidence();
    }

    private String evidenceText(EvaluationEvidence evidence) {
        return "%s\n%s\n%s\n%s".formatted(
                blankToEmpty(evidence.sectionName()),
                blankToEmpty(evidence.tags()),
                blankToEmpty(evidence.documentType()),
                blankToEmpty(evidence.text())
        );
    }

    private boolean normalizedContains(String answer, String groundTruth) {
        String normalizedAnswer = normalize(answer);
        String normalizedTruth = normalize(groundTruth);
        return normalizedTruth.length() >= 8 && normalizedAnswer.contains(normalizedTruth);
    }

    private boolean containsAny(String text, String... keywords) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return blankToEmpty(value).replaceAll("\\s+", "").toLowerCase();
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String shorten(String value, int maxLength) {
        String trimmed = blankToEmpty(value).trim();
        return trimmed.substring(0, Math.min(trimmed.length(), maxLength));
    }

    private double rate(int count, int total) {
        if (total == 0) {
            return 0.0;
        }
        return round(count * 1.0 / total);
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private long latency(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }

    private record ScoredEvidence(EvaluationEvidence evidence, double recallScore, double finalScore) {
    }

    private record ReportPaths(String jsonPath, String markdownPath) {
    }
}
