package hr.fer.zemris.nos.lab2.crypto;

import hr.fer.zemris.nos.lab2.Utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

public class SymmetricAlg {

    private final String name;
    private final int keySize;
    private final Cipher cipher;

    private SecretKey secretKey;
    private IvParameterSpec iv;

    public SymmetricAlg(String name, int keySize, String mode) throws Exception {
        this.name = name.equals("3DES") ? "DESede" : name;
        this.keySize = keySize;
        String instance = "%s/%s/%s".formatted(this.name, mode, mode.equals("CTR") ? "NoPadding" : "PKCS5Padding");
        this.cipher = Cipher.getInstance(instance);
    }

    public void generateKey(String saveFile) throws Exception {
        System.out.println("Generating secret key...");
        KeyGenerator keyGenerator = KeyGenerator.getInstance(name);
        keyGenerator.init(keySize);
        secretKey = keyGenerator.generateKey();
        Map<ParamType, String[]> params = new TreeMap<>();
        params.put(ParamType.DESCRIPTION, new String[]{"Secret key"});
        params.put(ParamType.METHOD, new String[]{name.equals("DESede") ? "3DES" : name});
        params.put(ParamType.KEY_LENGTH, new String[]{Utils.intToHex(keySize)});
        params.put(ParamType.SECRET_KEY, new String[]{Utils.bytesToHex(secretKey.getEncoded())});
        Utils.writeResults(Paths.get(saveFile), params);
        System.out.println("Secret key has been generated and stored into " + saveFile);
    }

    public void generateIV() {
        int blockSize = name.equals("AES") ? 16 : 8;
        byte[] bytes = new byte[blockSize];
        new SecureRandom().nextBytes(bytes);
        iv = new IvParameterSpec(bytes);
    }

    public void encrypt(String fileToEncrypt, String saveFile) throws Exception {
        System.out.println("Encrypting file...");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        byte[] cypherText = cipher.doFinal(Files.readAllBytes(Paths.get(fileToEncrypt)));
        String encoded = Base64.getEncoder().encodeToString(cypherText);
        Map<ParamType, String[]> params = new TreeMap<>();
        params.put(ParamType.DESCRIPTION, new String[]{"Crypted file"});
        params.put(ParamType.METHOD, new String[]{name.equals("DESede") ? "3DES" : name});
        params.put(ParamType.KEY_LENGTH, new String[]{Utils.intToHex(keySize)});
        params.put(ParamType.FILE_NAME, new String[]{fileToEncrypt});
        params.put(ParamType.DATA, new String[]{encoded});
        Utils.writeResults(Paths.get(saveFile), params);
        System.out.println("Encryption successful! Results are stored into " + saveFile);
    }

}
