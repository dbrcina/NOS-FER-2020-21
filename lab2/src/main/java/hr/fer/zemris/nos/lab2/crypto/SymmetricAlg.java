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
        params.put(ParamType.METHOD, new String[]{name});
        params.put(ParamType.KEY_LENGTH, new String[]{getKeySizeHex()});
        params.put(ParamType.SECRET_KEY, new String[]{Utils.bytesToHex(getSecretKey())});
        Utils.writeResults(Paths.get(saveFile), params);
        System.out.printf("Secret key has been generated and stored into '%s' file.%n", saveFile);
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
        params.put(ParamType.MODE, new String[]{getMode()});
        params.put(ParamType.INITIALIZATION_VECTOR, new String[]{Utils.bytesToHex(iv.getIV())});
    }

    private void generateIV() {
        byte[] bytes = new byte[getCipher().getBlockSize()];
        new SecureRandom().nextBytes(bytes);
        iv = new IvParameterSpec(bytes);
    }

    public byte[] getSecretKey() {
        return secretKey.getEncoded();
    }

    public void setKey(byte[] key) {
        secretKey = new SecretKeySpec(key, getName());
    }

}
