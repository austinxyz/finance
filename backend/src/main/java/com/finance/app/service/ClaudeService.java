package com.finance.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeService {

    @Value("${claude.api.key}")
    private String apiKey;

    @Value("${claude.api.url}")
    private String apiUrl;

    @Value("${claude.api.model}")
    private String model;

    @Value("${claude.api.max-tokens}")
    private int maxTokens;

    private final ObjectMapper objectMapper;

    /**
     * Call Claude API to generate personalized financial advice
     * @param systemPrompt System prompt defining Claude's role
     * @param userMessage User's request message
     * @return Claude's response text
     */
    public String generateAdvice(String systemPrompt, String userMessage) {
        // Check if using internal gateway (ANTHROPIC_BASE_URL set but no API key)
        String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
        boolean isInternalGateway = StringUtils.hasText(baseUrl) && !StringUtils.hasText(apiKey);

        // Only require API key if not using internal gateway
        if (!isInternalGateway && !StringUtils.hasText(apiKey)) {
            log.warn("Claude API key is not configured");
            return "Claude API 未配置，请设置 CLAUDE_API_KEY 环境变量以启用 AI 增强建议功能。";
        }

        try {
            // Use internal gateway URL if available, otherwise use configured URL
            String effectiveUrl;
            if (StringUtils.hasText(baseUrl)) {
                // For internal gateway, append /messages if not already present
                effectiveUrl = baseUrl.endsWith("/messages") ? baseUrl : baseUrl + "/messages";
            } else {
                effectiveUrl = apiUrl;
            }

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("system", systemPrompt);
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", userMessage)
            ));

            // Create WebClient with SSL disabled for internal gateway
            WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl(effectiveUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("anthropic-version", "2023-06-01");

            // Only add API key header if key is configured
            if (StringUtils.hasText(apiKey)) {
                webClientBuilder.defaultHeader("x-api-key", apiKey);
            }

            // For internal gateway, disable SSL verification
            if (isInternalGateway) {
                try {
                    SslContext sslContext = SslContextBuilder
                        .forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();

                    HttpClient httpClient = HttpClient.create()
                        .secure(sslSpec -> sslSpec.sslContext(sslContext));

                    webClientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient));
                    log.info("SSL verification disabled for internal gateway");
                } catch (SSLException e) {
                    log.warn("Failed to disable SSL verification: {}", e.getMessage());
                }
            }

            WebClient webClient = webClientBuilder.build();

            log.info("Calling Claude API at: {} (Internal Gateway: {})", effectiveUrl, isInternalGateway);

            // Make API call
            String response = webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(60))
                .onErrorResume(e -> {
                    log.error("Error calling Claude API: {}", e.getMessage(), e);
                    return Mono.just("{\"error\":{\"message\":\"" + e.getMessage() + "\"}}");
                })
                .block();

            // Parse response
            if (response != null) {
                log.debug("Claude API response: {}", response);
                JsonNode jsonNode = objectMapper.readTree(response);

                // Check for errors
                if (jsonNode.has("error")) {
                    JsonNode errorNode = jsonNode.get("error");
                    String errorMessage = errorNode.has("message")
                        ? errorNode.get("message").asText()
                        : errorNode.toString();
                    log.error("Claude API error: {}", errorMessage);
                    return "调用 Claude API 时出错: " + errorMessage;
                }

                // Extract content from response (standard Anthropic format)
                if (jsonNode.has("content") && jsonNode.get("content").isArray()
                    && jsonNode.get("content").size() > 0) {
                    return jsonNode.get("content").get(0).get("text").asText();
                }

                // If not standard format, log the response structure
                log.warn("Unexpected response format from Claude API. Response: {}", response);
                return "收到了意外的响应格式，请检查日志";
            }

            return "无法从 Claude API 获取有效响应";

        } catch (Exception e) {
            log.error("Error generating AI advice", e);
            return "生成 AI 建议时出错: " + e.getMessage();
        }
    }

    /**
     * Generate enhanced financial recommendations using Claude
     * @param financialData Structured financial data
     * @param userContext User's additional context/questions
     * @return Claude's personalized advice
     */
    public String generateEnhancedRecommendations(Map<String, Object> financialData, String userContext) {
        // Build enhanced system prompt
        String systemPrompt = """
            你是一位资深的理财顾问（CFP），拥有超过15年的个人财务规划经验。

            你的专业领域包括：
            - 资产配置优化与投资组合管理
            - 风险评估与保险规划
            - 债务管理与利息优化
            - 税务规划与合规建议
            - 退休规划与长期财务目标设定
            - 流动性管理与应急基金建设

            你的建议原则：
            1. 基于数据分析，提供可量化的建议
            2. 考虑用户的风险承受能力和生命周期阶段
            3. 平衡短期流动性需求和长期增值目标
            4. 提供具体的、可执行的行动步骤
            5. 用通俗易懂的语言解释专业概念
            6. 标注建议的优先级和预期效果

            回答格式要求：
            - 使用 Markdown 格式组织内容
            - 使用标题（##、###）清晰划分章节
            - 使用列表呈现要点和行动步骤
            - 使用 **粗体** 强调关键信息
            - 适当使用 emoji 提高可读性 (💡 建议、⚠️ 警告、✅ 优点、❌ 风险)
            - 数字和百分比要清晰标注
            """;

        // Build enhanced user message with financial data and context
        StringBuilder userMessage = new StringBuilder();
        userMessage.append("# 📊 我的财务数据分析\n\n");
        userMessage.append(formatFinancialData(financialData));

        if (StringUtils.hasText(userContext)) {
            userMessage.append("\n\n# 🎯 我的个人情况和问题\n\n");
            userMessage.append(userContext);
        }

        userMessage.append("\n\n# 📝 请求建议\n\n");
        userMessage.append("请基于以上财务数据分析和我的个人情况，为我提供个性化的理财建议。\n\n");
        userMessage.append("具体需要包括：\n");
        userMessage.append("1. 对我当前财务状况的综合评价\n");
        userMessage.append("2. 针对我提出的问题的专业解答\n");
        userMessage.append("3. 具体的优化建议和行动步骤（按优先级排序）\n");
        userMessage.append("4. 预期可以达到的改善效果\n");
        userMessage.append("5. 需要注意的风险点\n\n");
        userMessage.append("请用中文回答，并以友好、专业的语气进行说明。建议要切实可行，具有可操作性。");

        return generateAdvice(systemPrompt, userMessage.toString());
    }

    /**
     * Format financial data into readable text for Claude
     */
    private String formatFinancialData(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();

        try {
            // Overall score and health level
            if (data.containsKey("overallScore")) {
                sb.append("## 💯 财务健康度总评\n\n");
                double score = Double.parseDouble(data.get("overallScore").toString());
                sb.append("**评分**: ").append(String.format("%.1f", score)).append(" / 100\n");
            }
            if (data.containsKey("healthLevel")) {
                String level = data.get("healthLevel").toString();
                String levelName = switch(level) {
                    case "EXCELLENT" -> "🌟 优秀";
                    case "GOOD" -> "👍 良好";
                    case "FAIR" -> "⚠️ 一般";
                    case "POOR" -> "❌ 较差";
                    default -> level;
                };
                sb.append("**健康等级**: ").append(levelName).append("\n\n");
            }

            // Expected impact
            if (data.containsKey("expectedImpact")) {
                Map<?, ?> impact = (Map<?, ?>) data.get("expectedImpact");
                if (impact.containsKey("overallImprovement")) {
                    sb.append("**预期改善**: ").append(impact.get("overallImprovement")).append("\n\n");
                }
            }

            // Asset allocation
            if (data.containsKey("assetAllocationOptimization")) {
                sb.append("## 📈 资产配置分析\n\n");
                Map<?, ?> allocation = (Map<?, ?>) data.get("assetAllocationOptimization");

                if (allocation.containsKey("status")) {
                    String status = allocation.get("status").toString();
                    String statusIcon = switch(status) {
                        case "OPTIMAL" -> "✅";
                        case "ACCEPTABLE" -> "👌";
                        case "NEEDS_ATTENTION" -> "⚠️";
                        default -> "";
                    };
                    sb.append("**状态**: ").append(statusIcon).append(" ");
                    sb.append(switch(status) {
                        case "OPTIMAL" -> "最优";
                        case "ACCEPTABLE" -> "可接受";
                        case "NEEDS_ATTENTION" -> "需要关注";
                        default -> status;
                    }).append("\n");
                }

                if (allocation.containsKey("currentScore")) {
                    sb.append("**评分**: ").append(allocation.get("currentScore")).append("/100\n\n");
                }

                if (allocation.containsKey("summary")) {
                    sb.append(allocation.get("summary")).append("\n\n");
                }

                if (allocation.containsKey("currentAllocation") && allocation.containsKey("recommendedAllocation")) {
                    sb.append("### 当前配置 vs 建议配置\n\n");
                    Map<?, ?> current = (Map<?, ?>) allocation.get("currentAllocation");
                    Map<?, ?> recommended = (Map<?, ?>) allocation.get("recommendedAllocation");

                    sb.append("| 资产类别 | 当前比例 | 建议比例 | 差异 |\n");
                    sb.append("|---------|----------|----------|------|\n");
                    formatAllocationComparison(sb, "现金", current.get("cashPercentage"), recommended.get("cashPercentage"));
                    formatAllocationComparison(sb, "股票", current.get("stocksPercentage"), recommended.get("stocksPercentage"));
                    formatAllocationComparison(sb, "退休基金", current.get("retirementPercentage"), recommended.get("retirementPercentage"));
                    formatAllocationComparison(sb, "房地产", current.get("realEstatePercentage"), recommended.get("realEstatePercentage"));
                    formatAllocationComparison(sb, "其他", current.get("otherPercentage"), recommended.get("otherPercentage"));
                    sb.append("\n");
                }

                if (allocation.containsKey("suggestions")) {
                    List<?> suggestions = (List<?>) allocation.get("suggestions");
                    if (!suggestions.isEmpty()) {
                        sb.append("### 具体建议\n\n");
                        for (Object suggestion : suggestions) {
                            sb.append("- ").append(suggestion).append("\n");
                        }
                        sb.append("\n");
                    }
                }
            }

            // Debt management
            if (data.containsKey("debtManagementOptimization")) {
                sb.append("## 💳 负债管理分析\n\n");
                Map<?, ?> debt = (Map<?, ?>) data.get("debtManagementOptimization");
                formatOptimizationSection(sb, debt);
            }

            // Liquidity
            if (data.containsKey("liquidityOptimization")) {
                sb.append("## 💰 流动性分析\n\n");
                Map<?, ?> liquidity = (Map<?, ?>) data.get("liquidityOptimization");
                formatOptimizationSection(sb, liquidity);

                if (liquidity.containsKey("currentCash") && liquidity.containsKey("recommendedCash")) {
                    sb.append("**当前现金**: $").append(formatCurrency(liquidity.get("currentCash"))).append("\n");
                    sb.append("**建议现金**: $").append(formatCurrency(liquidity.get("recommendedCash"))).append("\n");
                    if (liquidity.containsKey("gap")) {
                        double gap = Double.parseDouble(liquidity.get("gap").toString());
                        sb.append("**缺口/盈余**: ");
                        if (gap < 0) {
                            sb.append("⚠️ 不足 $").append(formatCurrency(Math.abs(gap)));
                        } else {
                            sb.append("✅ 盈余 $").append(formatCurrency(gap));
                        }
                        sb.append("\n\n");
                    }
                }
            }

            // Risk
            if (data.containsKey("riskOptimization")) {
                sb.append("## ⚖️ 风险管理分析\n\n");
                Map<?, ?> risk = (Map<?, ?>) data.get("riskOptimization");
                formatOptimizationSection(sb, risk);
            }

            // Tax
            if (data.containsKey("taxOptimization")) {
                sb.append("## 🧾 税务优化分析\n\n");
                Map<?, ?> tax = (Map<?, ?>) data.get("taxOptimization");
                formatOptimizationSection(sb, tax);

                if (tax.containsKey("taxablePercentage")) {
                    sb.append("**应税资产占比**: ").append(tax.get("taxablePercentage")).append("%\n");
                }
                if (tax.containsKey("optimizationPotential")) {
                    sb.append("**优化空间**: ").append(tax.get("optimizationPotential")).append("%\n\n");
                }
            }

            // Prioritized actions
            if (data.containsKey("prioritizedActions")) {
                sb.append("## 🎯 系统推荐的优先行动计划\n\n");
                List<?> actions = (List<?>) data.get("prioritizedActions");
                int count = 1;
                for (Object actionObj : actions) {
                    Map<?, ?> action = (Map<?, ?>) actionObj;
                    sb.append(count++).append(". ");

                    // Priority icon
                    if (action.containsKey("priority")) {
                        String priority = action.get("priority").toString();
                        sb.append(switch(priority) {
                            case "CRITICAL" -> "🔴";
                            case "HIGH" -> "🟠";
                            case "MEDIUM" -> "🟡";
                            case "LOW" -> "🟢";
                            default -> "";
                        }).append(" ");
                    }

                    sb.append("**").append(action.get("action")).append("**");

                    if (action.containsKey("expectedImpact")) {
                        sb.append(" - ").append(action.get("expectedImpact"));
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }

        } catch (Exception e) {
            log.error("Error formatting financial data", e);
            sb.append("财务数据格式化时出现错误\n");
        }

        return sb.toString();
    }

    /**
     * Helper method to format optimization section
     */
    private void formatOptimizationSection(StringBuilder sb, Map<?, ?> section) {
        if (section.containsKey("status")) {
            String status = section.get("status").toString();
            String statusIcon = switch(status) {
                case "OPTIMAL" -> "✅";
                case "ACCEPTABLE" -> "👌";
                case "NEEDS_ATTENTION" -> "⚠️";
                default -> "";
            };
            sb.append("**状态**: ").append(statusIcon).append(" ");
            sb.append(switch(status) {
                case "OPTIMAL" -> "最优";
                case "ACCEPTABLE" -> "可接受";
                case "NEEDS_ATTENTION" -> "需要关注";
                default -> status;
            }).append("\n");
        }

        if (section.containsKey("currentScore")) {
            sb.append("**评分**: ").append(section.get("currentScore")).append("/100\n");
        }

        if (section.containsKey("summary")) {
            sb.append("\n").append(section.get("summary")).append("\n\n");
        }

        if (section.containsKey("suggestions")) {
            List<?> suggestions = (List<?>) section.get("suggestions");
            if (!suggestions.isEmpty()) {
                sb.append("**具体建议**:\n");
                for (Object suggestion : suggestions) {
                    sb.append("- ").append(suggestion).append("\n");
                }
                sb.append("\n");
            }
        }
    }

    /**
     * Helper method to format allocation comparison row
     */
    private void formatAllocationComparison(StringBuilder sb, String category, Object current, Object recommended) {
        if (current != null && recommended != null) {
            double currentVal = Double.parseDouble(current.toString());
            double recommendedVal = Double.parseDouble(recommended.toString());
            double diff = recommendedVal - currentVal;

            sb.append("| ").append(category).append(" | ");
            sb.append(String.format("%.1f%%", currentVal)).append(" | ");
            sb.append(String.format("%.1f%%", recommendedVal)).append(" | ");

            if (Math.abs(diff) < 0.1) {
                sb.append("→ ");
            } else if (diff > 0) {
                sb.append(String.format("↑ +%.1f%%", diff));
            } else {
                sb.append(String.format("↓ %.1f%%", diff));
            }
            sb.append(" |\n");
        }
    }

    /**
     * Helper method to format currency
     */
    private String formatCurrency(Object value) {
        if (value == null) return "0";
        double amount = Double.parseDouble(value.toString());
        return String.format("%,.2f", amount);
    }
}
