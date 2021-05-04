package hr.fer.zemris.nos.lab2.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Map;
import java.util.TreeMap;

public class SymmetricAlg extends CryptoAlg {

    private SecretKey secretKey;
    private byte[] secretKeyBytes;
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
        secretKeyBytes = secretKey.getEncoded();
        Map<ParamType, String[]> params = new TreeMap<>();
        params.put(ParamType.DESCRIPTION, new String[]{"Secret key"});
        params.put(ParamType.METHOD, new String[]{name.equals("DESede") ? "3DES" : name});
        params.put(ParamType.KEY_LENGTH, new String[]{getKeySizeHex()});
        params.put(ParamType.SECRET_KEY, new String[]{Utils.bytesToHex(secretKeyBytes)});
        Utils.writeResults(Paths.get(saveFile), params);
        System.out.printf("\tSecret key has been generated and stored into '%s' file.%n", saveFile);
    }

    @Override
    protected void initCipher(boolean encryption) throws Exception {
        Cipher cipher = getCipher();
        int opMode = encryption ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
        boolean ecbMode = getMode().equalsIgnoreCase("ecb");
        if (ecbMode) {
            cipher.init(opMode, secretKey);
        } else {
            if (encryption) {
                generateIV();
            }
            cipher.init(opMode, secretKey, iv);
        }
    }

    @Override
    protected void putParamsEncryption(Map<ParamType, String[]> params) {
        params.put(ParamType.INITIALIZATION_VECTOR, new String[]{Utils.bytesToHex(iv.getIV())});
    }

    public void generateIV() {
//        int blockSize = getName().equals("AES") ? 16 : 8;
        byte[] bytes = new byte[getCipher().getBlockSize()];
        new SecureRandom().nextBytes(bytes);
        iv = new IvParameterSpec(bytes);
    }

    public byte[] getSecretKeyBytes() {
        return secretKeyBytes;
    }

    public void setKey(byte[] key) {
        secretKey = new SecretKeySpec(key, getName());
        secretKeyBytes = secretKey.getEncoded();
    }

}
