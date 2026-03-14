package server.model;

import java.time.LocalDateTime;

public class FileRecord {
    public int fileId;
    public String fileName;
    public long fileSize;
    public String senderUsername;
    public String receiverUsername;
    public String serverPath;
    public LocalDateTime sentAt;
    public String status;
}