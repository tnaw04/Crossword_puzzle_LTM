package client;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLightLaf;

import client.gui.LoginWindow;
import client.network.Client;


public class MainClient {
    public static void main(String[] args) {
      
        SwingUtilities.invokeLater(() -> {
            try {
               
                UIManager.setLookAndFeel(new FlatLightLaf());
            } catch (Exception e) {
                e.printStackTrace();
            }

    
            String hostname = "localhost"; 
            int port = 12345;             
          
            Client client = new Client(hostname, port);

            
            LoginWindow loginWindow = new LoginWindow(client);
            client.setLoginWindow(loginWindow); 

          
            loginWindow.setVisible(true);

            client.connect();
        });
    }
}
