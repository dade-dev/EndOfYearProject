package msg.model;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.*;

public class Model {
    private final SecretKey key;
    private final Map<String, List<String>> chats = new HashMap<>();

    public Model() throws Exception {
        key = KeyGenerator.getInstance("AES").generateKey();
    }

    public byte[] enc(String t) throws Exception {
        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.ENCRYPT_MODE, key);
        return c.doFinal(t.getBytes());
    }

    public String dec(byte[] d) throws Exception {
        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.DECRYPT_MODE, key);
        return new String(c.doFinal(d));
    }

    public void addMsg(String ip, String msg) {
        chats.computeIfAbsent(ip, k -> new ArrayList<>()).add(msg);
    }

    public String getChat(String ip) {
        return String.join("\n", chats.getOrDefault(ip, new ArrayList<>()));
    }
}
