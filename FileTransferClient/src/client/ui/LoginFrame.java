package client.ui;

import client.net.ServerConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {

    private final JTextField hostField = new JTextField("localhost", 18);
    private final JTextField portField = new JTextField("9090", 6);
    private final JTextField userField = new JTextField("user1", 18);
    private final JPasswordField passField = new JPasswordField("123456", 18);
    private final JButton loginBtn = new JButton("Login");

    public LoginFrame() {
        setTitle("FileTransfer Client - Login");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(420, 320);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(16,16,16,16));

        JLabel title = new JLabel("File Transfer");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6,6,6,6);
        g.anchor = GridBagConstraints.WEST;

        int r = 0;
        addRow(form, g, r++, "Host:", hostField);
        addRow(form, g, r++, "Port:", portField);
        addRow(form, g, r++, "Username:", userField);
        addRow(form, g, r++, "Password:", passField);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(loginBtn);

        root.add(title, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);

        loginBtn.addActionListener(e -> onLogin());
        getRootPane().setDefaultButton(loginBtn);
    }

    private void addRow(JPanel form, GridBagConstraints g, int r, String label, JComponent comp) {
        g.gridx = 0; g.gridy = r;
        form.add(new JLabel(label), g);
        g.gridx = 1;
        form.add(comp, g);
    }

    private void onLogin() {
        String host = hostField.getText().trim();
        int port;
        try { port = Integer.parseInt(portField.getText().trim()); }
        catch (Exception e) { JOptionPane.showMessageDialog(this, "Port không hợp lệ!"); return; }

        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());

        loginBtn.setEnabled(false);

        SwingWorker<Void,Void> w = new SwingWorker<>() {
            ServerConnection conn;
            ServerConnection.LoginResult res;

            @Override protected Void doInBackground() throws Exception {
                conn = new ServerConnection();
                conn.connect(host, port);
                res = conn.login(user, pass);
                return null;
            }

            @Override protected void done() {
                loginBtn.setEnabled(true);
                try {
                    get();
                    if (!res.ok) {
                        conn.close();
                        JOptionPane.showMessageDialog(LoginFrame.this, res.message,
                                "Login failed", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    ClientMainFrame mf = new ClientMainFrame(conn);
                    mf.setVisible(true);
                    dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(LoginFrame.this, "Không kết nối được: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }
}