package hr.fer.zemris.nos.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;

public class DigitalSealGenerator {

    private static final String DEFAULT_FILE = "./default.txt";
    private static final String DEFAULT_SENDER = "alice";
    private static final String DEFAULT_RECEIVER = "bob";
    private static final String[] SYMMETRIC_MODES = {"ECB", "CBC", "OFB", "CFB", "CTR"};
    private static final String DEFAULT_SYMMETRIC_MODE = "CBC";
    private static final String[] HASH_ALGORITHM_KEY_LENGTHS = {"224", "256", "384", "512"};
    private static final String DEFAULT_HASH_ALGORITHM_KEY_LENGTH = "512";

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        String file = consoleFileName(sc);
        String senderSecretFile = consoleSecretFile(sc);
        String senderMode = consoleMode(sc);
        String senderPrivateFile = consolePrivateFile(sc);
        String receiverPublicFile = consolePublicFile(sc);
        String hashKeyLength = consoleHashKeyLength(sc);

        SecretKey senderSk = parseSecretKey(senderSecretFile);
        PublicKey receiverPub = parseRSAPublicKey(receiverPublicFile);

        createEnvelope(file, senderSk, senderMode, receiverPub, DEFAULT_SENDER);
        sc.close();
    }

    private static byte[] createEnvelope(
            String file,
            SecretKey senderSk,
            String senderMode,
            PublicKey receiverPub,
            String saveFile) throws Exception {
        byte[][] encryptionResults = encryptUsingSecretKey(Files.readAllBytes(Paths.get(file)), senderSk, senderMode);
        byte[] envelopeData = encryptionResults[0];
        byte[] iv = encryptionResults[1];
        byte[] envelopeEncryptKey = encryptSecretKeyUsingPublicKey(senderSk, receiverPub);
        Map<ParamType, String[]> params = new TreeMap<>();
        params.put(ParamType.DESCRIPTION, new String[]{"Envelope"});
        params.put(ParamType.FILE_NAME, new String[]{file});
        params.put(ParamType.METHOD, new String[]{
                senderSk.getAlgorithm(),
                receiverPub.getAlgorithm()
        });
        params.put(ParamType.KEY_LENGTH, new String[]{
                Utils.intToHex(new BigInteger(senderSk.getEncoded()).bitCount()),
                Utils.intToHex(new BigInteger(receiverPub.getEncoded()).bitCount())
        });
        params.put(ParamType.MODE, new String[]{senderMode});
        if (iv != null) {
            params.put(ParamType.INITIALIZATION_VECTOR, new String[]{Utils.bytesToHex(iv)});
        }
        params.put(ParamType.ENVELOPE_DATA, new String[]{Utils.bytesToHex(envelopeData)});
        params.put(ParamType.ENVELOPE_CRYPT_KEY, new String[]{Utils.bytesToHex(envelopeEncryptKey)});
        Utils.writeResults(Paths.get(saveFile + ".envelope"), params);
        return envelopeData;
    }

    private static byte[][] encryptUsingSecretKey(byte[] data, SecretKey senderSk, String mode) throws Exception {
        String algorithm = senderSk.getAlgorithm();
        String padding = mode.equals("CTR") ? "NoPadding" : "PKCS5Padding";
        Cipher cipher = Cipher.getInstance("%s/%s/%s".formatted(algorithm, mode, padding));
        byte[] iv = null;
        if (mode.equalsIgnoreCase("ecb")) {
            cipher.init(Cipher.ENCRYPT_MODE, senderSk);
        } else {
            iv = new byte[cipher.getBlockSize()];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, senderSk, new IvParameterSpec(iv));
        }
        return new byte[][]{Base64.getEncoder().encode(cipher.doFinal(data)), iv};
    }

    private static byte[] encryptSecretKeyUsingPublicKey(SecretKey senderSk, PublicKey receiverPub) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, receiverPub);
        return Base64.getEncoder().encode(cipher.doFinal(senderSk.getEncoded()));
    }

    private static PublicKey parseRSAPublicKey(String publicFile) throws Exception {
        Map<ParamType, String[]> params = Utils.parseCryptoFile(publicFile);
        String method = params.get(ParamType.METHOD)[0];
        String modulus = String.join("", params.get(ParamType.MODULUS));
        String publicExponent = params.get(ParamType.PUBLIC_EXPONENT)[0];
        return KeyFactory.getInstance(method).generatePublic(new RSAPublicKeySpec(
                new BigInteger(modulus, 16),
                new BigInteger(publicExponent, 16)
        ));
    }

    private static SecretKey parseSecretKey(String secretFile) throws Exception {
        Map<ParamType, String[]> params = Utils.parseCryptoFile(secretFile);
        String method = params.get(ParamType.METHOD)[0];
        String secretKeyHex = String.join("", params.get(ParamType.SECRET_KEY));
        return new SecretKeySpec(new BigInteger(secretKeyHex, 16).toByteArray(), method);
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

    private static String consoleSecretFile(Scanner sc) {
        System.out.printf("Enter sender's secret key file or press enter for '%s.secret': ", DEFAULT_SENDER);
        String line = sc.nextLine();
        return line.isEmpty() ? DEFAULT_SENDER + ".secret" : line;
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

    private static String consolePrivateFile(Scanner sc) {
        System.out.printf("Enter sender's private key file or press enter for '%s.priv': ", DEFAULT_SENDER);
        String line = sc.nextLine();
        return line.isEmpty() ? DEFAULT_SENDER + ".priv" : line;
    }

    private static String consolePublicFile(Scanner sc) {
        System.out.printf("Enter receiver's public key file or press enter for '%s.pub': ", DEFAULT_RECEIVER);
        String line = sc.nextLine();
        return line.isEmpty() ? DEFAULT_RECEIVER + ".pub" : line;
    }

    private static String consoleHashKeyLength(Scanner sc) {
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
        return hashKeyLength;
    }

}
