package hr.fer.zemris.nos.crypto;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class SymmetricKeyGenerator {

    private static final String[] SYMMETRIC_ALGORITHMS = {"AES", "DESede"};
    private static final String DEFAULT_SYMMETRIC_ALGORITHM = "AES";
    private static final Map<String, String[]> SECRET_KEY_SIZES = Map.of(
            "AES", new String[]{"128", "192", "256"},
            "DESEDE", new String[]{"112", "168"}
    );
    private static final Map<String, String> DEFAULT_SECRET_KEY_SIZES = Map.of(
            "AES", "128",
            "DESEDE", "168"
    );
    private static final String DEFAULT_SAVE_FILE = "alice";

    public static void main(String[] args) throws Exception {
        try (Scanner sc = new Scanner(System.in)) {
            // Parse symmetric algorithm's name.
            String algNames = String.join(",", SYMMETRIC_ALGORITHMS);
            System.out.printf(
                    "Choose symmetric algorithm [%s] or press enter for '%s': ",
                    algNames, DEFAULT_SYMMETRIC_ALGORITHM
            );
            String line = sc.nextLine();
            String name = line.isEmpty() ? DEFAULT_SYMMETRIC_ALGORITHM : line.toUpperCase();
            if (Arrays.stream(SYMMETRIC_ALGORITHMS).noneMatch(alg -> alg.equalsIgnoreCase(name))) {
                System.out.printf("'%s' is invalid symmetric algorithm! Exiting...%n", name);
                System.exit(-1);
            }

            // Parse symmetric algorithm's key size.
            String secretKeySizes = String.join(",", SECRET_KEY_SIZES.get(name));
            System.out.printf(
                    "Choose key size [%s] or press enter for '%s': ",
                    secretKeySizes, DEFAULT_SECRET_KEY_SIZES.get(name));
            line = sc.nextLine();
            String keySize = line.isEmpty() ? DEFAULT_SECRET_KEY_SIZES.get(name) : line;
            if (Arrays.stream(SECRET_KEY_SIZES.get(name)).noneMatch(size -> size.equals(keySize))) {
                System.out.printf("'%s' is invalid key size! Exiting...%n", keySize);
                System.exit(-1);
            }

            // Parse save file.
            System.out.printf("Enter save file or press enter for '%s': ", DEFAULT_SAVE_FILE);
            line = sc.nextLine();
            String saveFile = line.isEmpty() ? DEFAULT_SAVE_FILE : line;

            generateKey(name, Integer.parseInt(keySize), saveFile);
        }
    }

    private static void generateKey(String name, int keySize, String saveFile) throws Exception {
        System.out.println("Generating secret key...");
        saveFile = saveFile + ".secret";
        KeyGenerator keyGenerator = KeyGenerator.getInstance(name);
        keyGenerator.init(keySize);
        SecretKey secretKey = keyGenerator.generateKey();
        Map<ParamType, String[]> params = new TreeMap<>();
        params.put(ParamType.DESCRIPTION, new String[]{"Secret key"});
        params.put(ParamType.METHOD, new String[]{name});
        params.put(ParamType.KEY_LENGTH, new String[]{Utils.intToHex(keySize)});
        params.put(ParamType.SECRET_KEY, new String[]{Utils.bytesToHex(secretKey.getEncoded())});
        Utils.writeResults(Paths.get(saveFile), params);
        System.out.printf("Secret key has been generated and stored into '%s' file.%n", saveFile);
    }

}
