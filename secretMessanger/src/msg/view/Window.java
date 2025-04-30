package msg.view;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter; // Import FileNameExtensionFilter
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyledDocument;
import msg.controller.Controller;
import java.awt.GridBagConstraints; // Import GridBagConstraints
import java.awt.GridBagLayout; // Import GridBagLayout
import java.awt.Insets; // Import Insets

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
	private JButton removePeerBtn = new JButton("Rimuovi Peer");
	private JButton darkModeBtn = new JButton("Dark Mode"); // Changed from JToggleButton
	private JLabel statusLabel = new JLabel("");
	private Controller controller;
	private boolean isDarkMode = true; // Added state variable //Starts in darkmode ahh my eyes

	public Window(Controller c) {
		this.controller = c;

		setTitle("SecretMessenger");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(1200, 800);
		setLocationRelativeTo(null);
		DefaultCaret caret = (DefaultCaret) chatPane.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		chatPane.setEditable(false); // Set non-editable
		chatPane.setFont(new Font("Helvetica Neue", Font.PLAIN, 14));

		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(new JLabel("Peer"), BorderLayout.NORTH);
		leftPanel.add(new JScrollPane(peersList), BorderLayout.CENTER);
		peersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// --- Start of leftBottom panel changes ---
		JPanel leftBottom = new JPanel(new GridBagLayout()); // Use GridBagLayout
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL; // Make components fill horizontally
		gbc.insets = new Insets(2, 5, 2, 5); // Add some padding
		gbc.gridx = 0; // All components in the same column
		gbc.weightx = 1.0; // Allow horizontal expansion

		// Add peerIpField
		gbc.gridy = 0;
		leftBottom.add(peerIpField, gbc);

		// Add addPeerBtn
		gbc.gridy = 1;
		leftBottom.add(addPeerBtn, gbc);

		// Add renameChatBtn
		gbc.gridy = 2;
		leftBottom.add(renameChatBtn, gbc);

		// Add removePeerBtn
		gbc.gridy = 3;
		leftBottom.add(removePeerBtn, gbc);

		// Add darkModeBtn
		gbc.gridy = 4;
		leftBottom.add(darkModeBtn, gbc);

		leftPanel.add(leftBottom, BorderLayout.SOUTH);
		// --- End of leftBottom panel changes ---

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
			// Add a file filter for images
			FileNameExtensionFilter imageFilter = new FileNameExtensionFilter(
				"Image files (jpg, jpeg, png, gif, bmp)", "jpg", "jpeg", "png", "gif", "bmp");
			chooser.setFileFilter(imageFilter);
			chooser.setAcceptAllFileFilterUsed(false); // Optionally disable the "All Files" option

			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				File img = chooser.getSelectedFile();
				try {
					byte[] bytes = Files.readAllBytes(img.toPath());
					String b64 = Base64.getEncoder().encodeToString(bytes);
					controller.onSendMessage("!IMG" + b64);
				} catch (IOException ignored) {
				}
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

		removePeerBtn.addActionListener(e -> {
			String sel = peersList.getSelectedValue();
			if (sel != null) controller.onRemovePeer(sel);
		});

		peersList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting())
				controller.onPeerSelected(peersList.getSelectedValue());
		});

		peerIpField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					controller.onAddPeer(peerIpField.getText().trim());
					e.consume();
				}
			}
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

		darkModeBtn.addActionListener(e -> { // Changed listener logic
			isDarkMode = !isDarkMode;
			toggleDarkMode();
		});

		// Apply initial theme before showing the window
		toggleDarkMode(); // Apply initial theme (dark mode)
		setVisible(true);
		
	}

	public void appendText(String text) {
		StyledDocument doc = chatPane.getStyledDocument();
		try {
			doc.insertString(doc.getLength(), text + "\n", null);
			chatPane.setCaretPosition(doc.getLength());
		} catch (BadLocationException ignored) {
		}
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
		} catch (BadLocationException ignored) {
		}
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

	private void toggleDarkMode() { // Corrected implementation
		Color bg = isDarkMode ? new Color(40, 40, 40) : UIManager.getColor("Panel.background");
		Color fg = isDarkMode ? new Color(220, 220, 220) : UIManager.getColor("Label.foreground");
		Color btnBg = isDarkMode ? new Color(80, 80, 80) : UIManager.getColor("Button.background");
		Color btnFg = isDarkMode ? Color.WHITE : Color.BLACK;
		Color selectBg = isDarkMode ? new Color(100, 100, 100) : UIManager.getColor("List.selectionBackground"); // Use List selection background
		Color listBg = isDarkMode ? new Color(60, 60, 60) : Color.WHITE;

		darkModeBtn.setText(isDarkMode ? "Light Mode" : "Dark Mode"); // Update button text

		getContentPane().setBackground(bg);

		chatPane.setBackground(listBg);
		chatPane.setForeground(fg);
		inputArea.setBackground(listBg);
		inputArea.setForeground(fg);
		peersList.setBackground(listBg);
		peersList.setForeground(fg);
		peersList.setSelectionBackground(selectBg);
		peersList.setSelectionForeground(fg); // Ensure selected text is visible
		statusLabel.setForeground(fg);

		// Apply styles to all relevant buttons
		JButton[] buttonsToStyle = { sendBtn, sendImage, addPeerBtn, renameChatBtn, darkModeBtn };
		for (JButton btn : buttonsToStyle) { // Iterate over JButtons
			btn.setBackground(btnBg);
			btn.setForeground(btnFg);
			btn.setOpaque(true);
			btn.setBorderPainted(false);
			btn.setFocusPainted(false);
		}

		// Update colors recursively for panels and components
		updateComponentColors((JPanel) getContentPane(), bg, fg, btnBg, btnFg, listBg, isDarkMode);
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

	// Renamed and corrected recursive update method
	private void updateComponentColors(Container container, Color bg, Color fg, Color btnBg, Color btnFg, Color listBg, boolean darkMode) {
		container.setBackground(bg);
		container.setForeground(fg);

		for (Component comp : container.getComponents()) {
			comp.setBackground(bg);
			comp.setForeground(fg);

			if (comp instanceof JButton) {
				JButton btn = (JButton) comp;
				// Avoid restyling buttons already handled if needed, though reapplying is usually fine
				// Check if it's one of the main buttons if specific styling is needed
				boolean isMainButton = false;
				JButton[] mainButtons = { sendBtn, sendImage, addPeerBtn, renameChatBtn, darkModeBtn };
				for(JButton mainBtn : mainButtons) {
					if (btn == mainBtn) {
						isMainButton = true;
						break;
					}
				}
				if (isMainButton) {
					btn.setBackground(btnBg);
					btn.setForeground(btnFg);
					btn.setOpaque(true);
					btn.setBorderPainted(false);
					btn.setFocusPainted(false);
				} else {
					// Style other potential JButtons if necessary
					btn.setBackground(btnBg); // Apply default button style
					btn.setForeground(btnFg);
				}
			} else if (comp instanceof JTextField) {
				comp.setBackground(listBg); // Use listBg for text fields
				comp.setForeground(fg);
			} else if (comp instanceof JTextPane) {
				comp.setBackground(listBg);
				comp.setForeground(fg);
			} else if (comp instanceof JList) {
				JList<?> list = (JList<?>) comp;
				list.setBackground(listBg);
				list.setForeground(fg);
				list.setSelectionBackground(isDarkMode ? new Color(100, 100, 100) : UIManager.getColor("List.selectionBackground"));
				list.setSelectionForeground(fg);
			} else if (comp instanceof JScrollPane) {
				JScrollPane scrollPane = (JScrollPane) comp;
				scrollPane.getViewport().setBackground(listBg); // Background for viewport content
				scrollPane.setBackground(bg); // Scroll pane background itself
				// Style scrollbars if needed (more complex)
			} else if (comp instanceof JLabel) {
				comp.setForeground(fg); // Only set foreground for labels
			} else if (comp instanceof JPanel) { // Recurse into JPanels
				updateComponentColors((JPanel) comp, bg, fg, btnBg, btnFg, listBg, darkMode);
			} else if (comp instanceof Container) { // Recurse for other container types
                updateComponentColors((Container) comp, bg, fg, btnBg, btnFg, listBg, darkMode);
            }
		}
	}
}
