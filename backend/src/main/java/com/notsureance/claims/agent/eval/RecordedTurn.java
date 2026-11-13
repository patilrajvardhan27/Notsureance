package com.notsureance.claims.agent.eval;

import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;

/** One model reply as plain data, so a live run can be replayed in CI without a model key. */
public record RecordedTurn(String text, List<Call> toolCalls) {

    public record Call(String id, String name, String arguments) {
    }

    public static RecordedTurn of(AssistantMessage message) {
        return new RecordedTurn(message.getText(), message.getToolCalls().stream()
                .map(call -> new Call(call.id(), call.name(), call.arguments())).toList());
    }

    public AssistantMessage toMessage() {
        return AssistantMessage.builder().content(text == null ? "" : text)
                .toolCalls(toolCalls.stream()
                        .map(call -> new AssistantMessage.ToolCall(call.id(), "function", call.name(), call.arguments()))
                        .toList())
                .build();
    }
}
