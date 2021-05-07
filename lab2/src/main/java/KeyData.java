import java.security.Key;

public class KeyData {

    public final String algorithm;
    public final String keyLength;
    public final Key key;

    public KeyData(String keyLength, Key key) {
        this.algorithm = key.getAlgorithm();
        this.keyLength = keyLength;
        this.key = key;
    }

}