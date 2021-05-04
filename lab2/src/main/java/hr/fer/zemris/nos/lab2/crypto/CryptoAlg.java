package hr.fer.zemris.nos.lab2.crypto;

import javax.crypto.Cipher;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

public abstract class CryptoAlg {

    private final String name;
    private final int keySize;
    private final String keySizeHex;
    private final String mode;
    private final Cipher cipher;

    protected CryptoAlg(String name, int keySize, String mode, String padding) throws Exception {
        this.name = name;
        this.keySize = keySize;
        this.keySizeHex = Utils.intToHex(keySize);
        this.mode = mode;
        this.cipher = Cipher.getInstance("%s/%s/%s".formatted(name, mode, padding));
    }

    public abstract void generateKey(String saveFile) throws Exception;

    public String encrypt(byte[] data, String sourceFile, String saveFile) throws Exception {
        System.out.printf("Encrypting using '%s'...%n", name);
        initCipher(true);
        byte[] cypherText = cipher.doFinal(data);
        String encoded = Base64.getEncoder().encodeToString(cypherText);
        System.out.println("Encryption successful!");

        if (saveFile != null) {
            saveFile += ".encrypted";
            Map<ParamType, String[]> params = new TreeMap<>();
            params.put(ParamType.DESCRIPTION, new String[]{"Crypted file"});
            params.put(ParamType.METHOD, new String[]{name});
            params.put(ParamType.KEY_LENGTH, new String[]{keySizeHex});
            if (sourceFile != null) {
                params.put(ParamType.FILE_NAME, new String[]{sourceFile});
            }
            params.put(ParamType.DATA, new String[]{encoded});
            putParamsEncryption(params);
            Utils.writeResults(Paths.get(saveFile), params);
            System.out.printf("Results are stored into '%s' file.%n", saveFile);
        }

        return encoded;
    }

    protected abstract void initCipher(boolean encryption) throws Exception;

    protected abstract void putParamsEncryption(Map<ParamType, String[]> params);

    public byte[] decrypt(byte[] encoded, String saveFile) throws Exception {
        System.out.printf("Decrypting using '%s'...%n", name);
        initCipher(false);
        byte[] data = cipher.doFinal(Base64.getDecoder().decode(encoded));
        System.out.println("Decryption successful!");

        if (saveFile != null) {
            saveFile += ".decrypted";
            Files.write(Paths.get(saveFile), data);
            System.out.printf("Results are stored into '%s' file.%n", saveFile);
        }

        return data;
    }

    public String getName() {
        return name;
    }

    public int getKeySize() {
        return keySize;
    }

    public String getKeySizeHex() {
        return keySizeHex;
    }

    protected String getMode() {
        return mode;
    }

    protected Cipher getCipher() {
        return cipher;
    }

}
