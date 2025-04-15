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
        key = KeyGenerator.getInstance("AES").generateKey();
    }

    public void addMsg(String t) {
        msgs.add(t);
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
}
