package hr.fer.zemris.nos.lab2.crypto.api;

public enum ParamType {

    DESCRIPTION("Description"),
    METHOD("Method"),
    FILE_NAME("File name"),
    KEY_LENGTH("Key length"),
    SECRET_KEY("Secret key"),
    INITIALIZATION_VECTOR("Initialization vector"),
    MODULUS("Modulus"),
    PUBLIC_EXPONENT("Public exponent"),
    PRIVATE_EXPONENT("Private exponent"),
    SIGNATURE("Signature"),
    DATA("Data"),
    ENVELOPE_DATA("Envelope data"),
    ENVELOPE_CRYPT_KEY("Envelope crypt key");

    public static final int size;
    private static final ParamType[] params;

    static {
        params = values();
        size = params.length;
    }

    private final String representation;

    ParamType(String representation) {
        this.representation = representation;
    }

    @Override
    public String toString() {
        return representation;
    }

    public static ParamType forName(String name) {
        for (ParamType param : params) {
            if (param.representation.equals(name)) return param;
        }
        return null;
    }

    public static ParamType forOrd(int ord) {
        return params[ord];
    }

}
