package server.dao;

import server.db.Db;
import server.model.LogRecord;

import java.sql.*;
import java.util.*;

public class LogDao {

    public void insert(String action, Integer fileId, Integer senderId, Integer receiverId,
                       String result, String message) {
        String sql = "INSERT INTO dbo.transfer_log(action, file_id, sender_id, receiver_id, result, message) " +
                     "VALUES(?,?,?,?,?,?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, action);
            if (fileId == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, fileId);
            if (senderId == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, senderId);
            if (receiverId == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, receiverId);
            ps.setString(5, result);
            ps.setString(6, message);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<LogRecord> search(String keyword) throws Exception {
        String k = (keyword == null) ? "" : keyword.trim();
        String sql = "SELECT log_id, action, file_id, result, message, created_at " +
                     "FROM dbo.transfer_log " +
                     "WHERE action LIKE ? OR result LIKE ? OR message LIKE ? " +
                     "ORDER BY log_id DESC";
        List<LogRecord> out = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            String like = "%" + k + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LogRecord lr = new LogRecord();
                    lr.logId = rs.getInt("log_id");
                    lr.action = rs.getString("action");
                    lr.fileId = (Integer) rs.getObject("file_id");
                    lr.result = rs.getString("result");
                    lr.message = rs.getString("message");
                    Timestamp ts = rs.getTimestamp("created_at");
                    lr.createdAt = (ts == null) ? null : ts.toLocalDateTime();
                    out.add(lr);
                }
            }
        }
        return out;
    }
}