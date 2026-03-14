package server.ui;

import server.config.AppConfig;
import server.net.FileTransferServer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ServerMainFrame extends JFrame {

    private final JTextArea runtimeLog = new JTextArea();
    private final JTextField portField = new JTextField(String.valueOf(AppConfig.DEFAULT_PORT), 6);
    private final JButton startBtn = new JButton("Start");
    private final JButton stopBtn = new JButton("Stop");
    private final JLabel onlineLabel = new JLabel("Online: 0");

    private final FileTransferServer server = new FileTransferServer();

    public ServerMainFrame() {
        setTitle("FileTransfer Server (Host/Admin) - SQL Server");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);

        server.setListener(new FileTransferServer.Listener() {
            @Override public void onLog(String s) {
                SwingUtilities.invokeLater(() -> {
                    runtimeLog.append(s + "\n");
                    runtimeLog.setCaretPosition(runtimeLog.getDocument().getLength());
                });
            }
            @Override public void onOnlineChanged(int count) {
                SwingUtilities.invokeLater(() -> onlineLabel.setText("Online: " + count));
            }
        });

        setLayout(new BorderLayout());
        add(buildTopBar(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);

        stopBtn.setEnabled(false);
        hookActions();
    }

    private JComponent buildTopBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(10, 12, 10, 12));

        JLabel title = new JLabel("Server Control");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.add(new JLabel("Port:"));
        right.add(portField);
        right.add(startBtn);
        right.add(stopBtn);
        right.add(Box.createHorizontalStrut(12));
        right.add(onlineLabel);

        p.add(title, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JComponent buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Admin (CRUD + Search)", new AdminPanel());
        tabs.addTab("Runtime Log", buildRuntimeLog());
        return tabs;
    }

    private JComponent buildRuntimeLog() {
        runtimeLog.setEditable(false);
        runtimeLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return new JScrollPane(runtimeLog);
    }

    private void hookActions() {
        startBtn.addActionListener(e -> {
            int port;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Port không hợp lệ!");
                return;
            }

            try {
                server.start(port);
                startBtn.setEnabled(false);
                stopBtn.setEnabled(true);
                portField.setEnabled(false);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không start được server: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        stopBtn.addActionListener(e -> {
            server.stop();
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            portField.setEnabled(true);
        });
    }
}