package msg.view;

import javax.swing.*;
import java.awt.*;

public class Window extends JFrame {
    public final DefaultListModel<String> listModel = new DefaultListModel<>();
    public final JList<String> ipList = new JList<>(listModel);
    public final JTextArea chatArea = new JTextArea();
    public final JTextField input = new JTextField();
    public final JButton sendBtn = new JButton("Send");
    public final JLabel ipLabel = new JLabel();

    public Window(String ip) {
        setTitle("LAN Multi Chat");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        ipLabel.setText("Your IP: " + ip);
        add(ipLabel, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout());

        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        JScrollPane listScroll = new JScrollPane(ipList);

        listScroll.setPreferredSize(new Dimension(150, 0));
        mainPanel.add(listScroll, BorderLayout.WEST);
        mainPanel.add(chatScroll, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(input, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

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

    public void addIP(String ip) {
        if (!listModel.contains(ip)) listModel.addElement(ip);
    }

    public String getSelectedIP() {
        return ipList.getSelectedValue();
    }
}
