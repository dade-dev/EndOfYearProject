package msg.view;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

import msg.controller.Controller;

public class Window extends JFrame {
    private DefaultListModel<String> peersModel = new DefaultListModel<>();
    private JList<String> peersList = new JList<>(peersModel);
    private JTextArea chatArea = new JTextArea();
    private JTextArea inputArea = new JTextArea(2, 30);
    private JButton sendBtn = new JButton("Invia");
    private JButton addPeerBtn = new JButton("Aggiungi Peer");
    private JButton renameChatBtn = new JButton("Rinomina chat"); // NEW
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
        chatArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("Peer"), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(peersList), BorderLayout.CENTER);
        JPanel addPanel = new JPanel();
        addPanel.add(peerIpField); addPanel.add(addPeerBtn); addPanel.add(renameChatBtn);
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
        
        setVisible(true);
    }

    
    
    public void setPeers(List<String> peers) {
        String selected = getSelectedPeer();
        peersModel.clear();
        for (String p : peers) peersModel.addElement(p);
        if (selected != null && peers.contains(selected)) {
            peersList.setSelectedValue(selected, true);
        }
    }
    
    public void setChat(List<String> chatLines) {
        chatArea.setText(String.join("\n", chatLines));
    }
    
    public void appendChat(String line) {
        chatArea.append(line + "\n");
    }
    
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