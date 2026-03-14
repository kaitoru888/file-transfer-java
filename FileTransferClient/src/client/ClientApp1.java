package client;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.SwingUtilities;
import client.ui.LoginFrame;

public class ClientApp1 {
    public static void main(String[] args) {
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}