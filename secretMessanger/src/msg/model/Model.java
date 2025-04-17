package msg.model;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.*;

public class Model {
    private final SecretKey key;
    private final Map<String, List<String>> chats = new HashMap<>();
    private final Map<String, String> chatNames = new HashMap<>(); // NEW

    public Model() throws Exception {
        key = KeyGenerator.getInstance("AES").generateKey();
    }

    public byte[] encrypt(String t) throws Exception {
        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.ENCRYPT_MODE, key);
        return c.doFinal(t.getBytes());
    }

    public String decrypt(byte[] d) throws Exception {
    	System.out.println("[MODEL] DECRYPTING");
        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.DECRYPT_MODE, key);
        return new String(c.doFinal(d));
    }

    public void addMessage(String peerIp, String msg) {
        chats.computeIfAbsent(peerIp, k -> new ArrayList<>()).add(msg);
    }

    public List<String> getChat(String peerIp) {
        return new ArrayList<>(chats.getOrDefault(peerIp, new ArrayList<>()));
    }

    public Set<String> getPeers() {
        return chats.keySet();
    }

    // -------- Naming methods --------
    public void setChatName(String peerIp, String name) {
        chatNames.put(peerIp, name);
    }

    public String getChatName(String peerIp) {
        return chatNames.getOrDefault(peerIp, peerIp);
    }

    public Map<String, String> getAllChatNames() {
        return new HashMap<>(chatNames);
    }
}