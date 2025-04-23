package msg.view;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;

import msg.controller.Controller;

public class Window extends JFrame {
    private DefaultListModel<String> peersModel = new DefaultListModel<>();
    private JList<String> peersList = new JList<>(peersModel);
    private JTextArea chatArea = new JTextArea();
    private JTextArea inputArea = new JTextArea(2, 30);
    private JButton sendBtn = new JButton("Invia");
    private JButton addPeerBtn = new JButton("Aggiungi Peer");
    private JButton renameChatBtn = new JButton("Rinomina chat"); // NEW
    private JToggleButton darkModeBtn = new JToggleButton("Dark Mode");
    private JTextField peerIpField = new JTextField(12);
    private JLabel statusLabel = new JLabel("Benvenuto in SecretMessenger!");
    private Controller controller;

    public Window(Controller controller) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {}
        this.controller=controller;
        setTitle("SecretMessenger");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        chatArea.setEditable(false);
        chatArea.setFont(new Font("Helvetica Neue", Font.PLAIN, 14));
        
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("Peer"), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(peersList), BorderLayout.CENTER);
        JPanel addPanel = new JPanel();
        addPanel.add(peerIpField); 
        addPanel.add(addPeerBtn); 
        addPanel.add(renameChatBtn);
        addPanel.add(darkModeBtn);
        leftPanel.add(addPanel, BorderLayout.SOUTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputArea, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);
        centerPanel.add(inputPanel, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        sendBtn.addActionListener(e -> controller.onSendMessage(getInput()));
        addPeerBtn.addActionListener(e -> controller.onAddPeer(peerIpField.getText().trim()));
        renameChatBtn.addActionListener(e -> {
            if (controller != null && getSelectedPeer() != null) {
                String newName = JOptionPane.showInputDialog(this, "Nuovo nome per la chat:", "Rinomina chat", JOptionPane.PLAIN_MESSAGE);
                if (newName != null && !newName.isBlank())
                    controller.onRenameChat(getSelectedPeer(), newName.trim());
            }
        });
        peersList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                controller.onPeerSelected(peersList.getSelectedValue());
        });
        
        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isShiftDown()) {
                        inputArea.setText(inputArea.getText() + "\n");
                    } else {
                        controller.onSendMessage(getInput());
                        e.consume();
                    }
                }
            }
        });
        
        // Add dark mode toggle functionality
        darkModeBtn.addActionListener(e -> toggleDarkMode(darkModeBtn.isSelected()));
        
        setVisible(true);
    }
    
    // Add dark mode toggle method
    private void toggleDarkMode(boolean darkMode) {
        Color bgColor = darkMode ? new Color(40, 40, 40) : UIManager.getColor("Panel.background");
        Color textColor = darkMode ? new Color(220, 220, 220) : UIManager.getColor("Label.foreground");
        Color buttonBgColor = darkMode ? new Color(80, 80, 80) : UIManager.getColor("Button.background");
        Color buttonTextColor = darkMode ? new Color(220, 220, 220) : UIManager.getColor("Button.foreground");
        
        // Update main components
        getContentPane().setBackground(bgColor);
        chatArea.setBackground(darkMode ? new Color(60, 60, 60) : Color.WHITE);
        chatArea.setForeground(textColor);
        inputArea.setBackground(darkMode ? new Color(60, 60, 60) : Color.WHITE);
        inputArea.setForeground(textColor);
        peersList.setBackground(darkMode ? new Color(60, 60, 60) : Color.WHITE);
        peersList.setForeground(textColor);
        statusLabel.setForeground(textColor);
        
        // Update buttons - properly handle light mode by using system defaults
        sendBtn.setBackground(buttonBgColor);
        sendBtn.setForeground(buttonTextColor);
        addPeerBtn.setBackground(buttonBgColor);
        addPeerBtn.setForeground(buttonTextColor);
        renameChatBtn.setBackground(buttonBgColor);
        renameChatBtn.setForeground(buttonTextColor);
        darkModeBtn.setBackground(buttonBgColor);
        darkModeBtn.setForeground(buttonTextColor);
        
        // Fix for the blue highlight on selected buttons
        if (darkMode) {
            UIManager.put("Button.select", new Color(100, 100, 100));
        } else {
            UIManager.put("Button.select", UIManager.getColor("Button.select"));
        }
        
        peerIpField.setBackground(darkMode ? new Color(60, 60, 60) : Color.WHITE);
        peerIpField.setForeground(textColor);
        
        // Update all panels
        for (Component comp : getContentPane().getComponents()) {
            if (comp instanceof JPanel) {
                updatePanelColors((JPanel)comp, bgColor, textColor, buttonBgColor, buttonTextColor, darkMode);
            }
        }
        repaint();
    }
    
    private void updatePanelColors(JPanel panel, Color bgColor, Color textColor, 
                                  Color buttonBgColor, Color buttonTextColor, boolean darkMode) {
        panel.setBackground(bgColor);
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JLabel) {
                comp.setForeground(textColor);
            } else if (comp instanceof JPanel) {
                updatePanelColors((JPanel)comp, bgColor, textColor, buttonBgColor, buttonTextColor, darkMode);
            } else if (comp instanceof JScrollPane) {
                comp.setBackground(bgColor);
                ((JScrollPane)comp).getViewport().setBackground(bgColor);
            } else if (comp instanceof JButton || comp instanceof JToggleButton) {
                comp.setBackground(buttonBgColor);
                comp.setForeground(buttonTextColor);
            } else if (comp instanceof JTextField) {
                comp.setBackground(darkMode ? new Color(60, 60, 60) : Color.WHITE);
                comp.setForeground(textColor);
            }
        }
    }
    
    
    
    public void setPeers(List<String> peers) {
        String selected = getSelectedPeer();
        peersModel.clear();
        for (String p : peers) peersModel.addElement(p);
        
        // Select the previously selected peer if it still exists
        if (selected != null && peers.contains(selected)) {
            peersList.setSelectedValue(selected, true);
        } 
        // Otherwise select the first peer if the list is not empty
        else if (!peers.isEmpty()) {
            peersList.setSelectedIndex(0);
        }
    }
    
    // Remove this duplicate method
    // public void setChat(List<String> chatLines) {
    //     chatArea.setText(String.join("\n", chatLines));
    // }
    
    public void appendChat(String line) {
        chatArea.append(line + "\n");
    }
    
    public void setChat(List<String> chatLines) {
        chatArea.setText(String.join("\n", chatLines));
    }
    
    // Removing the processEmojis method
    
    public void setStatus(String text) {
        statusLabel.setText(text);
    }
    
    public void clearInput() {
        inputArea.setText("");
    }
    
    public void clearPeerInput() {
        peerIpField.setText("");
    }
    
    public String getInput() {
        return inputArea.getText();
    }
    
    public String getSelectedPeer() {
        return peersList.getSelectedValue();
    }
}