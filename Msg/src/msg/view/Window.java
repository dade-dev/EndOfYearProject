package msg.view;

import javax.swing.*;
import java.awt.*;

public class Window extends JFrame {
    public final JTextArea area = new JTextArea();
    public final JTextField field = new JTextField();
    public final JButton btn = new JButton("Send");
    public final JLabel ipLabel = new JLabel();

    public Window(String ip) {
        setTitle("LAN Chat");
        setSize(400, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        area.setEditable(false);
        add(new JScrollPane(area), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(field, BorderLayout.CENTER);
        bottom.add(btn, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        ipLabel.setText("Your IP: " + ip);
        add(ipLabel, BorderLayout.NORTH);

        setVisible(true);
    }

    public void addLine(String t) {
        area.append(t + "\n");
    }

    public String getText() {
        return field.getText();
    }

    public void clearText() {
        field.setText("");
    }
}
