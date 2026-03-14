package server.ui;

import javax.swing.*;
import java.awt.*;

public class UserFormPanel extends JPanel {
    private final JTextField username = new JTextField(18);
    private final JPasswordField password = new JPasswordField(18);
    private final JTextField fullName = new JTextField(18);
    private final JComboBox<String> status = new JComboBox<>(new String[]{"ACTIVE", "BLOCKED"});

    // init = null => add
    // init = {username, fullName, status} => edit
    public UserFormPanel(Object[] init) {
        setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5,5,5,5);
        g.anchor = GridBagConstraints.WEST;

        int r=0;
        addRow(g, r++, "Username:", username);
        addRow(g, r++, "Password:", password);
        addRow(g, r++, "Full name:", fullName);
        addRow(g, r++, "Status:", status);

        if (init != null) {
            username.setText(String.valueOf(init[0]));
            username.setEnabled(false);
            password.setEnabled(false);
            password.setText("");
            fullName.setText(String.valueOf(init[1]));
            status.setSelectedItem(String.valueOf(init[2]));
        }
    }

    private void addRow(GridBagConstraints g, int r, String label, JComponent comp){
        g.gridx=0; g.gridy=r;
        add(new JLabel(label), g);
        g.gridx=1;
        add(comp, g);
    }

    public String getUsername(){ return username.getText().trim(); }
    public String getPassword(){ return new String(password.getPassword()); }
    public String getFullName(){ return fullName.getText().trim(); }
    public String getStatus(){ return String.valueOf(status.getSelectedItem()); }
}