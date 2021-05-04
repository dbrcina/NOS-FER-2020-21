package hr.fer.zemris.nos.lab2.crypto;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class SignatureAlg {

    public static byte[] signature(
            byte[] data, RSA senderRsa, HashAlg hash, String fileDataName, String saveFile) throws Exception {
        System.out.println("Creating signature...");
        byte[] signatureData = data;
        if (hash != null) {
            signatureData = hash.digest(signatureData);
        }
        signatureData = senderRsa.encrypt(signatureData, null, null).getBytes();
        System.out.println("Signature created successfully!");

        if (saveFile != null) {
            saveFile = saveFile + ".signature";
            Map<ParamType, String[]> params = new TreeMap<>();
            params.put(ParamType.DESCRIPTION, new String[]{"Signature"});
            params.put(ParamType.METHOD, hash == null
                    ? new String[]{"RSA"}
                    : new String[]{hash.getName(), "RSA"}
            );
            params.put(ParamType.KEY_LENGTH, hash == null
                    ? new String[]{senderRsa.getKeySizeHex()}
                    : new String[]{hash.getKeySizeHex(), senderRsa.getKeySizeHex()}
            );
            if (fileDataName != null) {
                params.put(ParamType.FILE_NAME, new String[]{fileDataName});
            }
            params.put(ParamType.SIGNATURE, new String[]{Utils.bytesToHex(signatureData)});
            Utils.writeResults(Paths.get(saveFile), params);
            System.out.printf("Results are stored into '%s' file.%n", saveFile);
        }

        return signatureData;
    }

    public static boolean verify(byte[] data, byte[] signature, RSA senderRSA, HashAlg hash) throws Exception {
        System.out.println("Verifying signature...");
        byte[] hashedData = hash.digest(data);
        byte[] decryptedData = senderRSA.decrypt(signature, null, true);
        return Arrays.equals(hashedData, decryptedData);
    }

}
