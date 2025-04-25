package msg.model;

import java.awt.image.BufferedImage;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.ByteArrayInputStream;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;

public class Model {
	
	private static final String PASSWORD = "endofyearproject";
    private static final byte[] SALT = { 3, 14, 15, 9, 26, 5, 35, 89, 79, 32, 38, 46, 26, 43, 38, 32 };
	
    private final SecretKey key;
    private final Map<String, List<String>> chats = new HashMap<>();
    private final Map<String, String> chatNames = new HashMap<>();
    private final Map<String, ArrayList<BufferedImage>> images = new HashMap<>();

    public Model() throws Exception {
        key = deriveKey(PASSWORD, SALT);
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
    
    public void addImage(String senderIp, byte[] data) {
    	try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
            if (img != null) {
            	ArrayList<BufferedImage> tmp = images.get(senderIp);
                if (tmp == null) {
                    tmp = new ArrayList<>();
                }
                tmp.add(img);
                images.put(senderIp, tmp);
            }
        } catch (Exception ignored) {}
    }

    public void addMessage(String peerIp, String msg) {
    	ArrayList<String> tmp = (ArrayList<String>) chats.get(peerIp);
        if (tmp == null) {
            tmp = new ArrayList<>();
        }
        tmp.add(msg);
        chats.put(peerIp, tmp);
    }

    public List<String> getChat(String peerIp) {
    	ArrayList<String> tmp = new ArrayList<>();
    	tmp = (ArrayList<String>) this.chats.get(peerIp);
    	return tmp;
    }

    public Set<String> getPeers() {
        return chats.keySet();
    }

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