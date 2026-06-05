package com.zsj.meetingagent.frontendadapter;

import com.zsj.meetingagent.auth.security.LoginUserContext;
import com.zsj.meetingagent.common.result.ApiResponse;
import com.zsj.meetingagent.interview.dto.CreateInterviewSessionRequest;
import com.zsj.meetingagent.interview.dto.SubmitInterviewAnswerRequest;
import com.zsj.meetingagent.interview.service.InterviewService;
import com.zsj.meetingagent.interview.vo.InterviewAnswerResponse;
import com.zsj.meetingagent.interview.vo.InterviewQuestionResponse;
import com.zsj.meetingagent.interview.vo.InterviewReportResponse;
import com.zsj.meetingagent.interview.vo.InterviewSessionResponse;
import com.zsj.meetingagent.resume.dto.ResumeTextRequest;
import com.zsj.meetingagent.resume.service.ResumeService;
import com.zsj.meetingagent.resume.vo.ResumeResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 旧前端模拟面试接口兼容层。
 * 这里只做旧路径和字段名适配，核心简历和面试流程仍委托 resume/interview 模块。
 */
@RestController
@RequestMapping("/api/xunzhi/v1")
public class LegacyInterviewController {

    private final ResumeService resumeService;
    private final InterviewService interviewService;

    public LegacyInterviewController(ResumeService resumeService, InterviewService interviewService) {
        this.resumeService = resumeService;
        this.interviewService = interviewService;
    }

    @PostMapping(value = "/agents/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> uploadResume(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(toLegacyResume(resumeService.uploadFile(LoginUserContext.currentUsername(), file)));
    }

    @PostMapping("/agents/files/text")
    public ApiResponse<Map<String, Object>> uploadResumeText(
            @Valid @RequestBody ResumeTextRequest request
    ) {
        return ApiResponse.success(toLegacyResume(resumeService.uploadText(LoginUserContext.currentUsername(), request)));
    }

    @PostMapping("/interview/sessions")
    public ApiResponse<Map<String, Object>> createSession(
            @RequestBody Map<String, Object> request
    ) {
        /*
         * 旧前端的真实页面流程是：先创建一个空会话 ID，再上传 PDF 并生成题目。
         * 如果请求体里没有 resumeId，这里只返回前端需要的 sessionId，不提前写业务数据。
         */
        if (request == null || stringValue(request, "resumeId") == null) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("id", UUID.randomUUID().toString());
            payload.put("sessionId", payload.get("id"));
            payload.put("status", "CREATED");
            return ApiResponse.success(payload);
        }
        InterviewSessionResponse response = interviewService.createSession(LoginUserContext.currentUsername(), new CreateInterviewSessionRequest(
                stringValue(request, "resumeId"),
                stringValue(request, "jobTitle"),
                firstStringValue(request, "companyName", "company"),
                stringValue(request, "jobDescription"),
                intValue(request, "questionCount")
        ));
        return ApiResponse.success(toLegacySession(response));
    }

    @PostMapping("/interview/sessions/{sessionId}/interview-questions")
    public ApiResponse<Map<String, Object>> generateQuestions(
            @PathVariable String sessionId,
            HttpServletRequest request
    ) {
        MultipartFile resumePdf = request instanceof MultipartHttpServletRequest multipartRequest
                ? multipartRequest.getFile("resumePdf")
                : null;
        if (resumePdf != null && !resumePdf.isEmpty()) {
            ResumeResponse resume = resumeService.uploadFile(LoginUserContext.currentUsername(), resumePdf);
            /*
             * 前端创建空会话后会继续沿用这个 sessionId，所以这里用指定 ID 创建真实面试会话。
             */
            interviewService.createSession(LoginUserContext.currentUsername(), sessionId, new CreateInterviewSessionRequest(
                    resume.resumeId(),
                    "Java 后端开发实习生",
                    "",
                    "结合用户上传简历，重点考察 Java、Spring Boot、数据库、缓存、接口设计和项目表达能力。",
                    5
            ));
        }
        InterviewSessionResponse response = interviewService.generateQuestions(LoginUserContext.currentUsername(), sessionId);
        return ApiResponse.success(toLegacyQuestionExtraction(response));
    }

    @PostMapping("/interview/sessions/{sessionId}/interview/answer-json")
    public ApiResponse<Map<String, Object>> answerQuestion(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> request
    ) {
        String questionId = firstStringValue(request, "questionId", "questionNumber");
        String answer = firstStringValue(request, "answer", "userAnswer", "answerContent");
        InterviewAnswerResponse response = interviewService.submitAnswer(LoginUserContext.currentUsername(), sessionId, new SubmitInterviewAnswerRequest(questionId, answer));
        InterviewSessionResponse session = interviewService.getSession(LoginUserContext.currentUsername(), sessionId);
        InterviewQuestionResponse nextQuestion = nextUnansweredQuestion(session.questions());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", response.sessionId());
        payload.put("questionId", response.questionId());
        payload.put("questionNumber", response.questionId());
        payload.put("score", response.score());
        payload.put("totalScore", session.totalScore());
        payload.put("feedback", response.feedback());
        payload.put("followUpQuestion", response.followUpQuestion());
        payload.put("followUpRuleTrace", response.followUpRuleTrace());
        payload.put("status", response.status());
        payload.put("answeredCount", response.answeredCount());
        payload.put("questionCount", response.questionCount());
        payload.put("isSuccess", true);
        payload.put("finished", session.status().name().equals("COMPLETED"));
        if (nextQuestion != null) {
            payload.put("nextQuestionNumber", nextQuestion.questionId());
            payload.put("nextQuestion", nextQuestion.question());
            payload.put("questionContent", nextQuestion.question());
            payload.put("isFollowUp", false);
            payload.put("followUpCount", 0);
        }
        return ApiResponse.success(payload);
    }

    @GetMapping("/interview/sessions/{sessionId}/current-question")
    public ApiResponse<Map<String, Object>> getCurrentQuestion(@PathVariable String sessionId) {
        InterviewSessionResponse session = interviewService.getSession(LoginUserContext.currentUsername(), sessionId);
        InterviewQuestionResponse question = nextUnansweredQuestion(session.questions());
        return ApiResponse.success(toLegacyCurrentQuestion(session, question));
    }

    @GetMapping("/interview/sessions/{sessionId}/next-question")
    public ApiResponse<Map<String, Object>> getNextQuestion(@PathVariable String sessionId) {
        return getCurrentQuestion(sessionId);
    }

    @PutMapping("/interview/sessions/{sessionId}/finish")
    public ApiResponse<Void> finishInterviewSession(@PathVariable String sessionId) {
        interviewService.getReport(LoginUserContext.currentUsername(), sessionId);
        return ApiResponse.success(null);
    }

    @PostMapping({"/interview/interview/record", "/interview/record"})
    public ApiResponse<Void> saveInterviewRecord(@RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.success(null);
    }

    @PostMapping({
            "/interview/interview/record/save-from-redis/{sessionId}",
            "/interview/record/save-from-redis/{sessionId}"
    })
    public ApiResponse<Void> saveInterviewRecordFromRedis(@PathVariable String sessionId) {
        return ApiResponse.success(null);
    }

    @GetMapping({"/interview/interview/records", "/interview/records"})
    public ApiResponse<Map<String, Object>> pageInterviewRecords() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("records", List.of());
        payload.put("total", 0);
        payload.put("size", 20);
        payload.put("current", 1);
        payload.put("pages", 0);
        return ApiResponse.success(payload);
    }

    @GetMapping("/interview/conversations")
    public ApiResponse<Map<String, Object>> pageInterviewConversations() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("records", List.of());
        payload.put("total", 0);
        payload.put("size", 20);
        payload.put("current", 1);
        payload.put("pages", 0);
        return ApiResponse.success(payload);
    }

    @GetMapping("/interview/sessions/{sessionId}/radar-chart")
    public ApiResponse<Map<String, Object>> getRadarChart(@PathVariable String sessionId) {
        InterviewReportResponse report = interviewService.getReport(LoginUserContext.currentUsername(), sessionId);
        return ApiResponse.success(toLegacyRadar(report));
    }

    @GetMapping("/interview/interview/record/{sessionId}")
    public ApiResponse<Map<String, Object>> getReport(@PathVariable String sessionId) {
        return ApiResponse.success(toLegacyReport(interviewService.getReport(LoginUserContext.currentUsername(), sessionId)));
    }

    @GetMapping("/interview/record/{sessionId}")
    public ApiResponse<Map<String, Object>> getReportAlias(@PathVariable String sessionId) {
        return getReport(sessionId);
    }

    private Map<String, Object> toLegacyResume(ResumeResponse response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", response.resumeId());
        payload.put("resumeId", response.resumeId());
        payload.put("fileName", response.fileName());
        payload.put("summary", response.summary());
        payload.put("contentType", response.contentType());
        payload.put("fileSize", response.fileSize());
        payload.put("createTime", response.createdAt());
        return payload;
    }

    private Map<String, Object> toLegacySession(InterviewSessionResponse response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", response.sessionId());
        payload.put("sessionId", response.sessionId());
        payload.put("resumeId", response.resumeId());
        payload.put("jobTitle", response.jobTitle());
        payload.put("companyName", response.companyName());
        payload.put("status", response.status());
        payload.put("questionCount", response.questionCount());
        payload.put("answeredCount", response.answeredCount());
        payload.put("totalScore", response.totalScore());
        payload.put("reportSummary", response.reportSummary());
        payload.put("questions", response.questions().stream().map(this::toLegacyQuestion).toList());
        return payload;
    }

    private Map<String, Object> toLegacyQuestion(InterviewQuestionResponse question) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", question.questionId());
        payload.put("questionId", question.questionId());
        payload.put("questionOrder", question.questionOrder());
        payload.put("question", question.question());
        payload.put("referenceAnswer", question.referenceAnswer());
        payload.put("evaluationPoints", question.evaluationPoints());
        payload.put("followUpDirection", question.followUpDirection());
        payload.put("evidenceIds", question.evidenceIds());
        payload.put("evidenceSummary", question.evidenceSummary());
        payload.put("userAnswer", question.userAnswer());
        payload.put("score", question.score());
        payload.put("feedback", question.feedback());
        payload.put("followUpQuestion", question.followUpQuestion());
        return payload;
    }

    private Map<String, Object> toLegacyQuestionExtraction(InterviewSessionResponse response) {
        Map<String, Object> questions = new LinkedHashMap<>();
        Map<String, Object> suggestions = new LinkedHashMap<>();
        response.questions().forEach(question -> {
            questions.put(String.valueOf(question.questionOrder()), question.question());
            suggestions.put(String.valueOf(question.questionOrder()), question.evaluationPoints());
        });
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", response.sessionId());
        payload.put("sessionId", response.sessionId());
        payload.put("userName", response.sessionId());
        payload.put("questions", questions);
        payload.put("suggestions", suggestions);
        payload.put("interviewType", response.jobTitle());
        payload.put("resumeFileUrl", response.resumeId());
        payload.put("resumeScore", 80);
        payload.put("questionCount", response.questions().size());
        payload.put("suggestionCount", suggestions.size());
        payload.put("isSuccess", 1);
        payload.put("createTime", response.createdAt());
        payload.put("updateTime", response.updatedAt());
        payload.put("questionList", response.questions().stream().map(this::toLegacyQuestion).toList());
        return payload;
    }

    private Map<String, Object> toLegacyCurrentQuestion(InterviewSessionResponse session, InterviewQuestionResponse question) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", session.sessionId());
        payload.put("isSuccess", true);
        payload.put("finished", question == null || session.status().name().equals("COMPLETED"));
        payload.put("totalScore", session.totalScore());
        if (question != null) {
            payload.put("questionNumber", question.questionId());
            payload.put("questionContent", question.question());
            payload.put("nextQuestionNumber", question.questionId());
            payload.put("nextQuestion", question.question());
            payload.put("isFollowUp", false);
            payload.put("followUpCount", 0);
        }
        return payload;
    }

    private Map<String, Object> toLegacyReport(InterviewReportResponse report) {
        Map<String, Object> payload = new LinkedHashMap<>();
        List<Map<String, Object>> qaReviews = report.questions().stream()
                .map(this::toLegacyQaReview)
                .toList();
        payload.put("sessionId", report.sessionId());
        payload.put("id", report.sessionId());
        payload.put("userId", 0);
        payload.put("status", report.status());
        payload.put("interviewStatus", report.status());
        payload.put("resumeScore", 80);
        payload.put("interviewScore", report.totalScore());
        payload.put("compositeScore", report.totalScore());
        payload.put("totalScore", report.totalScore());
        payload.put("finalScore", report.totalScore());
        payload.put("answeredCount", report.answeredCount());
        payload.put("questionCount", report.questionCount());
        payload.put("reportSummary", report.reportSummary());
        payload.put("interviewSuggestions", report.reportSummary());
        payload.put("interviewSuggestionsMap", Map.of("1", report.reportSummary() == null ? "继续补充项目细节和量化结果。" : report.reportSummary()));
        payload.put("interviewDirection", "Java 后端开发");
        payload.put("radarChart", toLegacyRadar(report));
        payload.put("radarPoints", toLegacyRadar(report).get("radarPoints"));
        payload.put("qaReviews", qaReviews);
        payload.put("playbackItems", qaReviews);
        payload.put("questions", report.questions().stream().map(this::toLegacyQuestion).toList());
        return payload;
    }

    private Map<String, Object> toLegacyQaReview(InterviewQuestionResponse question) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("seq", question.questionOrder());
        payload.put("questionNumber", question.questionId());
        payload.put("question", question.question());
        payload.put("answer", question.userAnswer());
        payload.put("score", question.score());
        payload.put("feedback", question.feedback());
        payload.put("isFollowUp", false);
        payload.put("followUpNeeded", question.followUpQuestion() != null);
        payload.put("followUpRuleTrace", question.followUpRuleTrace());
        payload.put("followUpCount", 0);
        return payload;
    }

    private Map<String, Object> toLegacyRadar(InterviewReportResponse report) {
        int score = report.totalScore() == null ? 75 : report.totalScore();
        List<Map<String, Object>> points = List.of(
                Map.of("label", "简历评估", "value", 80),
                Map.of("label", "面试表现", "value", score),
                Map.of("label", "专业技能", "value", Math.max(60, score - 5)),
                Map.of("label", "表达结构", "value", Math.max(60, score - 8)),
                Map.of("label", "发展潜力", "value", Math.min(100, score + 3))
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("resumeScore", 80);
        payload.put("interviewPerformance", score);
        payload.put("professionalSkills", Math.max(60, score - 5));
        payload.put("potentialIndex", Math.min(100, score + 3));
        payload.put("interviewScore", score);
        payload.put("totalScore", score);
        payload.put("radarPoints", points);
        payload.put("radarMetrics", points);
        return payload;
    }

    private InterviewQuestionResponse nextUnansweredQuestion(List<InterviewQuestionResponse> questions) {
        return questions.stream()
                .filter(question -> question.userAnswer() == null || question.userAnswer().isBlank())
                .findFirst()
                .orElse(null);
    }

    private String firstStringValue(Map<String, Object> request, String... keys) {
        for (String key : keys) {
            String value = stringValue(request, key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String stringValue(Map<String, Object> request, String key) {
        Object value = request.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Integer intValue(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }
        return null;
    }
}
