package hr.fer.zemris.nos.lab2.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

public class SymmetricAlg extends CryptoAlg {

    private SecretKey secretKey;
    private IvParameterSpec iv;

    public SymmetricAlg(String name, int keySize, String mode) throws Exception {
        super(name, keySize, mode, mode.equals("CTR") ? "NoPadding" : "PKCS5Padding");
    }

    @Override
    public void generateKey(String saveFile) throws Exception {
        System.out.println("Generating secret key...");
        saveFile = saveFile + ".secret";
        String name = getName();
        KeyGenerator keyGenerator = KeyGenerator.getInstance(name);
        keyGenerator.init(getKeySize());
        secretKey = keyGenerator.generateKey();
        Map<ParamType, String[]> params = new TreeMap<>();
        params.put(ParamType.DESCRIPTION, new String[]{"Secret key"});
        params.put(ParamType.METHOD, new String[]{name.equals("DESede") ? "3DES" : name});
        params.put(ParamType.KEY_LENGTH, new String[]{getKeySizeHash()});
        params.put(ParamType.SECRET_KEY, new String[]{Utils.bytesToHex(secretKey.getEncoded())});
        Utils.writeResults(Paths.get(saveFile), params);
        System.out.printf("\tSecret key has been generated and stored into '%s' file.%n", saveFile);
    }

    @Override
    public String encrypt(byte[] plainData, String fileDataName, String saveFile) throws Exception {
        String name = getName();
        System.out.printf("Encrypting using '%s'...%n", name.equals("DESede") ? "3DES" : name);
        if (saveFile != null) {
            saveFile = saveFile + ".encrypted";
        }
        generateIV();
        Cipher cipher = getCipher();
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        byte[] cypherText = cipher.doFinal(plainData);
        String encoded = Base64.getEncoder().encodeToString(cypherText);
        Map<ParamType, String[]> params = new TreeMap<>();
        params.put(ParamType.DESCRIPTION, new String[]{"Crypted file"});
        params.put(ParamType.METHOD, new String[]{name.equals("DESede") ? "3DES" : name});
        params.put(ParamType.KEY_LENGTH, new String[]{getKeySizeHash()});
        if (fileDataName != null) {
            params.put(ParamType.FILE_NAME, new String[]{fileDataName});
        }
        params.put(ParamType.INITIALIZATION_VECTOR, new String[]{Utils.bytesToHex(iv.getIV())});
        params.put(ParamType.DATA, new String[]{encoded});
        System.out.println("\tEncryption successful!");
        if (saveFile != null) {
            Utils.writeResults(Paths.get(saveFile), params);
            System.out.printf("\tResults are stored into '%s' file.%n", saveFile);
        }
        return encoded;
    }

    public void generateIV() {
        int blockSize = getName().equals("AES") ? 16 : 8;
        byte[] bytes = new byte[blockSize];
        new SecureRandom().nextBytes(bytes);
        iv = new IvParameterSpec(bytes);
    }

}
