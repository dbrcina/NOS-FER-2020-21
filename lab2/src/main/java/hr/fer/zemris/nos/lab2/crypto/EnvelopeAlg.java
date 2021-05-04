package hr.fer.zemris.nos.lab2.crypto;

import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

public class EnvelopeAlg {

    public static String[] envelope(
            byte[] data,
            SymmetricAlg symmetricAlg,
            RSA rsa,
            String fileDataName,
            String saveFile) throws Exception {
        System.out.println("Creating envelope...");
        String envelopeData = symmetricAlg.encrypt(data, null, null);
        String envelopeCryptKey = rsa.encrypt(symmetricAlg.getSecretKeyBytes(), null, null, true);
        System.out.println("Envelope created successfully!");

        if (saveFile != null) {
            saveFile = saveFile + ".envelope";
            Map<ParamType, String[]> params = new TreeMap<>();
            params.put(ParamType.DESCRIPTION, new String[]{"Envelope"});
            params.put(ParamType.METHOD, new String[]{symmetricAlg.getName(), rsa.getName()});
            params.put(ParamType.KEY_LENGTH, new String[]{symmetricAlg.getKeySizeHex(), rsa.getKeySizeHex()});
            if (fileDataName != null) {
                params.put(ParamType.FILE_NAME, new String[]{fileDataName});
            }
            params.put(ParamType.ENVELOPE_DATA, new String[]{envelopeData});
            params.put(ParamType.ENVELOPE_CRYPT_KEY, new String[]{envelopeCryptKey});
            Utils.writeResults(Paths.get(saveFile), params);
            System.out.printf("Results are stored into '%s' file.%n", saveFile);
        }

        return new String[]{envelopeData, envelopeCryptKey};
    }

}
