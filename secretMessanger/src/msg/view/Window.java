package msg.view;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyledDocument;
import msg.controller.Controller;

public class Window extends JFrame {
	private static final long serialVersionUID = 1L;
	private DefaultListModel<String> peersModel = new DefaultListModel<>();
    private JList<String> peersList = new JList<>(peersModel);
    private JTextPane chatPane = new JTextPane();
    private JTextField inputArea = new JTextField();
    private JTextField peerIpField = new JTextField(12);
    private JButton sendBtn = new JButton("Invia");
    private JButton sendImage = new JButton("Immagini");
    private JButton addPeerBtn = new JButton("Aggiungi Peer");
    private JButton renameChatBtn = new JButton("Rinomina chat");
    private JToggleButton darkModeBtn = new JToggleButton("Dark Mode");
    private JLabel statusLabel = new JLabel("");
	private Controller controller;

    public Window(Controller c) {
        this.controller = c;
        
        setTitle("SecretMessenger");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        DefaultCaret caret = (DefaultCaret) chatPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        chatPane.setEditable(false);
        chatPane.setFont(new Font("Helvetica Neue", Font.PLAIN, 14));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("Peer"), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(peersList), BorderLayout.CENTER);
        peersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel leftBottom = new JPanel();
        leftBottom.add(peerIpField);
        leftBottom.add(addPeerBtn);
        leftBottom.add(renameChatBtn);
        leftBottom.add(darkModeBtn);
        leftPanel.add(leftBottom, BorderLayout.SOUTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(new JScrollPane(chatPane), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputArea, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.add(sendImage);
        buttonPanel.add(sendBtn);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        centerPanel.add(inputPanel, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        sendBtn.addActionListener(e -> {
            controller.onSendMessage(inputArea.getText());
            clearInput();
        });

        sendImage.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File img = chooser.getSelectedFile();
                try {
                    byte[] bytes = Files.readAllBytes(img.toPath());
                    String b64 = Base64.getEncoder().encodeToString(bytes);
                    controller.onSendMessage("!IMG" + b64);
                } catch (IOException ignored) {}
            }
        });

        addPeerBtn.addActionListener(e -> controller.onAddPeer(peerIpField.getText().trim()));

        renameChatBtn.addActionListener(e -> {
            String sel = peersList.getSelectedValue();
            if (sel != null) {
                String newName = JOptionPane.showInputDialog(this, "Nuovo nome per la chat:");
                if (newName != null && !newName.isBlank())
                    controller.onRenameChat(sel, newName.trim());
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
                    controller.onSendMessage(inputArea.getText());
                    clearInput();
                    e.consume();
                }
            }
        });

        darkModeBtn.addActionListener(e -> toggleDarkMode(darkModeBtn.isSelected()));

        setVisible(true);
    }

    public void appendText(String text) {
        StyledDocument doc = chatPane.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), text + "\n", null);
            chatPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException ignored) {}
    }

    public void appendImage(Image img) {
        StyledDocument doc = chatPane.getStyledDocument();
        try {
            if (img.getWidth(null) > 400) {
                img = img.getScaledInstance(400, -1, Image.SCALE_SMOOTH);
            }
            doc.insertString(doc.getLength(), "\n", null);
            
            chatPane.setCaretPosition(doc.getLength());
            chatPane.insertIcon(new ImageIcon(img));
            
            doc.insertString(doc.getLength(), "\n", null);
            
            chatPane.setCaretPosition(doc.getLength());
            
            // Force the repainting for some bug of Java
            chatPane.revalidate();
            chatPane.repaint();
        } catch (BadLocationException ignored) {}
    }

    public void selectPeer(String display) {
        peersList.setSelectedValue(display, true);
    }

    public void setPeers(List<String> peers) {
        String prev = peersList.getSelectedValue();
        peersModel.clear();
        peers.forEach(peersModel::addElement);
        if (prev != null && peers.contains(prev))
            peersList.setSelectedValue(prev, true);
        else if (!peers.isEmpty())
            peersList.setSelectedIndex(0);
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    public void clearInput() {
        inputArea.setText("");
    }

    private void toggleDarkMode(boolean darkMode) {
        Color bg = darkMode ? new Color(40,40,40) : UIManager.getColor("Panel.background");
        Color fg = darkMode ? new Color(220,220,220) : UIManager.getColor("Label.foreground");
        Color btnBg = darkMode ? new Color(80,80,80) : UIManager.getColor("Button.background");
        Color btnFg = darkMode ? new Color(220,220,220) : UIManager.getColor("Button.foreground");

        getContentPane().setBackground(bg);
        chatPane.setBackground(darkMode ? new Color(60,60,60) : Color.WHITE);
        chatPane.setForeground(fg);
        inputArea.setBackground(darkMode ? new Color(60,60,60) : Color.WHITE);
        inputArea.setForeground(fg);
        peersList.setBackground(darkMode ? new Color(60,60,60) : Color.WHITE);
        peersList.setForeground(fg);
        statusLabel.setForeground(fg);

        for (Component c : new Component[]{sendBtn, sendImage, addPeerBtn, renameChatBtn, darkModeBtn}) {
            c.setBackground(btnBg);
            c.setForeground(btnFg);
        }
        UIManager.put("Button.select", darkMode ? new Color(100,100,100) : UIManager.getColor("Button.select"));

        for (Component comp : getContentPane().getComponents())
            if (comp instanceof JPanel) 
            	updatePanelColors((JPanel)comp, bg, fg, btnBg, btnFg, darkMode);
        repaint();
    }

    public void clearChat() {
        chatPane.setText("");
    }
    
    public String getSelectedPeer() {
        return peersList.getSelectedValue();
    }
    
    public void clearPeerInput() {
        peerIpField.setText("");
    }

    private void updatePanelColors(JPanel panel, Color bg, Color fg, Color btnBg, Color btnFg, boolean darkMode) {
        panel.setBackground(bg);
        for (Component comp : panel.getComponents())
            if (comp instanceof JLabel) 
            	comp.setForeground(fg);
            else if (comp instanceof JPanel) 
            	updatePanelColors((JPanel)comp, bg, fg, btnBg, btnFg, darkMode);
            else if (comp instanceof JScrollPane) {
                comp.setBackground(bg);
                ((JScrollPane)comp).getViewport().setBackground(bg);
            } else if (comp instanceof JButton || comp instanceof JToggleButton) {
                comp.setBackground(btnBg);
                comp.setForeground(btnFg);
            } else if (comp instanceof JTextField) {
                comp.setBackground(darkMode ? new Color(60,60,60) : Color.WHITE);
                comp.setForeground(fg);
            }
    }
}
