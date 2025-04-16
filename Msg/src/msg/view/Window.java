package msg.view;

import javax.swing.*;
import java.awt.*;

public class Window extends JFrame {
    // Entry per la lista peer
    public static class PeerEntry {
        public String ip;
        public String name;
        public PeerEntry(String ip, String name) {
            this.ip = ip;
            this.name = name;
        }
        @Override
        public String toString() {
            return name + " [" + ip + "]";
        }
    }
    
    private final DefaultListModel<PeerEntry> listModel = new DefaultListModel<>();
    public final JList<PeerEntry> ipList = new JList<>(listModel);
    public final JTextArea chatArea = new JTextArea();
    public final JTextField input = new JTextField();
    public final JButton sendBtn = new JButton("Send");
    public final JLabel myIpLabel = new JLabel();

    // Pannello impostazioni (in alto a destra)
    public final JPanel settingsPanel = new JPanel();
    public final JButton toggleModeBtn = new JButton("Dark Mode");
    public final JTextField manualIpField = new JTextField(10);
    public final JButton setManualIpBtn = new JButton("Set IP");
    public final JTextField renameField = new JTextField(10);
    public final JButton renameBtn = new JButton("Rename");

    public Window(String myIp) {
        setTitle("LAN Multi Chat");
        setSize(800, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        myIpLabel.setText("Your IP: " + myIp);
        add(myIpLabel, BorderLayout.NORTH);

        // Sidebar e area chat
        JPanel mainPanel = new JPanel(new BorderLayout());
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        JScrollPane listScroll = new JScrollPane(ipList);
        listScroll.setPreferredSize(new Dimension(150, 0));
        mainPanel.add(listScroll, BorderLayout.WEST);
        mainPanel.add(chatScroll, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);

        // Pannello per input e tasto Send (sotto)
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(input, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // Pannello impostazioni (a destra)
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Settings"));
        JPanel modePanel = new JPanel();
        modePanel.add(toggleModeBtn);
        settingsPanel.add(modePanel);
        JPanel manualPanel = new JPanel();
        manualPanel.add(new JLabel("Manual IP:"));
        manualPanel.add(manualIpField);
        manualPanel.add(setManualIpBtn);
        settingsPanel.add(manualPanel);
        JPanel renamePanel = new JPanel();
        renamePanel.add(new JLabel("Rename:"));
        renamePanel.add(renameField);
        renamePanel.add(renameBtn);
        settingsPanel.add(renamePanel);
        add(settingsPanel, BorderLayout.EAST);

        setVisible(true);
    }

    // Metodi per la chat
    public void setChat(String text) {
        chatArea.setText(text);
    }

    public void addChat(String text) {
        chatArea.append(text + "\n");
    }

    public String getInput() {
        return input.getText();
    }

    public void clearInput() {
        input.setText("");
    }

    // Metodi per gestire la lista dei peer
    public void clearPeers() {
        listModel.clear();
    }

    public void addPeer(PeerEntry entry) {
        // Se già presente, aggiorna il nome
        for (int i = 0; i < listModel.size(); i++) {
            PeerEntry pe = listModel.get(i);
            if (pe.ip.equals(entry.ip)) {
                pe.name = entry.name;
                listModel.set(i, pe);
                return;
            }
        }
        listModel.addElement(entry);
    }

    public String getSelectedIp() {
        PeerEntry pe = ipList.getSelectedValue();
        return pe != null ? pe.ip : null;
    }
}
