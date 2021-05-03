package hr.fer.zemris.nos.lab2.crypto;

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
        params.put(ParamType.KEY_LENGTH, new String[]{getKeySizeHash()});
        params.put(ParamType.MODULUS, new String[]{modulus});
        params.put(isPublic ? ParamType.PUBLIC_EXPONENT : ParamType.PRIVATE_EXPONENT, new String[]{exponent});
        Utils.writeResults(Paths.get(saveFile), params);
        System.out.printf("\t%s has been generated and stored into '%s' file.%n", description, saveFile);
    }

    @Override
    public String encrypt(byte[] plainData, String fileDataName, String saveFile) throws Exception {
        return encrypt(plainData, fileDataName, saveFile, false);
    }

    public String encrypt(byte[] plainData, String fileDataName, String saveFile, boolean withPublic) throws Exception {
        String name = getName();
        System.out.printf("Encrypting using '%s' with %s key...%n", name, withPublic ? "public" : "private");
        if (saveFile != null) {
            saveFile = saveFile + ".encrypted";
        }
        Cipher cipher = getCipher();
        cipher.init(Cipher.ENCRYPT_MODE, withPublic ? publicKey : privateKey);
        byte[] cypherText = cipher.doFinal(plainData);
        String encoded = Base64.getEncoder().encodeToString(cypherText);
        Map<ParamType, String[]> params = new TreeMap<>();
        params.put(ParamType.DESCRIPTION, new String[]{"Crypted file"});
        params.put(ParamType.METHOD, new String[]{name});
        params.put(ParamType.KEY_LENGTH, new String[]{getKeySizeHash()});
        if (fileDataName != null) {
            params.put(ParamType.FILE_NAME, new String[]{fileDataName});
        }
        params.put(ParamType.DATA, new String[]{encoded});
        System.out.println("\tEncryption successful!");
        if (saveFile != null) {
            Utils.writeResults(Paths.get(saveFile), params);
            System.out.printf("\tResults are stored into '%s' file.%n", saveFile);
        }
        return encoded;
    }

}
