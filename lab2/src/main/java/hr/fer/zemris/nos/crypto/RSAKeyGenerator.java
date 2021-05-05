package hr.fer.zemris.nos.crypto;

import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class RSAKeyGenerator {

    private static final String[] RSA_KEY_SIZES = {"1024", "2048", "3072", "4096"};
    private static final String DEFAULT_RSA_KEY_SIZE = "2048";
    private static final String DEFAULT_SAVE_FILE = "alice";

    public static void main(String[] args) throws Exception {
        try (Scanner sc = new Scanner(System.in)) {
            // Parse key size.
            System.out.printf(
                    "Choose RSA key size [%s] or press enter for '%s': ",
                    String.join(",", RSA_KEY_SIZES), DEFAULT_RSA_KEY_SIZE
            );
            String line = sc.nextLine();
            String keySize = line.isEmpty() ? DEFAULT_RSA_KEY_SIZE : line;
            if (Arrays.stream(RSA_KEY_SIZES).noneMatch(size -> size.equals(keySize))) {
                System.out.printf("'%s' is invalid key size! Exiting...%n", keySize);
                System.exit(-1);
            }

            // Parse save file.
            System.out.printf("Enter save file or press enter for '%s': ", DEFAULT_SAVE_FILE);
            line = sc.nextLine();
            String saveFile = line.isEmpty() ? DEFAULT_SAVE_FILE : line;

            generateRSAKeys(Integer.parseInt(keySize), saveFile);
        }
    }

    private static void generateRSAKeys(int keySize, String saveFile) throws Exception {
        System.out.println("Generating RSA keys...");
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(keySize);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        String keySizeHex = Utils.intToHex(keySize);
        String mod = Utils.bytesToHex(publicKey.getModulus().toByteArray());
        String publicExponent = Utils.bytesToHex(publicKey.getPublicExponent().toByteArray());
        String privateExponent = Utils.bytesToHex(privateKey.getPrivateExponent().toByteArray());
        saveRSAKeys(true, keySizeHex, mod, publicExponent, saveFile + ".pub");
        saveRSAKeys(false, keySizeHex, mod, privateExponent, saveFile + ".priv");
    }

    private static void saveRSAKeys(
            boolean isPublic, String keySizeHex, String mod, String exp, String saveFile) throws Exception {
        String description = (isPublic ? "Public" : "Private") + " key";
        Map<ParamType, String[]> params = new TreeMap<>();
        params.put(ParamType.DESCRIPTION, new String[]{description});
        params.put(ParamType.METHOD, new String[]{"RSA"});
        params.put(ParamType.KEY_LENGTH, new String[]{keySizeHex});
        params.put(ParamType.MODULUS, new String[]{mod});
        params.put(isPublic ? ParamType.PUBLIC_EXPONENT : ParamType.PRIVATE_EXPONENT, new String[]{exp});
        Utils.writeResults(Paths.get(saveFile), params);
        System.out.printf("\t%s has been generated and stored into '%s' file.%n", description, saveFile);
    }

}
