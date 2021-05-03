package hr.fer.zemris.nos.lab2.crypto;

import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

public class SignatureAlg {

    public static byte[] signature(
            String data, RSA rsa, HashAlg hash, String fileDataName, String saveFile) throws Exception {
        System.out.println("Creating signature...");
        if (saveFile != null) {
            saveFile = saveFile + ".signature";
        }
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] signatureData = decoder.decode(data);
        if (hash != null) {
            signatureData = hash.digest(signatureData);
        }
        signatureData = decoder.decode(rsa.encrypt(signatureData, null, null));
        Map<ParamType, String[]> params = new TreeMap<>();
        params.put(ParamType.DESCRIPTION, new String[]{"Signature"});
        params.put(ParamType.METHOD, hash == null
                ? new String[]{"RSA"}
                : new String[]{hash.getName(), "RSA"}
        );
        params.put(ParamType.KEY_LENGTH, hash == null
                ? new String[]{rsa.getKeySizeHash()}
                : new String[]{hash.getKeySizeHash(), rsa.getKeySizeHash()}
        );
        if (fileDataName != null) {
            params.put(ParamType.FILE_NAME, new String[]{fileDataName});
        }
        params.put(ParamType.SIGNATURE, new String[]{Utils.bytesToHex(signatureData)});
        System.out.println("\tSignature created successfully!");
        if (saveFile != null) {
            Utils.writeResults(Paths.get(saveFile), params);
            System.out.printf("\tResults are stored into '%s' file.%n", saveFile);
        }
        return signatureData;
    }

}
