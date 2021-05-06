package hr.fer.zemris.nos.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
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
        SecretKeyData senderSk = parseSecretKey();
        RSAPublicKey receiverPub = parseRSAPublicKey();
        RSAPrivateKey senderPriv = parseRSAPrivateKey();

        try (Scanner sc = new Scanner(System.in)) {
            // Read file and mode from console.
            String file = consoleFileName(sc);
            String senderMode = consoleMode(sc);

            // Create envelope.
            System.out.println("--------------------------------");
            byte[] envelopeData = createEnvelope(file, senderSk, senderMode, receiverPub);
            System.out.println("--------------------------------");

            // Make hash algorithm.
            HashAlg hash = consoleHash(sc);

            // Create signature.
            System.out.println("--------------------------------");
            createSignature(envelopeData, file, senderPriv, hash);
        }
    }

    private static byte[] createEnvelope(
            String file, SecretKeyData senderSk, String senderMode, RSAPublicKey receiverPub) throws Exception {
        System.out.println("Creating envelope...");
        byte[][] encryptionResults = encryptUsingSecretKey(Files.readAllBytes(Paths.get(file)), senderSk, senderMode);
        byte[] envelopeData = encryptionResults[0];
        byte[] iv = encryptionResults[1];
        byte[] envelopeEncryptKey = encryptUsingPubPrivKey(senderSk.key, receiverPub);
        Map<ParamType, String[]> params = new TreeMap<>();
        params.put(ParamType.DESCRIPTION, new String[]{"Envelope"});
        params.put(ParamType.FILE_NAME, new String[]{file});
        params.put(ParamType.METHOD, new String[]{
                senderSk.algorithm,
                receiverPub.getAlgorithm()
        });
        params.put(ParamType.KEY_LENGTH, new String[]{
                senderSk.keyLength,
                Utils.intToHex(Utils.removeLeadingZero(receiverPub.getModulus().toByteArray()).length * 8)
        });
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
        return envelopeData;
    }

    private static void createSignature(
            byte[] data, String file, RSAPrivateKey senderPriv, HashAlg hash) throws Exception {
        System.out.println("Creating signature...");
        byte[] signature = encryptUsingPubPrivKey(hash.digest(data), senderPriv);
        Map<ParamType, String[]> params = new TreeMap<>();
        params.put(ParamType.DESCRIPTION, new String[]{"Signature"});
        params.put(ParamType.FILE_NAME, new String[]{file});
        params.put(ParamType.METHOD, new String[]{hash.getName(), senderPriv.getAlgorithm()});
        params.put(ParamType.KEY_LENGTH, new String[]{
                hash.getKeySizeHex(),
                Utils.intToHex(Utils.removeLeadingZero(senderPriv.getModulus().toByteArray()).length * 8)
        });
        params.put(ParamType.SIGNATURE, new String[]{Utils.bytesToHex(signature)});
        String saveFile = "sender.signature";
        Utils.writeResults(Paths.get(saveFile), params);
        System.out.println("Signature created successfully!");
        System.out.printf("Results are stored into '%s' file.%n", saveFile);
    }

    private static byte[][] encryptUsingSecretKey(byte[] data, SecretKeyData senderSk, String mode) throws Exception {
        String padding = mode.equals("CTR") ? "NoPadding" : "PKCS5Padding";
        Cipher cipher = Cipher.getInstance("%s/%s/%s".formatted(senderSk.algorithm, mode, padding));
        SecretKey sk = new SecretKeySpec(senderSk.key, senderSk.algorithm);
        byte[] iv = null;
        if (mode.equalsIgnoreCase("ecb")) {
            cipher.init(Cipher.ENCRYPT_MODE, sk);
        } else {
            iv = new byte[cipher.getBlockSize()];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, sk, new IvParameterSpec(iv));
        }
        return new byte[][]{Base64.getEncoder().encode(cipher.doFinal(data)), iv};
    }

    private static byte[] encryptUsingPubPrivKey(byte[] data, Key key) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return Base64.getEncoder().encode(cipher.doFinal(data));
    }

    private static SecretKeyData parseSecretKey() throws Exception {
        Map<ParamType, String[]> params = Utils.parseCryptoFile(SENDER_SECRET_FILE);
        String algorithm = params.get(ParamType.METHOD)[0];
        String keyLength = params.get(ParamType.KEY_LENGTH)[0];
        String secretKeyHex = String.join("", params.get(ParamType.SECRET_KEY));
        return new SecretKeyData(algorithm, keyLength, Utils.hexToBytes(secretKeyHex));
    }

    private static RSAPublicKey parseRSAPublicKey() throws Exception {
        Map<ParamType, String[]> params = Utils.parseCryptoFile(RECEIVER_PUB_FILE);
        String method = params.get(ParamType.METHOD)[0];
        String modulus = String.join("", params.get(ParamType.MODULUS));
        String publicExponent = params.get(ParamType.PUBLIC_EXPONENT)[0];
        return (RSAPublicKey) KeyFactory.getInstance(method).generatePublic(new RSAPublicKeySpec(
                new BigInteger(modulus, 16),
                new BigInteger(publicExponent, 16)
        ));
    }

    private static RSAKeyData parseRSAPrivateKey() throws Exception {
        Map<ParamType, String[]> params = Utils.parseCryptoFile(SENDER_PRIV_FILE);
        String algorithm = params.get(ParamType.METHOD)[0];
        String keyLength = params.get(ParamType.KEY_LENGTH)[0];
        String modulus = String.join("", params.get(ParamType.MODULUS));
        String privateExponent = String.join("", params.get(ParamType.PRIVATE_EXPONENT));
        Key key = KeyFactory.getInstance(algorithm).generatePrivate(new RSAPrivateKeySpec(
                new BigInteger(modulus, 16),
                new BigInteger(privateExponent, 16)
        ));
        return new RSAKeyData(keyLength, key);
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

    private static class SecretKeyData {
        private final String algorithm;
        private final String keyLength;
        private final byte[] key;

        private SecretKeyData(String algorithm, String keyLength, byte[] key) {
            this.algorithm = algorithm;
            this.keyLength = keyLength;
            this.key = key;
        }
    }

    private static class RSAKeyData {
        private final String algorithm = "RSA";
        private final String keyLength;
        private final Key key;

        private RSAKeyData(String keyLength, Key key) {
            this.keyLength = keyLength;
            this.key = key;
        }
    }

}
