package hr.fer.zemris.nos.lab2.crypto;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class SignatureAlg {

    private final HashAlg hash;
    private final RSA rsa;

    public SignatureAlg(HashAlg hash, RSA rsa) {
        this.hash = hash;
        this.rsa = rsa;
    }

    public byte[] signature(byte[] data, String sourceFile, String saveFile) throws Exception {
        System.out.println("Creating signature...");
        byte[] signature = rsa.encrypt(hash.digest(data), null, null, true).getBytes();
        System.out.println("Signature created successfully!");

        if (saveFile != null) {
            saveFile = saveFile + ".signature";
            Map<ParamType, String[]> params = new TreeMap<>();
            params.put(ParamType.DESCRIPTION, new String[]{"Signature"});
            params.put(ParamType.METHOD, new String[]{hash.getName(), rsa.getName()});
            params.put(ParamType.KEY_LENGTH, new String[]{hash.getKeySizeHex(), rsa.getKeySizeHex()});
            if (sourceFile != null) {
                params.put(ParamType.FILE_NAME, new String[]{sourceFile});
            }
            params.put(ParamType.SIGNATURE, new String[]{Utils.bytesToHex(signature)});
            Utils.writeResults(Paths.get(saveFile), params);
            System.out.printf("Results are stored into '%s' file.%n", saveFile);
        }

        return signature;
    }

    public boolean verify(byte[] data, byte[] signature) throws Exception {
        System.out.println("Verifying signature...");
        byte[] hashedData = hash.digest(data);
        byte[] decryptedData = rsa.decrypt(signature, null, false);
        System.out.println("Signature verified!");
        return Arrays.equals(hashedData, decryptedData);
    }

}
