package server;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.SwingUtilities;
import server.ui.ServerMainFrame;

public class ServerApp {
    public static void main(String[] args) {
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> new ServerMainFrame().setVisible(true));
    }
}