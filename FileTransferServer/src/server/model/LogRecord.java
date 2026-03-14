package server.model;

import java.time.LocalDateTime;

public class LogRecord {
    public int logId;
    public String action;
    public Integer fileId;
    public String result;
    public String message;
    public LocalDateTime createdAt;
}