package com.notsureance.claims.agent.model;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/** Spring AI ChatModel backed by the official Anthropic Java SDK, so the agent loop stays provider neutral. */
public class ClaudeChatModel implements ChatModel {

    static final String RAW_TURN = "claude.turn";
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };

    private final AnthropicClient client;
    private final ClaudeProperties properties;
    private final ObjectMapper json;

    public ClaudeChatModel(AnthropicClient client, ClaudeProperties properties, ObjectMapper json) {
        this.client = client;
        this.properties = properties;
        this.json = json;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        MessageCreateParams.Builder request = MessageCreateParams.builder()
                .model(properties.model())
                .maxTokens(properties.maxTokens())
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.of(properties.effort())).build())
                .cacheControl(CacheControlEphemeral.builder().build());
        if (properties.refusalFallback()) {
            request.putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                    .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
        }
        String system = prompt.getInstructions().stream().filter(SystemMessage.class::isInstance)
                .map(m -> m.getText()).collect(Collectors.joining("\n\n"));
        if (!system.isBlank()) {
            request.system(system);
        }
        if (prompt.getOptions() instanceof ToolCallingChatOptions options && options.getToolCallbacks() != null) {
            options.getToolCallbacks().forEach(callback -> request.addTool(toTool(callback)));
        }
        prompt.getInstructions().forEach(message -> {
            if (message instanceof UserMessage user) {
                request.addUserMessage(user.getText());
            } else if (message instanceof AssistantMessage assistant) {
                Object raw = assistant.getMetadata().get(RAW_TURN);
                request.addMessage(raw instanceof MessageParam turn ? turn : MessageParam.ofAssistant(assistant.getText()));
            } else if (message instanceof ToolResponseMessage results) {
                request.addMessage(MessageParam.ofUserOfContentBlockParams(results.getResponses().stream()
                        .map(r -> ContentBlockParam.ofToolResult(ToolResultBlockParam.builder()
                                .toolUseId(r.id()).content(r.responseData()).build()))
                        .toList()));
            }
        });
        return new ChatResponse(List.of(new Generation(toAssistant(client.messages().create(request.build())))));
    }

    // The full turn, thinking blocks included, rides along so the next request sends it back unchanged.
    private AssistantMessage toAssistant(Message response) {
        if (response.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
            throw new AgentModelException("The model declined to answer this request");
        }
        StringBuilder text = new StringBuilder();
        List<AssistantMessage.ToolCall> calls = new ArrayList<>();
        for (ContentBlock block : response.content()) {
            block.text().ifPresent(t -> text.append(t.text()));
            block.toolUse().ifPresent(use -> calls.add(new AssistantMessage.ToolCall(use.id(), "function", use.name(),
                    writeJson(use._input()))));
        }
        return AssistantMessage.builder().content(text.toString()).toolCalls(calls)
                .properties(Map.of(RAW_TURN, response.toParam())).build();
    }

    private Tool toTool(ToolCallback callback) {
        ToolDefinition definition = callback.getToolDefinition();
        Map<String, Object> schema = readJson(definition.inputSchema());
        Map<String, JsonValue> fields = new LinkedHashMap<>();
        Object properties = schema.getOrDefault("properties", Map.of());
        ((Map<?, ?>) properties).forEach((name, spec) -> fields.put(String.valueOf(name), JsonValue.from(spec)));
        Tool.InputSchema.Builder input = Tool.InputSchema.builder()
                .properties(Tool.InputSchema.Properties.builder().putAllAdditionalProperties(fields).build());
        if (schema.get("required") instanceof List<?> required) {
            input.required(required.stream().map(String::valueOf).toList());
        }
        return Tool.builder().name(definition.name()).description(definition.description()).inputSchema(input.build()).build();
    }

    private Map<String, Object> readJson(String text) {
        try {
            return json.readValue(text, MAP);
        } catch (Exception ex) {
            throw new AgentModelException("Tool schema could not be read");
        }
    }

    private String writeJson(JsonValue value) {
        try {
            return json.writeValueAsString(value.convert(Object.class));
        } catch (Exception ex) {
            return "{}";
        }
    }
}
