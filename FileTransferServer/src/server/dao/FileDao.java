package server.dao;

import server.db.Db;
import server.model.FileRecord;

import java.sql.*;
import java.util.*;

public class FileDao {

    public int insert(String fileName, long fileSize, int senderId, int receiverId, String serverPath, String status) throws Exception {
        String sql = "INSERT INTO dbo.files(file_name, file_size, sender_id, receiver_id, server_path, status) VALUES(?,?,?,?,?,?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fileName);
            ps.setLong(2, fileSize);
            ps.setInt(3, senderId);
            ps.setInt(4, receiverId);
            ps.setString(5, serverPath);
            ps.setString(6, status);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            return -1;
        }
    }

    public void updateStatus(int fileId, String status) throws Exception {
        String sql = "UPDATE dbo.files SET status=? WHERE file_id=?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, fileId);
            ps.executeUpdate();
        }
    }

    public List<FileRecord> searchAdmin(String keyword) throws Exception {
        String k = (keyword == null) ? "" : keyword.trim();
        String sql =
                "SELECT f.file_id, f.file_name, f.file_size, f.server_path, f.sent_at, f.status, " +
                "s.username AS sender_username, r.username AS receiver_username " +
                "FROM dbo.files f " +
                "JOIN dbo.users s ON s.user_id=f.sender_id " +
                "JOIN dbo.users r ON r.user_id=f.receiver_id " +
                "WHERE f.file_name LIKE ? OR s.username LIKE ? OR r.username LIKE ? " +
                "ORDER BY f.file_id DESC";
        List<FileRecord> out = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            String like = "%" + k + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FileRecord fr = new FileRecord();
                    fr.fileId = rs.getInt("file_id");
                    fr.fileName = rs.getString("file_name");
                    fr.fileSize = rs.getLong("file_size");
                    fr.serverPath = rs.getString("server_path");
                    Timestamp ts = rs.getTimestamp("sent_at");
                    fr.sentAt = (ts == null) ? null : ts.toLocalDateTime();
                    fr.status = rs.getString("status");
                    fr.senderUsername = rs.getString("sender_username");
                    fr.receiverUsername = rs.getString("receiver_username");
                    out.add(fr);
                }
            }
        }
        return out;
    }

    public String serverPathById(int fileId) throws Exception {
        String sql = "SELECT server_path FROM dbo.files WHERE file_id=?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, fileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
                return null;
            }
        }
    }

    public void delete(int fileId) throws Exception {
        String sql = "DELETE FROM dbo.files WHERE file_id=?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, fileId);
            ps.executeUpdate();
        }
    }

    public List<FileRecord> pendingForReceiver(int receiverId) throws Exception {
        String sql =
                "SELECT f.file_id, f.file_name, f.file_size, f.server_path, f.sent_at, f.status, " +
                "s.username AS sender_username, r.username AS receiver_username " +
                "FROM dbo.files f " +
                "JOIN dbo.users s ON s.user_id=f.sender_id " +
                "JOIN dbo.users r ON r.user_id=f.receiver_id " +
                "WHERE f.receiver_id=? AND f.status=N'SENT' ORDER BY f.file_id ASC";
        List<FileRecord> out = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, receiverId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FileRecord fr = new FileRecord();
                    fr.fileId = rs.getInt("file_id");
                    fr.fileName = rs.getString("file_name");
                    fr.fileSize = rs.getLong("file_size");
                    fr.serverPath = rs.getString("server_path");
                    Timestamp ts = rs.getTimestamp("sent_at");
                    fr.sentAt = (ts == null) ? null : ts.toLocalDateTime();
                    fr.status = rs.getString("status");
                    fr.senderUsername = rs.getString("sender_username");
                    fr.receiverUsername = rs.getString("receiver_username");
                    out.add(fr);
                }
            }
        }
        return out;
    }

    public List<FileRecord> historyForUser(int userId) throws Exception {
        String sql =
                "SELECT f.file_id, f.file_name, f.file_size, f.sent_at, f.status, " +
                "s.username AS sender_username, r.username AS receiver_username " +
                "FROM dbo.files f " +
                "JOIN dbo.users s ON s.user_id=f.sender_id " +
                "JOIN dbo.users r ON r.user_id=f.receiver_id " +
                "WHERE f.sender_id=? OR f.receiver_id=? ORDER BY f.file_id DESC";
        List<FileRecord> out = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FileRecord fr = new FileRecord();
                    fr.fileId = rs.getInt("file_id");
                    fr.fileName = rs.getString("file_name");
                    fr.fileSize = rs.getLong("file_size");
                    Timestamp ts = rs.getTimestamp("sent_at");
                    fr.sentAt = (ts == null) ? null : ts.toLocalDateTime();
                    fr.status = rs.getString("status");
                    fr.senderUsername = rs.getString("sender_username");
                    fr.receiverUsername = rs.getString("receiver_username");
                    out.add(fr);
                }
            }
        }
        return out;
    }
}