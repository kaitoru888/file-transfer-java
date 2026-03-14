package server.ui;

import server.dao.FileDao;
import server.dao.LogDao;
import server.dao.UserDao;
import server.model.FileRecord;
import server.model.LogRecord;
import server.model.User;
import server.security.PasswordUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AdminPanel extends JPanel {

    private final UserDao userDao = new UserDao();
    private final FileDao fileDao = new FileDao();
    private final LogDao logDao = new LogDao();

    // Users
    private final JTextField userSearch = new JTextField(20);
    private final DefaultTableModel usersModel = new DefaultTableModel(
            new String[]{"ID","Username","Full name","Status"}, 0
    ){ public boolean isCellEditable(int r,int c){ return false; } };
    private final JTable usersTable = new JTable(usersModel);

    // Files
    private final JTextField fileSearch = new JTextField(20);
    private final DefaultTableModel filesModel = new DefaultTableModel(
            new String[]{"FileID","File","Size","Sender","Receiver","Status","SentAt"}, 0
    ){ public boolean isCellEditable(int r,int c){ return false; } };
    private final JTable filesTable = new JTable(filesModel);

    // Logs
    private final JTextField logSearch = new JTextField(20);
    private final DefaultTableModel logsModel = new DefaultTableModel(
            new String[]{"LogID","Action","FileID","Result","Message","CreatedAt"}, 0
    ){ public boolean isCellEditable(int r,int c){ return false; } };
    private final JTable logsTable = new JTable(logsModel);

    public AdminPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10,10,10,10));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Users (CRUD)", buildUsersTab());
        tabs.addTab("Files (Search/Delete)", buildFilesTab());
        tabs.addTab("Logs (Search)", buildLogsTab());

        add(tabs, BorderLayout.CENTER);

        refreshUsers();
        refreshFiles();
        refreshLogs();
    }

    private JComponent buildUsersTab() {
        JPanel root = new JPanel(new BorderLayout(10,10));

        JPanel top = new JPanel(new BorderLayout());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.add(new JLabel("Search:"));
        left.add(userSearch);
        JButton btnFind = new JButton("Find");
        JButton btnRefresh = new JButton("Refresh");
        left.add(btnFind); left.add(btnRefresh);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnAdd = new JButton("Add");
        JButton btnEdit = new JButton("Edit");
        JButton btnReset = new JButton("Reset Pass");
        JButton btnDel = new JButton("Delete");
        right.add(btnAdd); right.add(btnEdit); right.add(btnReset); right.add(btnDel);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        usersTable.setRowHeight(26);
        JScrollPane sp = new JScrollPane(usersTable);

        root.add(top, BorderLayout.NORTH);
        root.add(sp, BorderLayout.CENTER);

        btnFind.addActionListener(e -> refreshUsers());
        btnRefresh.addActionListener(e -> { userSearch.setText(""); refreshUsers(); });

        btnAdd.addActionListener(e -> onAddUser());
        btnEdit.addActionListener(e -> onEditUser());
        btnReset.addActionListener(e -> onResetPass());
        btnDel.addActionListener(e -> onDeleteUser());

        return root;
    }

    private JComponent buildFilesTab() {
        JPanel root = new JPanel(new BorderLayout(10,10));

        JPanel top = new JPanel(new BorderLayout());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.add(new JLabel("Search:"));
        left.add(fileSearch);
        JButton btnFind = new JButton("Find");
        JButton btnRefresh = new JButton("Refresh");
        left.add(btnFind); left.add(btnRefresh);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnDelete = new JButton("Delete file");
        right.add(btnDelete);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        filesTable.setRowHeight(26);
        JScrollPane sp = new JScrollPane(filesTable);

        root.add(top, BorderLayout.NORTH);
        root.add(sp, BorderLayout.CENTER);

        btnFind.addActionListener(e -> refreshFiles());
        btnRefresh.addActionListener(e -> { fileSearch.setText(""); refreshFiles(); });
        btnDelete.addActionListener(e -> onDeleteFile());

        return root;
    }

    private JComponent buildLogsTab() {
        JPanel root = new JPanel(new BorderLayout(10,10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.add(new JLabel("Search:"));
        top.add(logSearch);
        JButton btnFind = new JButton("Find");
        JButton btnRefresh = new JButton("Refresh");
        top.add(btnFind); top.add(btnRefresh);

        logsTable.setRowHeight(26);
        JScrollPane sp = new JScrollPane(logsTable);

        root.add(top, BorderLayout.NORTH);
        root.add(sp, BorderLayout.CENTER);

        btnFind.addActionListener(e -> refreshLogs());
        btnRefresh.addActionListener(e -> { logSearch.setText(""); refreshLogs(); });

        return root;
    }

    private void refreshUsers() {
        try {
            usersModel.setRowCount(0);
            List<User> list = userDao.search(userSearch.getText());
            for (User u : list) {
                usersModel.addRow(new Object[]{u.userId, u.username, u.fullName, u.status});
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Load users lỗi: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshFiles() {
        try {
            filesModel.setRowCount(0);
            List<FileRecord> list = fileDao.searchAdmin(fileSearch.getText());
            for (FileRecord f : list) {
                filesModel.addRow(new Object[]{
                        f.fileId, f.fileName, f.fileSize, f.senderUsername, f.receiverUsername,
                        f.status, (f.sentAt == null ? "" : f.sentAt.toString())
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Load files lỗi: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshLogs() {
        try {
            logsModel.setRowCount(0);
            List<LogRecord> list = logDao.search(logSearch.getText());
            for (LogRecord l : list) {
                String msg = (l.message == null) ? "" : l.message;
                if (msg.length() > 80) msg = msg.substring(0, 80) + "...";
                logsModel.addRow(new Object[]{
                        l.logId, l.action, l.fileId, l.result, msg, (l.createdAt == null ? "" : l.createdAt.toString())
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Load logs lỗi: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onAddUser() {
        UserFormPanel form = new UserFormPanel(null);
        int ok = JOptionPane.showConfirmDialog(this, form, "Add User",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        String u = form.getUsername();
        String p = form.getPassword();
        if (u.isBlank() || p.isBlank()) {
            JOptionPane.showMessageDialog(this, "Username/Password không được rỗng!");
            return;
        }

        try {
            userDao.insert(u, PasswordUtil.hashNew(p), form.getFullName(), form.getStatus());
            refreshUsers();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Add user lỗi: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onEditUser() {
        int row = usersTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Chọn user để sửa!"); return; }

        int id = (int) usersModel.getValueAt(row, 0);
        String username = (String) usersModel.getValueAt(row, 1);
        String fullName = (String) usersModel.getValueAt(row, 2);
        String status = (String) usersModel.getValueAt(row, 3);

        UserFormPanel form = new UserFormPanel(new Object[]{username, fullName, status});
        int ok = JOptionPane.showConfirmDialog(this, form, "Edit User",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        try {
            userDao.update(id, form.getFullName(), form.getStatus());
            refreshUsers();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Edit lỗi: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onResetPass() {
        int row = usersTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Chọn user để reset pass!"); return; }

        int id = (int) usersModel.getValueAt(row, 0);
        String username = (String) usersModel.getValueAt(row, 1);

        String newPass = JOptionPane.showInputDialog(this, "Nhập password mới cho " + username + ":");
        if (newPass == null || newPass.isBlank()) return;

        try {
            userDao.updatePassword(id, PasswordUtil.hashNew(newPass));
            JOptionPane.showMessageDialog(this, "Đã reset password!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Reset lỗi: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDeleteUser() {
        int row = usersTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Chọn user để xóa!"); return; }

        int id = (int) usersModel.getValueAt(row, 0);
        String username = (String) usersModel.getValueAt(row, 1);

        int ok = JOptionPane.showConfirmDialog(this, "Xóa user " + username + " ?",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        try {
            userDao.delete(id);
            refreshUsers();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Xóa không được (có thể user đã có file/log). Bạn nên chuyển BLOCKED thay vì xóa.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDeleteFile() {
        int row = filesTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Chọn file để xóa!"); return; }

        int fileId = (int) filesModel.getValueAt(row, 0);
        String fileName = (String) filesModel.getValueAt(row, 1);

        int ok = JOptionPane.showConfirmDialog(this, "Xóa file " + fileName + " (DB + ổ đĩa)?",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        try {
            String path = fileDao.serverPathById(fileId);
            fileDao.delete(fileId);
            if (path != null) {
                try { Files.deleteIfExists(Path.of(path)); } catch (Exception ignored) {}
            }
            refreshFiles();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Xóa file lỗi: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}