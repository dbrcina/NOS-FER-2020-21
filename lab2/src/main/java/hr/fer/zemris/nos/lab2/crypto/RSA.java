package hr.fer.zemris.nos.lab2.crypto;

import hr.fer.zemris.nos.lab2.Utils;

import javax.crypto.Cipher;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

public class RSA extends CryptoAlg {

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;

    public RSA(int keySize) throws Exception {
        super("RSA", keySize, "ECB", "PKCS1Padding");
    }

    @Override
    public void generateKey(String saveFile) throws Exception {
        System.out.println("Generating RSA keys...");
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(name);
        keyPairGenerator.initialize(keySize);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        String keyLength = Utils.intToHex(keySize);
        String modulus = Utils.bytesToHex(publicKey.getModulus().toByteArray());
        String publicExponent = Utils.bytesToHex(publicKey.getPublicExponent().toByteArray());
        String privateExponent = Utils.bytesToHex(privateKey.getPrivateExponent().toByteArray());
        saveKeys(true, keyLength, modulus, publicExponent, saveFile + ".pub");
        saveKeys(false, keyLength, modulus, privateExponent, saveFile + ".priv");
    }

    private void saveKeys(
            boolean isPublic, String keyLength, String modulus, String exponent, String saveFile) throws IOException {
        String description = (isPublic ? "Public" : "Private") + " key";
        Map<ParamType, String[]> params = new TreeMap<>();
        params.put(ParamType.DESCRIPTION, new String[]{description});
        params.put(ParamType.METHOD, new String[]{name});
        params.put(ParamType.KEY_LENGTH, new String[]{keyLength});
        params.put(ParamType.MODULUS, new String[]{modulus});
        params.put(isPublic ? ParamType.PUBLIC_EXPONENT : ParamType.PRIVATE_EXPONENT, new String[]{exponent});
        Utils.writeResults(Paths.get(saveFile), params);
        System.out.println("\t" + description + " has been generated and stored into '" + saveFile + "' file.");
    }

    @Override
    public String encrypt(String fileToEncrypt, String saveFile) throws Exception {
        return null;
    }

    public String encrypt(byte[] data, String saveFile, boolean withPublic) throws Exception {
        saveFile = saveFile + ".encrypted";
        System.out.println("Encrypting using " + (withPublic ? "public" : "private") + " key...");
        cipher.init(Cipher.ENCRYPT_MODE, withPublic ? publicKey : privateKey);
        byte[] cypherText = cipher.doFinal(data);
        String encoded = Base64.getEncoder().encodeToString(cypherText);
        Map<ParamType, String[]> params = new TreeMap<>();
        params.put(ParamType.DESCRIPTION, new String[]{"Crypted file"});
        params.put(ParamType.METHOD, new String[]{name});
        params.put(ParamType.KEY_LENGTH, new String[]{Utils.intToHex(keySize)});
        params.put(ParamType.DATA, new String[]{encoded});
        Utils.writeResults(Paths.get(saveFile), params);
        System.out.println("\tEncryption successful! Results are stored into '" + saveFile + "' file.");
        return encoded;
    }

}
