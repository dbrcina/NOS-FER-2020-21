package hr.fer.zemris.nos.crypto;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Utils {

    private static final byte[] HEX_ARRAY = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
    private static final String BEGIN_FILE = "---BEGIN OS2 CRYPTO DATA---";
    private static final String END_FILE = "---END OS2 CRYPTO DATA---";

    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.US_ASCII);
    }

    public static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0, j = 0; i < bytes.length; i++, j += 2) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(j, j + 2), 16);
        }
        return bytes;
    }

    public static byte[] removeLeadingZero(byte[] bytes) {
        if (bytes[0] == 0) {
            return Arrays.copyOfRange(bytes, 1, bytes.length);
        } else {
            return bytes;
        }
    }

    public static String intToHex(int i) {
        String hex = Integer.toHexString(i);
        if (hex.length() % 2 != 0) {
            hex = "0".concat(hex);
        }
        return hex;
    }

    public static void writeResults(Path file, Map<ParamType, String[]> params) throws IOException {
        StringBuilder sb = new StringBuilder();
        String sep = System.lineSeparator();
        String fourSpaces = " ".repeat(4);
        sb.append(BEGIN_FILE).append(sep);
        for (Map.Entry<ParamType, String[]> param : params.entrySet()) {
            sb.append(param.getKey()).append(":").append(sep);
            for (String data : param.getValue()) {
                // Wrap text at 60 characters.
                int k = data.length() / 60 + 1;
                for (int i = 0; i < k; i++) {
                    int start = i * 60;
                    int end = i == (k - 1) ? data.length() : start + 60;
                    sb.append(fourSpaces).append(data, start, end).append(sep);
                }
            }
            sb.append(sep);
        }
        sb.append(END_FILE);
        Files.writeString(file, sb);
    }

    public static Map<ParamType, String[]> parseCryptoFile(String cryptoFile) throws Exception {
        Map<ParamType, String[]> params = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(cryptoFile))) {
            boolean inside = false;
            while (true) {
                String line = br.readLine();
                if (line == null) break;
                line = line.strip();
                if (line.equals(END_FILE)) break;
                if (line.isEmpty()) continue;
                if (line.equals(BEGIN_FILE)) {
                    inside = true;
                    continue;
                }
                if (inside) {
                    ParamType paramType = ParamType.forRepresentation(line.split(":")[0]);
                    Collection<String> paramValues = new ArrayList<>();
                    while (!(line = br.readLine().strip()).isEmpty()) {
                        paramValues.add(line);
                    }
                    params.put(paramType, paramValues.toArray(String[]::new));
                }
            }
        } catch (Exception e) {
            throw new Exception(String.format("'%s' is invalid crypto file!%n%s", cryptoFile, e.getMessage()));
        }
        return params;
    }

}
