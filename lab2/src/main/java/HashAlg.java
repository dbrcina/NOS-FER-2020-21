import java.security.MessageDigest;

public class HashAlg {

    private final String name;
    private final String keySizeHex;
    private final MessageDigest messageDigest;

    public HashAlg(String name, int keySize) throws Exception {
        this.name = name;
        this.keySizeHex = Utils.intToHex(keySize);
        this.messageDigest = MessageDigest.getInstance(name + "-" + keySize);
    }

    public byte[] digest(byte[] input) {
        return messageDigest.digest(input);
    }

    public String getName() {
        return name;
    }

    public String getKeySizeHex() {
        return keySizeHex;
    }

}
