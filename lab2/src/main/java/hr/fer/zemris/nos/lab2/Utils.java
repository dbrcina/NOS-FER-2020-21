package hr.fer.zemris.nos.lab2;

import hr.fer.zemris.nos.lab2.crypto.ParamType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class Utils {

    private static final byte[] HEX_ARRAY = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);

    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    public static String intToHex(int i) {
        String hex = Integer.toHexString(i);
        if (hex.length() % 2 != 0) {
            hex = "0".concat(hex);
        }
        return hex;
    }

    public static void writeResults(String file, Map<ParamType, String[]> params) throws IOException {
        StringBuilder sb = new StringBuilder();
        String sep = System.lineSeparator();
        String fourSpaces = " ".repeat(4);
        sb.append("---BEGIN OS2 CRYPTO DATA---").append(sep);
        for (Map.Entry<ParamType, String[]> param : params.entrySet()) {
            sb.append(param.getKey()).append(":").append(sep);
            for (String data : param.getValue()) {
                // Wrap text at 60 characters.
                int k = data.length() / 60 + 1;
                for (int i = 0; i < k; i++) {
                    int start = i * k;
                    int end = start + i == (k - 1) ? data.length() : 60;
                    sb.append(fourSpaces).append(data, start, end).append(sep);
                }
            }
            sb.append(sep);
        }
        sb.append("---END OS2 CRYPTO DATA---");
        Files.writeString(Paths.get(file), sb);
    }

}
