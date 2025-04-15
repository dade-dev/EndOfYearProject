package msg.model;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;

public class Model {
    private final List<String> msgs = new ArrayList<>();
    private final SecretKey key;

    public Model() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(128);
        key = kg.generateKey();
    }

    public void addMsg(String m) {
        msgs.add(m);
    }

    public List<String> getMsgs() {
        return msgs;
    }

    public byte[] enc(String m) throws Exception {
        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.ENCRYPT_MODE, key);
        return c.doFinal(m.getBytes());
    }

    public String dec(byte[] data) throws Exception {
        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.DECRYPT_MODE, key);
        return new String(c.doFinal(data));
    }

    public SecretKey getKey() {
        return key;
    }
}
