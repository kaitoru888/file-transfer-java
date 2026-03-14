package server.dao;

import server.db.Db;
import server.model.User;

import java.sql.*;
import java.util.*;

public class UserDao {

    public Optional<User> findByUsername(String username) throws Exception {
        String sql = "SELECT user_id, username, password_hash, full_name, status FROM dbo.users WHERE username=?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                User u = new User();
                u.userId = rs.getInt("user_id");
                u.username = rs.getString("username");
                u.passwordHash = rs.getString("password_hash");
                u.fullName = rs.getString("full_name");
                u.status = rs.getString("status");
                return Optional.of(u);
            }
        }
    }

    public List<User> search(String keyword) throws Exception {
        String k = (keyword == null) ? "" : keyword.trim();
        String sql = "SELECT user_id, username, full_name, status FROM dbo.users " +
                     "WHERE username LIKE ? OR full_name LIKE ? ORDER BY user_id DESC";
        List<User> out = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            String like = "%" + k + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("full_name"),
                            rs.getString("status")
                    ));
                }
            }
        }
        return out;
    }

    public int insert(String username, String passwordHash, String fullName, String status) throws Exception {
        String sql = "INSERT INTO dbo.users(username, password_hash, full_name, status) VALUES(?,?,?,?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, fullName);
            ps.setString(4, status);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            return -1;
        }
    }

    public void update(int userId, String fullName, String status) throws Exception {
        String sql = "UPDATE dbo.users SET full_name=?, status=? WHERE user_id=?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, status);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public void updatePassword(int userId, String passwordHash) throws Exception {
        String sql = "UPDATE dbo.users SET password_hash=? WHERE user_id=?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void delete(int userId) throws Exception {
        String sql = "DELETE FROM dbo.users WHERE user_id=?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public List<String> listAllActiveUsernames() throws Exception {
        String sql = "SELECT username FROM dbo.users WHERE status=N'ACTIVE' ORDER BY username";
        List<String> out = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(rs.getString(1));
        }
        return out;
    }

    public int idByUsername(String username) throws Exception {
        return findByUsername(username).map(u -> u.userId).orElseThrow();
    }
}