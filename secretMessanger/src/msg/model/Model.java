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

@SuppressWarnings("rawtypes")
public class Model {

    private final SecretKey key;
    private final Map<String, List<Message>> chats = new ConcurrentHashMap<>();
    private final Map<String, String> chatNames = new ConcurrentHashMap<>();

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

    public byte[] encrypt(String t) throws Exception {
        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.ENCRYPT_MODE, key);
        return c.doFinal(t.getBytes());
    }

    public String decrypt(byte[] d) throws Exception {
        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.DECRYPT_MODE, key);
        return new String(c.doFinal(d));
    }


    public void addMessage(String peerIp, String msg) {
    	addMessage(peerIp, msg,null);
    }
    public void addMessage(String peerIp, String msg, byte[] data) {
        List<Message> tmp = chats.computeIfAbsent(peerIp, k -> new ArrayList<>());
        try {
            tmp.add(new Message<>(msg, data!=null ? ImageIO.read(new ByteArrayInputStream(data)) : null));
        } catch (IOException e) {}
    }

    public List<Message> getChat(String peerIp) {
        return chats.get(peerIp);
    }

    public Set<String> getPeers() {
        return chats.keySet();
    }

    public void setChatName(String peerIp, String name) {
        chatNames.put(peerIp, name);
        chats.get(peerIp).stream().filter(m -> m.haveContent()).forEach(m -> {
            if(m.getMessage().contains(peerIp))
                m.setMessage(name+": ");
        });// For images, for msg we send prefix + message 
    }

    public String getChatName(String peerIp) {
        return chatNames.getOrDefault(peerIp, peerIp);
    }

    public Map<String, String> getAllChatNames() {
        return new ConcurrentHashMap<>(chatNames);
    }

    public boolean removePeer(String peerIp) {

        // Remove chat history
        List<Message> removedChat = chats.remove(peerIp);

        // Remove chat name
        String removedName = chatNames.remove(peerIp);


        // Return true if anything was removed
        return (removedChat != null || removedName != null);
    }
}