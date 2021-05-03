package hr.fer.zemris.nos.lab2.crypto;

import javax.crypto.Cipher;

public abstract class CryptoAlg {

    private final String name;
    private final int keySize;
    private final String keySizeHash;
    private final Cipher cipher;

    protected CryptoAlg(String name, int keySize, String mode, String padding) throws Exception {
        this.name = name.equalsIgnoreCase("3DES") ? "DESede" : name;
        this.keySize = keySize;
        this.keySizeHash = Utils.intToHex(keySize);
        this.cipher = Cipher.getInstance("%s/%s/%s".formatted(this.name, mode, padding));
    }

    public abstract void generateKey(String saveFile) throws Exception;

    public abstract String encrypt(byte[] plainData, String fileDataName, String saveFile) throws Exception;

    public String getName() {
        return name;
    }

    public int getKeySize() {
        return keySize;
    }

    public String getKeySizeHash() {
        return keySizeHash;
    }

    protected Cipher getCipher() {
        return cipher;
    }

}
