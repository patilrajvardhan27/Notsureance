package com.notsureance.claims.support;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/** Plays back a hand-written sequence of model turns. These are test scripts, not recorded model output. */
public class ScriptedChatModel implements ChatModel {

    private final Deque<AssistantMessage> turns = new ConcurrentLinkedDeque<>();
    private final List<Prompt> prompts = new ArrayList<>();

    public static AssistantMessage tool(String name, String argumentsJson) {
        var call = new AssistantMessage.ToolCall(UUID.randomUUID().toString(), "function", name, argumentsJson);
        return AssistantMessage.builder().content("").toolCalls(List.of(call)).build();
    }

    public static AssistantMessage answer(String text) {
        return new AssistantMessage(text);
    }

    public void script(AssistantMessage... next) {
        turns.clear();
        prompts.clear();
        turns.addAll(List.of(next));
    }

    public List<Prompt> prompts() {
        return List.copyOf(prompts);
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        prompts.add(prompt);
        AssistantMessage next = turns.poll();
        return new ChatResponse(List.of(new Generation(next == null ? answer("Script ended.") : next)));
    }
}
