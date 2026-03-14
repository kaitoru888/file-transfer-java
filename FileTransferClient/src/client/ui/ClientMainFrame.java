package client.ui;

import client.net.ServerConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ClientMainFrame extends JFrame {

    private final ServerConnection conn;

    // Users tab
    private final JButton refreshUsersBtn = new JButton("Refresh users");
    private final DefaultTableModel usersModel = new DefaultTableModel(
            new String[]{"Username","Online"}, 0
    ){ public boolean isCellEditable(int r,int c){ return false; } };
    private final JTable usersTable = new JTable(usersModel);

    // Send tab
    private final JComboBox<String> receiverCombo = new JComboBox<>();
    private final JTextField fileField = new JTextField(28);
    private final JButton chooseBtn = new JButton("Choose");
    private final JButton sendBtn = new JButton("Send");
    private final JProgressBar progress = new JProgressBar(0, 100);
    private File selectedFile;

    // History tab
    private final JTextField historySearch = new JTextField(22);
    private final JButton historyBtn = new JButton("Search");
    private final DefaultTableModel historyModel = new DefaultTableModel(
            new String[]{"ID","File","Size","Sender","Receiver","Status","SentAt"}, 0
    ){ public boolean isCellEditable(int r,int c){ return false; } };
    private final JTable historyTable = new JTable(historyModel);

    public ClientMainFrame(ServerConnection conn) {
        this.conn = conn;

        setTitle("FileTransfer Client - " + conn.getUsername());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1150, 720);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        add(buildTopBar(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Users", buildUsersTab());
        tabs.addTab("Send", buildSendTab());
        tabs.addTab("History", buildHistoryTab());

        add(tabs, BorderLayout.CENTER);

        // listener nhận file push/pull
        conn.setIncomingFileListener((fileId, sender, fileName, size, bytes) -> {
            SwingUtilities.invokeLater(() -> onIncomingFile(sender, fileName, bytes));
        });

        // auto pull pending lúc mở app
        SwingUtilities.invokeLater(() -> {
            try {
                int pulled = conn.pullPendingAndHandle();
                if (pulled > 0) JOptionPane.showMessageDialog(this, "Bạn có " + pulled + " file chờ, đã tải về.");
            } catch (Exception ignored) {}
        });

        refreshUsers();
        refreshHistory("");
    }

    private JComponent buildTopBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(10, 12, 10, 12));

        JLabel title = new JLabel("File Transfer Client");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton pullBtn = new JButton("Pull pending");
        JButton disconnectBtn = new JButton("Disconnect");

        right.add(pullBtn);
        right.add(disconnectBtn);

        pullBtn.addActionListener(e -> {
            try {
                int pulled = conn.pullPendingAndHandle();
                JOptionPane.showMessageDialog(this, "Đã tải " + pulled + " file chờ.");
                refreshHistory("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        disconnectBtn.addActionListener(e -> {
            conn.close();
            dispose();
            new LoginFrame().setVisible(true);
        });

        p.add(title, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JComponent buildUsersTab() {
        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(10,10,10,10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.add(refreshUsersBtn);

        usersTable.setRowHeight(26);
        JScrollPane sp = new JScrollPane(usersTable);

        root.add(top, BorderLayout.NORTH);
        root.add(sp, BorderLayout.CENTER);

        refreshUsersBtn.addActionListener(e -> refreshUsers());

        return root;
    }

    private JComponent buildSendTab() {
        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(10,10,10,10));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6,6,6,6);
        g.anchor = GridBagConstraints.WEST;

        int r=0;
        addRow(form, g, r++, "Receiver:", receiverCombo);

        fileField.setEditable(false);
        JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        fileRow.add(fileField);
        fileRow.add(chooseBtn);
        addRow(form, g, r++, "File:", fileRow);

        progress.setStringPainted(true);
        progress.setValue(0);
        addRow(form, g, r++, "Progress:", progress);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btnRow.add(sendBtn);
        addRow(form, g, r++, "", btnRow);

        chooseBtn.addActionListener(e -> onChooseFile());
        sendBtn.addActionListener(e -> onSend());

        root.add(form, BorderLayout.NORTH);
        return root;
    }

    private JComponent buildHistoryTab() {
        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(10,10,10,10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.add(new JLabel("Keyword:"));
        top.add(historySearch);
        top.add(historyBtn);

        historyTable.setRowHeight(26);
        JScrollPane sp = new JScrollPane(historyTable);

        historyBtn.addActionListener(e -> refreshHistory(historySearch.getText()));

        root.add(top, BorderLayout.NORTH);
        root.add(sp, BorderLayout.CENTER);
        return root;
    }

    private void addRow(JPanel form, GridBagConstraints g, int r, String label, Component comp) {
        g.gridx=0; g.gridy=r;
        if (!label.isBlank()) form.add(new JLabel(label), g);
        g.gridx=1;
        form.add(comp, g);
    }

    private void refreshUsers() {
        refreshUsersBtn.setEnabled(false);
        SwingWorker<List<ServerConnection.UserOnline>,Void> w = new SwingWorker<>() {
            @Override protected List<ServerConnection.UserOnline> doInBackground() throws Exception {
                return conn.listUsers();
            }
            @Override protected void done() {
                refreshUsersBtn.setEnabled(true);
                try {
                    var list = get();
                    usersModel.setRowCount(0);
                    receiverCombo.removeAllItems();
                    for (var u : list) {
                        usersModel.addRow(new Object[]{u.username, u.online ? "Yes" : "No"});
                        if (!u.username.equals(conn.getUsername())) receiverCombo.addItem(u.username);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ClientMainFrame.this, "Load users lỗi: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }

    private void onChooseFile() {
        JFileChooser fc = new JFileChooser();
        int ok = fc.showOpenDialog(this);
        if (ok != JFileChooser.APPROVE_OPTION) return;
        selectedFile = fc.getSelectedFile();
        fileField.setText(selectedFile.getAbsolutePath());
        progress.setValue(0);
    }

    private void onSend() {
        String receiver = (String) receiverCombo.getSelectedItem();
        if (receiver == null || receiver.isBlank()) {
            JOptionPane.showMessageDialog(this, "Chọn người nhận!");
            return;
        }
        if (selectedFile == null || !selectedFile.exists()) {
            JOptionPane.showMessageDialog(this, "Chọn file cần gửi!");
            return;
        }

        sendBtn.setEnabled(false);
        progress.setValue(0);

        SwingWorker<ServerConnection.SendResult,Void> w = new SwingWorker<>() {
            @Override protected ServerConnection.SendResult doInBackground() throws Exception {
                return conn.sendFile(receiver, selectedFile, (sent, total) -> {
                    int pct = (int) Math.round((sent * 100.0) / Math.max(1, total));
                    SwingUtilities.invokeLater(() -> progress.setValue(Math.min(100, pct)));
                });
            }
            @Override protected void done() {
                sendBtn.setEnabled(true);
                try {
                    var res = get();
                    JOptionPane.showMessageDialog(ClientMainFrame.this, res.message,
                            res.ok ? "OK" : "Error",
                            res.ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                    refreshHistory("");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ClientMainFrame.this, "Send lỗi: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }

    private void refreshHistory(String keyword) {
        SwingWorker<List<ServerConnection.HistoryRow>,Void> w = new SwingWorker<>() {
            @Override protected List<ServerConnection.HistoryRow> doInBackground() throws Exception {
                return conn.history(keyword);
            }
            @Override protected void done() {
                try {
                    var list = get();
                    historyModel.setRowCount(0);
                    for (var r : list) {
                        historyModel.addRow(new Object[]{
                                r.fileId, r.fileName, r.size, r.sender, r.receiver, r.status, r.sentAt
                        });
                    }
                } catch (Exception ignored) {}
            }
        };
        w.execute();
    }

    private void onIncomingFile(String sender, String fileName, byte[] bytes) {
        int ok = JOptionPane.showConfirmDialog(this,
                "Nhận file từ: " + sender + "\n" + fileName + "\nLưu file?",
                "Incoming file", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(fileName));
        int choose = fc.showSaveDialog(this);
        if (choose != JFileChooser.APPROVE_OPTION) return;

        try {
            Path p = fc.getSelectedFile().toPath();
            Files.write(p, bytes);
            JOptionPane.showMessageDialog(this, "Đã lưu: " + p.toAbsolutePath());
            refreshHistory("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Không lưu được: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}