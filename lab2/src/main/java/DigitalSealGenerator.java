import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.SecureRandom;
import java.util.*;

public class DigitalSealGenerator {

    private static final String DEFAULT_FILE = "./default.txt";
    private static final String SENDER_SECRET_FILE = "sender.secret";
    private static final String SENDER_PRIV_FILE = "sender.priv";
    private static final String RECEIVER_PUB_FILE = "receiver.pub";
    private static final String[] SYMMETRIC_MODES = {"ECB", "CBC", "OFB", "CFB", "CTR"};
    private static final String DEFAULT_SYMMETRIC_MODE = "CBC";
    private static final String[] HASH_ALGORITHM_KEY_LENGTHS = {"224", "256", "384", "512"};
    private static final String DEFAULT_HASH_ALGORITHM_KEY_LENGTH = "512";

    public static void main(String[] args) throws Exception {
        // Prepare keys...
        KeyData senderSk = Utils.parseSecretKey(SENDER_SECRET_FILE);
        KeyData receiverPub = Utils.parseRSAPublicKey(RECEIVER_PUB_FILE);
        KeyData senderPriv = Utils.parseRSAPrivateKey(SENDER_PRIV_FILE);

        try (Scanner sc = new Scanner(System.in)) {
            // Read file and mode from console.
            String file = consoleFileName(sc);
            String senderMode = consoleMode(sc);

            // Create envelope.
            System.out.println("--------------------------------");
            byte[][] envelope = createEnvelope(file, senderSk, senderMode, receiverPub);
            System.out.println("--------------------------------");

            // Make hash algorithm.
            HashAlg hash = consoleHash(sc);

            // Create signature.
            System.out.println("--------------------------------");
            createSignature(envelope, file, senderPriv, hash);
        }
    }

    private static byte[][] createEnvelope(
            String file, KeyData senderSk, String senderMode, KeyData receiverPub) throws Exception {
        System.out.println("Creating envelope...");
        byte[][] results = encryptUsingSecretKey(Files.readAllBytes(Paths.get(file)), senderSk.key, senderMode);
        byte[] envelopeData = results[0];
        byte[] iv = results[1];
        byte[] envelopeEncryptKey = encryptUsingPubPrivKey(senderSk.key.getEncoded(), receiverPub.key);
        Map<ParamType, String[]> params = new TreeMap<>();
        params.put(ParamType.DESCRIPTION, new String[]{"Envelope"});
        params.put(ParamType.FILE_NAME, new String[]{file});
        params.put(ParamType.METHOD, new String[]{senderSk.algorithm, receiverPub.algorithm});
        params.put(ParamType.KEY_LENGTH, new String[]{senderSk.keyLength, receiverPub.keyLength});
        params.put(ParamType.MODE, new String[]{senderMode});
        if (iv != null) {
            params.put(ParamType.INITIALIZATION_VECTOR, new String[]{Utils.bytesToHex(iv)});
        }
        params.put(ParamType.ENVELOPE_DATA, new String[]{Utils.bytesToHex(envelopeData)});
        params.put(ParamType.ENVELOPE_CRYPT_KEY, new String[]{Utils.bytesToHex(envelopeEncryptKey)});
        String saveFile = "sender.envelope";
        Utils.writeResults(Paths.get(saveFile), params);
        System.out.println("Envelope is successfully created!");
        System.out.printf("Results are stored into '%s'.%n", saveFile);
        return new byte[][]{envelopeData, envelopeEncryptKey};
    }

    private static void createSignature(byte[][] data, String file, KeyData senderPriv, HashAlg hash) throws Exception {
        System.out.println("Creating signature...");
        byte[] signature = encryptUsingPubPrivKey(hash.digest(Utils.flatten(data)), senderPriv.key);
        Map<ParamType, String[]> params = new TreeMap<>();
        params.put(ParamType.DESCRIPTION, new String[]{"Signature"});
        params.put(ParamType.FILE_NAME, new String[]{file});
        params.put(ParamType.METHOD, new String[]{hash.getName(), senderPriv.algorithm});
        params.put(ParamType.KEY_LENGTH, new String[]{hash.getKeySizeHex(), senderPriv.keyLength});
        params.put(ParamType.SIGNATURE, new String[]{Utils.bytesToHex(signature)});
        String saveFile = "sender.signature";
        Utils.writeResults(Paths.get(saveFile), params);
        System.out.println("Signature created successfully!");
        System.out.printf("Results are stored into '%s' file.%n", saveFile);
    }

    private static byte[][] encryptUsingSecretKey(byte[] data, Key key, String mode) throws Exception {
        String padding = mode.equals("CTR") ? "NoPadding" : "PKCS5Padding";
        Cipher cipher = Cipher.getInstance("%s/%s/%s".formatted(key.getAlgorithm(), mode, padding));
        byte[] iv = null;
        if (mode.equalsIgnoreCase("ecb")) {
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } else {
            iv = new byte[cipher.getBlockSize()];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        }
        return new byte[][]{Base64.getEncoder().encode(cipher.doFinal(data)), iv};
    }

    private static byte[] encryptUsingPubPrivKey(byte[] data, Key key) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return Base64.getEncoder().encode(cipher.doFinal(data));
    }

    private static String consoleFileName(Scanner sc) {
        System.out.printf(
                "Create digital seal for file with provided name or press enter for '%s': ",
                DEFAULT_FILE
        );
        String line = sc.nextLine();
        String file = line.isEmpty() ? DEFAULT_FILE : line;
        System.out.printf("File to be digitally sealed: '%s'%n", file);
        return file;
    }

    private static String consoleMode(Scanner sc) {
        System.out.printf(
                "Choose mode [%s] or press enter for '%s': ",
                String.join(",", SYMMETRIC_MODES), DEFAULT_SYMMETRIC_MODE);
        String line = sc.nextLine();
        String mode = line.isEmpty() ? DEFAULT_SYMMETRIC_MODE : line.toUpperCase();
        if (Arrays.stream(SYMMETRIC_MODES).noneMatch(m -> m.equals(mode))) {
            System.out.printf("'%s' is invalid mode! Exiting...%n", mode);
            System.exit(-1);
        }
        return mode;
    }

    private static HashAlg consoleHash(Scanner sc) throws Exception {
        System.out.printf(
                "SHA3 hash is used for digital signature. Choose key length [%s] or press enter for '%s': ",
                String.join(",", HASH_ALGORITHM_KEY_LENGTHS), DEFAULT_HASH_ALGORITHM_KEY_LENGTH
        );
        String line = sc.nextLine();
        String hashKeyLength = line.isEmpty() ? DEFAULT_HASH_ALGORITHM_KEY_LENGTH : line;
        if (Arrays.stream(HASH_ALGORITHM_KEY_LENGTHS).noneMatch(hashKey -> hashKey.equals(hashKeyLength))) {
            System.out.printf("'%s' is invalid hash key length! Exiting...%n", hashKeyLength);
            System.exit(-1);
        }
        return new HashAlg("SHA3", Integer.parseInt(hashKeyLength));
    }

}
