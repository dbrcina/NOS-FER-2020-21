package hr.fer.zemris.nos.lab2.crypto;

import javax.crypto.Cipher;

public abstract class CryptoAlg {

    protected final String name;
    protected final int keySize;
    protected final Cipher cipher;

    protected CryptoAlg(String name, int keySize, String mode, String padding) throws Exception {
        this.name = name.equalsIgnoreCase("3DES") ? "DESede" : name;
        this.keySize = keySize;
        this.cipher = Cipher.getInstance("%s/%s/%s".formatted(this.name, mode, padding));
    }

    public abstract void generateKey(String saveFile) throws Exception;

    public abstract String encrypt(String fileToEncrypt, String saveFile) throws Exception;

}
