package com.notsureance.claims.agent;

import com.notsureance.claims.agent.tools.AgentTool;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;

/** Runs the tool loop by hand so the step cap, the time budget and the trace stay under our control. */
@Component
public class AgentRunner {

    public record Outcome(AgentRunStatus status, String answer, int toolCalls, Citations.Check citations) {
    }

    private final Map<String, ToolCallback> tools;
    private final AgentTraceRepository traces;
    private final AgentProperties properties;
    private final Clock clock;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public AgentRunner(List<AgentTool> agentTools, AgentTraceRepository traces, AgentProperties properties, Clock clock) {
        ToolCallback[] callbacks = MethodToolCallbackProvider.builder().toolObjects(agentTools.toArray()).build()
                .getToolCallbacks();
        this.tools = Arrays.stream(callbacks)
                .collect(Collectors.toMap(c -> c.getToolDefinition().name(), Function.identity()));
        this.traces = traces;
        this.properties = properties;
        this.clock = clock;
    }

    Outcome run(ChatModel model, AgentToolContext context, String question) {
        Instant deadline = clock.instant().plus(properties.timeout());
        List<Message> conversation = new ArrayList<>(List.of(new SystemMessage(AgentPrompt.SYSTEM),
                new UserMessage(AgentPrompt.userTurn(context.claimId(), question))));
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(List.copyOf(tools.values())).internalToolExecutionEnabled(false).build();
        int calls = 0;
        while (true) {
            Duration left = Duration.between(clock.instant(), deadline);
            if (left.isNegative() || left.isZero()) {
                return new Outcome(AgentRunStatus.TIMED_OUT, null, calls, Citations.check(null, context.citable()));
            }
            AssistantMessage reply;
            try {
                reply = callWithin(model, new Prompt(List.copyOf(conversation), options), left);
            } catch (TimeoutException ex) {
                return new Outcome(AgentRunStatus.TIMED_OUT, null, calls, Citations.check(null, context.citable()));
            }
            conversation.add(reply);
            if (!reply.hasToolCalls()) {
                String answer = reply.getText();
                return new Outcome(AgentRunStatus.COMPLETED, answer, calls, Citations.check(answer, context.citable()));
            }
            if (calls + reply.getToolCalls().size() > properties.maxToolCalls()) {
                return new Outcome(AgentRunStatus.STEP_LIMIT, null, calls, Citations.check(null, context.citable()));
            }
            List<ToolResponse> results = new ArrayList<>();
            for (AssistantMessage.ToolCall call : reply.getToolCalls()) {
                calls++;
                results.add(new ToolResponse(call.id(), call.name(), execute(call, context, calls)));
            }
            conversation.add(ToolResponseMessage.builder().responses(results).build());
        }
    }

    private AssistantMessage callWithin(ChatModel model, Prompt prompt, Duration left) throws TimeoutException {
        Future<AssistantMessage> pending = executor.submit(() -> model.call(prompt).getResult().getOutput());
        try {
            return pending.get(left.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            pending.cancel(true);
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TimeoutException("Interrupted");
        } catch (ExecutionException ex) {
            throw ex.getCause() instanceof RuntimeException runtime ? runtime : new IllegalStateException(ex.getCause());
        }
    }

    // A failing tool goes back to the model as an error result rather than ending the run.
    private String execute(AssistantMessage.ToolCall call, AgentToolContext context, int step) {
        long started = System.nanoTime();
        ToolCallback tool = tools.get(call.name());
        String output;
        boolean failed = false;
        try {
            output = tool == null ? "Unknown tool " + call.name() : tool.call(call.arguments(), context.asToolContext());
            failed = tool == null;
        } catch (RuntimeException ex) {
            output = "Tool error: " + rootMessage(ex);
            failed = true;
        }
        long millis = Duration.ofNanos(System.nanoTime() - started).toMillis();
        traces.save(new AgentTrace(context.runId(), step, call.name(), call.arguments(), output, failed, millis));
        return output;
    }

    private static String rootMessage(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
    }
}
