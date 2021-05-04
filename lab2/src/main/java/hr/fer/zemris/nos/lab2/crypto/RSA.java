package hr.fer.zemris.nos.lab2.crypto;

import javax.crypto.Cipher;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.TreeMap;

public class RSA extends CryptoAlg {

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private boolean withPrivate;

    public RSA(int keySize) throws Exception {
        super("RSA", keySize, "ECB", "PKCS1Padding");
    }

    @Override
    public void generateKey(String saveFile) throws Exception {
        System.out.println("Generating RSA keys...");
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(getName());
        keyPairGenerator.initialize(getKeySize());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        String modulus = Utils.bytesToHex(publicKey.getModulus().toByteArray());
        String publicExponent = Utils.bytesToHex(publicKey.getPublicExponent().toByteArray());
        String privateExponent = Utils.bytesToHex(privateKey.getPrivateExponent().toByteArray());
        saveKeys(true, modulus, publicExponent, saveFile + ".pub");
        saveKeys(false, modulus, privateExponent, saveFile + ".priv");
    }

    private void saveKeys(boolean isPublic, String modulus, String exponent, String saveFile) throws IOException {
        String description = (isPublic ? "Public" : "Private") + " key";
        Map<ParamType, String[]> params = new TreeMap<>();
        params.put(ParamType.DESCRIPTION, new String[]{description});
        params.put(ParamType.METHOD, new String[]{getName()});
        params.put(ParamType.KEY_LENGTH, new String[]{getKeySizeHex()});
        params.put(ParamType.MODULUS, new String[]{modulus});
        params.put(isPublic ? ParamType.PUBLIC_EXPONENT : ParamType.PRIVATE_EXPONENT, new String[]{exponent});
        Utils.writeResults(Paths.get(saveFile), params);
        System.out.printf("\t%s has been generated and stored into '%s' file.%n", description, saveFile);
    }

    @Override
    public String encrypt(byte[] data, String sourceFile, String saveFile) throws Exception {
        return encrypt(data, sourceFile, saveFile, false);
    }

    public String encrypt(byte[] data, String sourceFile, String saveFile, boolean withPrivate) throws Exception {
        this.withPrivate = withPrivate;
        return super.encrypt(data, sourceFile, saveFile);
    }

    @Override
    public byte[] decrypt(byte[] encoded, String saveFile) throws Exception {
        return decrypt(encoded, saveFile, true);
    }

    public byte[] decrypt(byte[] encoded, String saveFile, boolean withPrivate) throws Exception {
        this.withPrivate = withPrivate;
        return super.decrypt(encoded, saveFile);
    }

    @Override
    protected void initCipher(boolean encryption) throws Exception {
        getCipher().init(encryption ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, withPrivate ? privateKey : publicKey);
    }

    @Override
    protected void putParamsEncryption(Map<ParamType, String[]> params) {
        // DO NOTHING.
    }

}
