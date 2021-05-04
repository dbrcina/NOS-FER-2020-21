package hr.fer.zemris.nos.lab2.crypto;

import javax.crypto.Cipher;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

public abstract class CryptoAlg {

    private final String name;
    private final int keySize;
    private final String keySizeHex;
    private final Cipher cipher;

    protected CryptoAlg(String name, int keySize, String mode, String padding) throws Exception {
        this.name = name.equalsIgnoreCase("3DES") ? "DESede" : name;
        this.keySize = keySize;
        this.keySizeHex = Utils.intToHex(keySize);
        this.cipher = Cipher.getInstance("%s/%s/%s".formatted(this.name, mode, padding));
    }

    public abstract void generateKey(String saveFile) throws Exception;

    public String encrypt(byte[] plainData, String fileDataName, String saveFile) throws Exception {
        String name = this.name.equals("DESede") ? "3DES" : this.name;
        System.out.printf("Encrypting using '%s'...%n", name);

        initCipherEncryption(cipher);
        byte[] cypherText = cipher.doFinal(plainData);
        String encoded = Base64.getEncoder().encodeToString(cypherText);
        System.out.println("\tEncryption successful!");

        if (saveFile != null) {
            saveFile += ".encrypted";
            Map<ParamType, String[]> params = new TreeMap<>();
            params.put(ParamType.DESCRIPTION, new String[]{"Crypted file"});
            params.put(ParamType.METHOD, new String[]{name});
            params.put(ParamType.KEY_LENGTH, new String[]{keySizeHex});
            if (fileDataName != null) {
                params.put(ParamType.FILE_NAME, new String[]{fileDataName});
            }
            params.put(ParamType.DATA, new String[]{encoded});
            putParamsEncryption(params);
            Utils.writeResults(Paths.get(saveFile), params);
            System.out.printf("\tResults are stored into '%s' file.%n", saveFile);
        }

        return encoded;
    }

    protected abstract void initCipherEncryption(Cipher cipher) throws Exception;

    protected abstract void putParamsEncryption(Map<ParamType, String[]> params);

    public String getName() {
        return name;
    }

    public int getKeySize() {
        return keySize;
    }

    public String getKeySizeHex() {
        return keySizeHex;
    }

}
