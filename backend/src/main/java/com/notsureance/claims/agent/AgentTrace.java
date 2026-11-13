package com.notsureance.claims.agent;

import com.notsureance.claims.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "agent_traces")
public class AgentTrace extends BaseEntity {

    private static final int MAX_INPUT = 2000;
    private static final int MAX_OUTPUT = 8000;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(nullable = false)
    private int step;

    @Column(name = "tool_name", nullable = false)
    private String toolName;

    @Column(nullable = false)
    private String input;

    @Column(nullable = false)
    private String output;

    @Column(nullable = false)
    private boolean failed;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    protected AgentTrace() {
    }

    AgentTrace(Long runId, int step, String toolName, String input, String output, boolean failed, long durationMs) {
        this.runId = runId;
        this.step = step;
        this.toolName = toolName;
        this.input = clip(input, MAX_INPUT);
        this.output = clip(output, MAX_OUTPUT);
        this.failed = failed;
        this.durationMs = durationMs;
    }

    private static String clip(String value, int max) {
        String text = value == null ? "" : value;
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }

    public int getStep() {
        return step;
    }

    public String getToolName() {
        return toolName;
    }

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }

    public boolean isFailed() {
        return failed;
    }

    public long getDurationMs() {
        return durationMs;
    }
}
