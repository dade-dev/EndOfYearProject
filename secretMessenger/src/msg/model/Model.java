package msg.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import msg.config.Config;

/**
 * The Model class is responsible for managing chat data, including storing messages,
 * handling chat names, and performing encryption and decryption of messages.
 */
@SuppressWarnings("rawtypes")
public class Model {

    private final SecretKey key;
    private final Map<String, List<Message>> chats = new ConcurrentHashMap<>();
    private final Map<String, String> chatNames = new ConcurrentHashMap<>();

    /**
     * Constructs a new Model, initializing the encryption key.
     * @throws Exception if key derivation fails.
     */
    public Model() throws Exception {
        key = deriveKey(Config.getPassword(), Config.getSalt());
    }

    // Deriva una chiave AES-128 forte dalla password
    private SecretKey deriveKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    /**
     * Encrypts a given string.
     * @param t The string to encrypt.
     * @return The encrypted byte array.
     * @throws Exception if encryption fails.
     */
    public byte[] encrypt(String t) throws Exception {
        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.ENCRYPT_MODE, key);
        return c.doFinal(t.getBytes());
    }

    /**
     * Decrypts a given byte array.
     * @param d The byte array to decrypt.
     * @return The decrypted string.
     * @throws Exception if decryption fails.
     */
    public String decrypt(byte[] d) throws Exception {
        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.DECRYPT_MODE, key);
        return new String(c.doFinal(d));
    }

    /**
     * Adds a text message to the chat with a specific peer.
     * @param peerIp The IP address of the peer.
     * @param msg The message content.
     */
    public void addMessage(String peerIp, String msg) {
        addMessage(peerIp, msg, null);
    }

    /**
     * Adds a message, potentially with image data, to the chat with a specific peer.
     * @param peerIp The IP address of the peer.
     * @param msg The message content (e.g., prefix like "Tu: ").
     * @param data The byte array of the image data, or null if it's a text message.
     */
    public void addMessage(String peerIp, String msg, byte[] data) {
        List<Message> tmp = chats.computeIfAbsent(peerIp, k -> new ArrayList<>());
        try {
            tmp.add(new Message<>(msg, data != null ? ImageIO.read(new ByteArrayInputStream(data)) : null));
        } catch (IOException e) {
        }
    }

    /**
     * Retrieves the chat history for a specific peer.
     * @param peerIp The IP address of the peer.
     * @return A list of messages for the specified peer, or null if no chat exists.
     */
    public List<Message> getChat(String peerIp) {
        return chats.get(peerIp);
    }

    /**
     * Gets the set of all peer IP addresses with whom chats exist.
     * @return A set of peer IP strings.
     */
    public Set<String> getPeers() {
        return chats.keySet();
    }

    /**
     * Sets a custom name for a chat with a specific peer.
     * This also updates the message prefixes for existing image messages in that chat.
     * @param peerIp The IP address of the peer.
     * @param name The new name for the chat.
     */
    public void setChatName(String peerIp, String name) {
        chatNames.put(peerIp, name);
        chats.get(peerIp).stream().filter(m -> m.haveContent()).forEach(m -> {
            if (!m.getMessage().equals("Tu: "))
                m.setMessage(name + ": ");
        }); // For images
    }

    /**
     * Retrieves the display name for a chat with a specific peer.
     * Returns the custom name if set, otherwise returns the peer's IP address.
     * @param peerIp The IP address of the peer.
     * @return The chat name or IP address.
     */
    public String getChatName(String peerIp) {
        return chatNames.getOrDefault(peerIp, peerIp);
    }

    /**
     * Gets a copy of the map containing all custom chat names.
     * @return A map where keys are peer IPs and values are their custom chat names.
     */
    public Map<String, String> getAllChatNames() {
        return new ConcurrentHashMap<>(chatNames);
    }

    /**
     * Removes a peer and their associated chat history and name.
     * @param peerIp The IP address of the peer to remove.
     * @return True if the peer or their chat name was successfully removed, false otherwise.
     */
    public boolean removePeer(String peerIp) {

        // Remove chat history
        List<Message> removedChat = chats.remove(peerIp);

        // Remove chat name
        String removedName = chatNames.remove(peerIp);

        // Return true if anything was removed
        return (removedChat != null || removedName != null);
    }
}