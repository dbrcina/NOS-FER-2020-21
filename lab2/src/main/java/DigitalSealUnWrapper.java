import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

public class DigitalSealUnWrapper {

    private static final String SENDER_PUB_FILE = "sender.pub";
    private static final String SENDER_SIGNATURE_FILE = "sender.signature";
    private static final String SENDER_ENVELOPE_FILE = "sender.envelope";
    private static final String RECEIVER_PRIV_FILE = "receiver.priv";

    public static void main(String[] args) throws Exception {
        KeyData senderPub = Utils.parseRSAPublicKey(SENDER_PUB_FILE);
        KeyData receiverPriv = Utils.parseRSAPrivateKey(RECEIVER_PRIV_FILE);

        System.out.println("Unwrapping envelope...");
        Map<ParamType, String[]> params = Utils.parseCryptoFile(SENDER_ENVELOPE_FILE);
        byte[] envelopeData = Utils.hexToBytes(String.join("", params.get(ParamType.ENVELOPE_DATA)));
        byte[] envelopeCryptKey = Utils.hexToBytes(String.join("", params.get(ParamType.ENVELOPE_CRYPT_KEY)));
        String mode = params.get(ParamType.MODE)[0];
        String secretAlgorithm = params.get(ParamType.METHOD)[0];
        String iv = params.getOrDefault(ParamType.INITIALIZATION_VECTOR, new String[1])[0];
        String realFile = params.get(ParamType.FILE_NAME)[0];
        Path realFilePath = Paths.get(realFile);
        System.out.println("Envelope unwrapped successfully!");
        System.out.println("------------------------------");

        System.out.println("Verifying envelope's signature...");
        boolean verify = verifySignature(new byte[][]{envelopeData, envelopeCryptKey}, senderPub);
        System.out.printf("Verification done. Signature is %s!%n", verify ? "valid" : "invalid");
        System.out.println("------------------------------");

        System.out.println("Decrypting envelope data...");
        SecretKey senderSk = new SecretKeySpec(decryptUsingPrivPubKey(envelopeCryptKey, receiverPriv), secretAlgorithm);
        byte[] decryptedEnvelopeData = decryptUsingSecretKey(envelopeData, senderSk, mode, iv);
        String decryptedFile = "receiver.decrypted";
        Path decryptedFilePath = Paths.get(decryptedFile);
        Files.write(decryptedFilePath, decryptedEnvelopeData);
        System.out.println("Decryption successful!");
        System.out.printf("Results are stored into '%s'.%n", decryptedFile);
        System.out.println("------------------------------");

        System.out.printf(
                "Are real('%s') and decrypted('%s') files equal? %s!%n",
                realFile, decryptedFile, Arrays.equals(Files.readAllBytes(realFilePath), Files.readAllBytes(decryptedFilePath))
        );
    }

    private static boolean verifySignature(byte[][] data, KeyData senderPub) throws Exception {
        Map<ParamType, String[]> params = Utils.parseCryptoFile(SENDER_SIGNATURE_FILE);
        String hashName = params.get(ParamType.METHOD)[0];
        String hashKeyLengthHex = params.get(ParamType.KEY_LENGTH)[0];
        HashAlg hash = new HashAlg(hashName, Integer.parseInt(hashKeyLengthHex, 16));
        byte[] hashedData = hash.digest(Utils.flatten(data));
        String signatureHex = String.join("", params.get(ParamType.SIGNATURE));
        byte[] signature = Utils.hexToBytes(signatureHex);
        byte[] decryptedSignature = decryptUsingPrivPubKey(signature, senderPub);
        return Arrays.equals(hashedData, decryptedSignature);
    }

    private static byte[] decryptUsingPrivPubKey(byte[] data, KeyData key) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key.key);
        return cipher.doFinal(Base64.getDecoder().decode(data));
    }

    private static byte[] decryptUsingSecretKey(byte[] data, Key key, String mode, String iv) throws Exception {
        String padding = mode.equals("CTR") ? "NoPadding" : "PKCS5Padding";
        Cipher cipher = Cipher.getInstance("%s/%s/%s".formatted(key.getAlgorithm(), mode, padding));
        if (mode.equalsIgnoreCase("ecb")) {
            cipher.init(Cipher.DECRYPT_MODE, key);
        } else {
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(Utils.hexToBytes(iv)));
        }
        return cipher.doFinal(Base64.getDecoder().decode(data));
    }

}
