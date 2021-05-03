package hr.fer.zemris.nos.lab2.crypto;

import java.security.MessageDigest;

public class HashAlg {

    private final String name;
    private final int keySize;
    private final String keySizeHash;
    private final MessageDigest messageDigest;

    public HashAlg(String name, int keySize) throws Exception {
        this.name = name;
        this.keySize = keySize;
        this.keySizeHash = Utils.intToHex(keySize);
        this.messageDigest = MessageDigest.getInstance(name + "-" + keySize);
    }

    public byte[] digest(byte[] input) {
        return messageDigest.digest(input);
    }

    public String getName() {
        return name;
    }

    public String getKeySizeHash() {
        return keySizeHash;
    }

}
