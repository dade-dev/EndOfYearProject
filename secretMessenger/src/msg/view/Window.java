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

/**
 * Represents the main window of the SecretMessenger application.
 * This class handles the user interface for chat, peer management, and other functionalities.
 */
public class Window extends JFrame {
	private static final long serialVersionUID = 1L;
	private final DefaultListModel<String> peersModel = new DefaultListModel<>();
	private final JList<String> peersList = new JList<>(peersModel);
	private final JTextPane chatPane = new JTextPane();
	private final JTextField inputArea = new JTextField();
	private final JTextField peerIpField = new JTextField(12);
	private final JButton sendBtn = new JButton("Invia");
	private final JButton sendImage = new JButton("Immagini");
	private final JButton addPeerBtn = new JButton("Aggiungi Peer");
	private final JButton renameChatBtn = new JButton("Rinomina chat");
	private final JButton removePeerBtn = new JButton("Rimuovi Peer");
	private final JButton darkModeBtn = new JButton("Dark Mode"); // Changed from JToggleButton
	private final JLabel statusLabel = new JLabel("");
	private Controller controller;
	private boolean isDarkMode = true; // Added state variable //Starts in darkmode ahh my eyes
	private final JLabel peerStatusLabel = new JLabel("");

	/**
	 * Constructs the main window.
	 * @param c The controller for handling user actions.
	 */
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
		
		JPanel chatHeaderPanel = new JPanel(new BorderLayout());
		JLabel chatTitleLabel = new JLabel("Chat");
		chatHeaderPanel.add(chatTitleLabel, BorderLayout.WEST);
		
		peerStatusLabel.setOpaque(true);
		peerStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		peerStatusLabel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
		peerStatusLabel.setVisible(false); //Nascosto inizialmente
		chatHeaderPanel.add(peerStatusLabel, BorderLayout.EAST);
		
		centerPanel.add(chatHeaderPanel, BorderLayout.NORTH);
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

		// Replace original listener with suppression logic
        peersList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) { return; }
            String selectedValue = peersList.getSelectedValue();
            if (selectedValue != null) {
                clearChat(); // Clear chat here 
                controller.onPeerSelected(selectedValue);
            }
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
		toggleDarkMode(); // (dark mode)
		
	}

	/**
	 * Appends a text message to the chat pane.
	 * @param text The text message to append.
	 */
	public void appendText(String text) {
		SwingUtilities.invokeLater(() -> {
			StyledDocument doc = chatPane.getStyledDocument();
			try {
				doc.insertString(doc.getLength(), text + "\n", null);
				chatPane.setCaretPosition(doc.getLength());
			} catch (BadLocationException ignored) {}
		});
	}

	/**
	 * Appends an image to the chat pane.
	 * @param img The image to append.
	 */
	public void appendImage(Image img) {
		SwingUtilities.invokeLater(() -> {
			StyledDocument doc = chatPane.getStyledDocument();
			try {
				doc.insertString(doc.getLength(), "\n", null);
	
				chatPane.setCaretPosition(doc.getLength());
				chatPane.insertIcon(new ImageIcon(img.getScaledInstance(400, -1, Image.SCALE_SMOOTH)));
	
				doc.insertString(doc.getLength(), "\n", null);
	
				chatPane.setCaretPosition(doc.getLength());
	
				// Force the repainting for some bug of Java
				chatPane.revalidate();
				chatPane.repaint();
			} catch (BadLocationException ignored) {}
		});
	}

	/**
	 * Updates the status of the selected peer (Online/Offline).
	 * @param online True if the peer is online, false otherwise.
	 */
	public void updatePeerStatus(boolean online) {
	    if (online) {
	        peerStatusLabel.setText("Online");
	        peerStatusLabel.setBackground(new Color(76, 175, 80)); //Verde
	        peerStatusLabel.setForeground(Color.WHITE);
	    } else {
	        peerStatusLabel.setText("Offline");
	        peerStatusLabel.setBackground(new Color(244, 67, 54)); //Rosso
	        peerStatusLabel.setForeground(Color.WHITE);
	    }
	    peerStatusLabel.setVisible(true);
	}

	/**
	 * Selects a peer in the peer list.
	 * @param display The display name of the peer to select.
	 */
	public void selectPeer(String display) {
		peersList.setSelectedValue(display, true);
		peerStatusLabel.setVisible(false);
	}

	/**
	 * Sets the list of peers to be displayed.
	 * @param peers A list of peer display names.
	 */
	public void setPeers(List<String> peers) {
		peersModel.clear();
		peers.forEach(peersModel::addElement);
	}

	/**
	 * Sets the status message displayed at the bottom of the window.
	 * @param text The status text to display.
	 */
	public void setStatus(String text) {
		statusLabel.setText(text);
	}

	/**
	 * Clears the message input area.
	 */
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
		
		SwingUtilities.invokeLater(() -> { updateComponentColors(getContentPane(), bg, fg, btnBg, btnFg, listBg, selectBg, isDarkMode);});
		
		darkModeBtn.setText(isDarkMode ? "Light Mode" : "Dark Mode"); // Update button text
		
		repaint();
	}

	/**
	 * Clears the chat pane.
	 */
	public void clearChat() {
		chatPane.setText("");
	}

	/**
	 * Gets the currently selected peer from the list.
	 * @return The display name of the selected peer, or null if no peer is selected.
	 */
	public String getSelectedPeer() {
		return peersList.getSelectedValue();
	}

	/**
	 * Clears the peer IP input field.
	 */
	public void clearPeerInput() {
		peerIpField.setText("");
	}

	// Renamed and corrected recursive update method
	private void updateComponentColors(Container container, Color bg, Color fg, Color btnBg, Color btnFg, Color listBg, Color selectBg, boolean darkMode) {
		container.setBackground(bg);
		container.setForeground(fg);
		
		for (Component comp : container.getComponents()) {
			comp.setBackground(bg);
			comp.setForeground(fg);

			if (comp instanceof JButton btn) {
				btn.setBackground(btnBg);
				btn.setForeground(btnFg);
				btn.setOpaque(true);
				btn.setBorderPainted(false);
				btn.setFocusPainted(false);
			} else if (comp instanceof JTextField textField) {
				textField.setBackground(listBg); // Use listBg for text fields
				textField.setForeground(fg);
				textField.setCaretColor(fg);
			} else if (comp instanceof JTextPane textPane) {
				textPane.setBackground(listBg);
				textPane.setForeground(fg);
				textPane.setCaretColor(fg);
			} else if (comp instanceof JList list) {
				list.setBackground(listBg);
				list.setForeground(fg);
				list.setSelectionBackground(selectBg);
				list.setSelectionForeground(fg);
			} else if (comp instanceof JScrollPane scrollPane) {
				scrollPane.setBackground(bg); // Scroll pane background itself
				updateComponentColors(scrollPane.getViewport(), bg, fg, btnBg, btnFg, listBg, selectBg,darkMode);
			} else if (comp instanceof JLabel label) {
				if (label != peerStatusLabel) {
					comp.setForeground(fg); // Only set foreground for labels
				}
			} else if (comp instanceof JPanel pane) { // Recurse into JPanels
				updateComponentColors(pane, bg, fg, btnBg, btnFg, listBg, selectBg,darkMode);
			} else if (comp instanceof Container cont) { // Recurse for other container types
                updateComponentColors(cont, bg, fg, btnBg, btnFg, listBg,selectBg, darkMode);
            } else {
            	throw new RuntimeException("Unhandled component: " + comp.getClass().getName());
            }
		}
	}
}
