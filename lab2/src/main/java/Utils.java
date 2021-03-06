import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
        String hex = new String(hexChars, StandardCharsets.US_ASCII);
        if (hex.length() % 2 != 0) {
            hex = "0".concat(hex);
        }
        return hex;
    }

    public static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0, j = 0; i < bytes.length; i++, j += 2) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(j, j + 2), 16);
        }
        return bytes;
    }

    public static String intToHex(int i) {
        String hex = Integer.toHexString(i);
        if (hex.length() % 2 != 0) {
            hex = "0".concat(hex);
        }
        return hex;
    }

    public static byte[] flatten(byte[][] matrix) {
        byte[] flatten = new byte[matrix.length * matrix[0].length];
        int index = 0;
        for (byte[] bytes : matrix) {
            for (byte b : bytes) {
                flatten[index++] = b;
            }
        }
        return flatten;
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

    public static KeyData parseSecretKey(String file) throws Exception {
        Map<ParamType, String[]> params = Utils.parseCryptoFile(file);
        String algorithm = params.get(ParamType.METHOD)[0];
        String keyLength = params.get(ParamType.KEY_LENGTH)[0];
        String secretKeyHex = String.join("", params.get(ParamType.SECRET_KEY));
        return new KeyData(keyLength, new SecretKeySpec(Utils.hexToBytes(secretKeyHex), algorithm));
    }

    public static KeyData parseRSAPublicKey(String file) throws Exception {
        Map<ParamType, String[]> params = Utils.parseCryptoFile(file);
        String algorithm = params.get(ParamType.METHOD)[0];
        String keyLength = params.get(ParamType.KEY_LENGTH)[0];
        String modulus = String.join("", params.get(ParamType.MODULUS));
        String publicExponent = params.get(ParamType.PUBLIC_EXPONENT)[0];
        Key key = KeyFactory.getInstance(algorithm).generatePublic(new RSAPublicKeySpec(
                new BigInteger(modulus, 16),
                new BigInteger(publicExponent, 16)
        ));
        return new KeyData(keyLength, key);
    }

    public static KeyData parseRSAPrivateKey(String file) throws Exception {
        Map<ParamType, String[]> params = Utils.parseCryptoFile(file);
        String algorithm = params.get(ParamType.METHOD)[0];
        String keyLength = params.get(ParamType.KEY_LENGTH)[0];
        String modulus = String.join("", params.get(ParamType.MODULUS));
        String privateExponent = String.join("", params.get(ParamType.PRIVATE_EXPONENT));
        Key key = KeyFactory.getInstance(algorithm).generatePrivate(new RSAPrivateKeySpec(
                new BigInteger(modulus, 16),
                new BigInteger(privateExponent, 16)
        ));
        return new KeyData(keyLength, key);
    }

}
