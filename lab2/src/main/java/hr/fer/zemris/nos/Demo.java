package hr.fer.zemris.nos;

import hr.fer.zemris.nos.crypto.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;

public class Demo {

    private static final String DEFAULT_FILE = "./default.txt";
    private static final String[] SYMMETRIC_ALGORITHMS = {"AES", "DESede"};
    private static final String DEFAULT_SYMMETRIC_ALGORITHM = "AES";
    private static final Map<String, String[]> SECRET_KEY_LENGTHS = Map.of(
            "AES", new String[]{"128", "192", "256"},
            "DESEDE", new String[]{"112", "168"}
    );
    private static final Map<String, String> DEFAULT_SECRET_KEY_LENGTHS = Map.of(
            "AES", "128",
            "DESEDE", "168"
    );
    private static final String[] SYMMETRIC_MODES = {"ECB", "CBC", "OFB", "CFB", "CTR"};
    private static final String DEFAULT_SYMMETRIC_MODE = "CBC";
    private static final String[] RSA_KEY_LENGTHS = {"1024", "2048", "3072", "4096"};
    private static final String DEFAULT_RSA_KEY_LENGTH = "2048";
    private static final String[] HASH_ALGORITHM_KEY_LENGTHS = {"224", "256", "384", "512"};
    private static final String DEFAULT_HASH_ALGORITHM_KEY_LENGTH = "512";

    public static void main(String[] args) throws Exception {
        try (Scanner sc = new Scanner(System.in)) {
            String sourceFile = parseFileName(sc);
            System.out.println("-------------------------------------");
            SymmetricAlg senderSymmetricAlg = parseSymmetricAlgorithm(sc);
            System.out.println("-------------------------------------");
            RSA senderRSA = parseRSA(sc, "sender");
            System.out.println("-------------------------------------");
            RSA receiverRSA = parseRSA(sc, "receiver");
            System.out.println("-------------------------------------");
            HashAlg hash = parseHashAlgorithm(sc);
            System.out.println("-------------------------------------");
            System.out.println("\t[SENDER]:");
            senderSymmetricAlg.generateKey("sender");
            senderRSA.generateKey("sender");
            System.out.println("-------------------------------------");
            System.out.println("\t[RECEIVER]:");
            receiverRSA.generateKey("receiver");
            System.out.println("-------------------------------------");
            System.out.println("\t[SENDER]:");
            byte[] data = Files.readAllBytes(Paths.get(sourceFile));
            EnvelopeAlg envelopeAlg = new EnvelopeAlg(senderSymmetricAlg, receiverRSA);
            String[] envelope = envelopeAlg.envelope(data, sourceFile, "sender");
            System.out.println("-------------------------------------");
            System.out.println("\t[SENDER]:");
            SignatureAlg signatureAlg = new SignatureAlg(hash, senderRSA);
            byte[] signature = signatureAlg.signature(envelope[0].getBytes(), sourceFile, "sender");
            System.out.println("-------------------------------------");
            System.out.println("\t[RECEIVER]:");
            boolean isValid = signatureAlg.verify(envelope[0].getBytes(), signature);
            System.out.printf("Signature is %s!%n", isValid ? "valid" : "invalid");
            System.out.println("-------------------------------------");
            System.out.println("\t[RECEIVER]:");
            System.out.println("Decrypting sender's secret key...");
            byte[] secretKey = receiverRSA.decrypt(envelope[1].getBytes(), null);
            System.out.printf(
                    "Are real and decrypted secret keys equal: %s%n",
                    Arrays.equals(secretKey, senderSymmetricAlg.getSecretKey())
            );
            SymmetricAlg receiverSymAlg = new SymmetricAlg("DESEDE", 112, "ecb");
            receiverSymAlg.setKey(secretKey);
            System.out.println("-------------------------------------");
            System.out.println("\t[RECEIVER]:");
            System.out.println("Decrypting sender's data...");
            byte[] decryptedData = receiverSymAlg.decrypt(envelope[0].getBytes(), "sender");
            System.out.printf(
                    "Are real and decrypted data equal: %s%n",
                    Arrays.equals(data, decryptedData)
            );
        }
    }

    private static String parseFileName(Scanner sc) {
        System.out.printf(
                "Create digital seal for file with provided name or press enter for '%s': ",
                DEFAULT_FILE
        );
        String line = sc.nextLine();
        String file = line.isEmpty() ? DEFAULT_FILE : line;
        System.out.printf("File to be digitally sealed: '%s'%n", file);
        return file;
    }

    private static SymmetricAlg parseSymmetricAlgorithm(Scanner sc) throws Exception {
        // Parse symmetric algorithm's name.
        String algNames = String.join(",", SYMMETRIC_ALGORITHMS);
        System.out.printf(
                "Choose sender's symmetric algorithm [%s] or press enter for '%s': ",
                algNames, DEFAULT_SYMMETRIC_ALGORITHM
        );
        String line = sc.nextLine();
        String symAlgName = line.isEmpty() ? DEFAULT_SYMMETRIC_ALGORITHM : line.toUpperCase();
        if (Arrays.stream(SYMMETRIC_ALGORITHMS).noneMatch(alg -> alg.equalsIgnoreCase(symAlgName))) {
            System.out.printf("'%s' is invalid symmetric algorithm! Exiting...%n", symAlgName);
            System.exit(-1);
        }

        // Parse symmetric algorithm's key length.
        String secretKeyLengths = String.join(",", SECRET_KEY_LENGTHS.get(symAlgName));
        System.out.printf(
                "Choose key length [%s] or press enter for '%s': ",
                secretKeyLengths, DEFAULT_SECRET_KEY_LENGTHS.get(symAlgName));
        line = sc.nextLine();
        String secretKeyLengthStr = line.isEmpty() ? DEFAULT_SECRET_KEY_LENGTHS.get(symAlgName) : line;
        if (Arrays.stream(SECRET_KEY_LENGTHS.get(symAlgName)).noneMatch(len -> len.equals(secretKeyLengthStr))) {
            System.out.printf("'%s' is invalid key length! Exiting...%n", secretKeyLengthStr);
            System.exit(-1);
        }
        int secretKeyLength = Integer.parseInt(secretKeyLengthStr);

        // Parse symmetric algorithm' encryption/decryption mode.
        String modes = String.join(",", SYMMETRIC_MODES);
        System.out.printf(
                "Choose mode [%s] or press enter for '%s': ",
                modes, DEFAULT_SYMMETRIC_MODE);
        line = sc.nextLine();
        String mode = line.isEmpty() ? DEFAULT_SYMMETRIC_MODE : line.toUpperCase();
        if (Arrays.stream(SYMMETRIC_MODES).noneMatch(m -> m.equals(mode))) {
            System.out.printf("'%s' is invalid mode! Exiting...%n", mode);
            System.exit(-1);
        }

        System.out.printf("Sender's symmetric algorithm: %s/%d/%s%n", symAlgName, secretKeyLength, mode);
        return new SymmetricAlg(symAlgName, secretKeyLength, mode);
    }

    private static RSA parseRSA(Scanner sc, String from) throws Exception {
        String keyLengths = String.join(",", RSA_KEY_LENGTHS);
        System.out.printf(
                "Choose %s's RSA key length [%s] or press enter for '%s': ",
                from, keyLengths, DEFAULT_RSA_KEY_LENGTH
        );
        String line = sc.nextLine();
        String keyLength = line.isEmpty() ? DEFAULT_RSA_KEY_LENGTH : line;
        if (Arrays.stream(RSA_KEY_LENGTHS).noneMatch(key -> key.equals(keyLength))) {
            System.out.printf("'%s' is invalid key length! Exiting...%n", keyLength);
            System.exit(-1);
        }
        return new RSA(Integer.parseInt(keyLength));
    }

    private static HashAlg parseHashAlgorithm(Scanner sc) throws Exception {
        String keyLengths = String.join(",", HASH_ALGORITHM_KEY_LENGTHS);
        System.out.printf(
                "SHA3 hashing function is used for digital signature. Choose key length [%s] or press enter for '%s': ",
                keyLengths, DEFAULT_HASH_ALGORITHM_KEY_LENGTH
        );
        String line = sc.nextLine();
        String keyLength = line.isEmpty() ? DEFAULT_HASH_ALGORITHM_KEY_LENGTH : line;
        if (Arrays.stream(HASH_ALGORITHM_KEY_LENGTHS).noneMatch(key -> key.equals(keyLength))) {
            System.out.printf("'%s' is invalid key length! Exiting...%n", keyLength);
            System.exit(-1);
        }
        return new HashAlg("SHA3", Integer.parseInt(keyLength));
    }

}
