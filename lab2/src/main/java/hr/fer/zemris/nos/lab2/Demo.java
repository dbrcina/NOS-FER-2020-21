package hr.fer.zemris.nos.lab2;

import hr.fer.zemris.nos.lab2.crypto.SymmetricAlg;

import java.util.Map;
import java.util.Scanner;

public class Demo {

    private static final String DEFAULT_FILE = "default.txt";
    private static final String DEFAULT_SYMMETRIC_ALGORITHM = "AES";
    private static final Map<String, Integer> DEFAULT_SECRET_KEY_LENGTHS = Map.of(
            "AES", 128,
            "3DES", 168
    );
    private static final String DEFAULT_SYMMETRIC_MODE = "CBC";

    public static void main(String[] args) throws Exception {
        try (Scanner sc = new Scanner(System.in)) {
            System.out.print("Create digital seal for file with provided name or press enter for default: ");
            String line = sc.nextLine();
            String file = line.isEmpty() ? DEFAULT_FILE : line;
            System.out.println("File to be digitally sealed: " + file);

            System.out.print("Choose symmetric algorithm [AES,3DES] or press enter for default: ");
            line = sc.nextLine();
            String symAlgName = line.isEmpty() ? DEFAULT_SYMMETRIC_ALGORITHM : line.toUpperCase();

            System.out.print("Choose key length [AES:128,192,256, 3DES:112,168] or press enter for default: ");
            line = sc.nextLine();
            int secretKeyLength = line.isEmpty() ? DEFAULT_SECRET_KEY_LENGTHS.get(symAlgName) : Integer.parseInt(line);

            System.out.print("Choose mode [ECB,CBC,OFB,CFB,CTR] or press enter for default: ");
            line = sc.nextLine();
            String mode = line.isEmpty() ? DEFAULT_SYMMETRIC_MODE : line.toUpperCase();
            System.out.printf("Symmetric algorithm: %s/%d/%s%n", symAlgName, secretKeyLength, mode);

            SymmetricAlg symmetricAlg = new SymmetricAlg(symAlgName, secretKeyLength, mode);
            symmetricAlg.generateKey("sender-secret.txt");
            symmetricAlg.generateIV();
            symmetricAlg.encrypt(file, "sender-data-encrypted.txt");
        }
    }

}
