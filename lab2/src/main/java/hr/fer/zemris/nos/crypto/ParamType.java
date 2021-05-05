package hr.fer.zemris.nos.crypto;

public enum ParamType {

    DESCRIPTION("Description"),
    FILE_NAME("File name"),
    METHOD("Method"),
    KEY_LENGTH("Key length"),
    MODE("Mode"),
    SECRET_KEY("Secret key"),
    INITIALIZATION_VECTOR("Initialization vector"),
    MODULUS("Modulus"),
    PUBLIC_EXPONENT("Public exponent"),
    PRIVATE_EXPONENT("Private exponent"),
    SIGNATURE("Signature"),
    DATA("Data"),
    ENVELOPE_DATA("Envelope data"),
    ENVELOPE_CRYPT_KEY("Envelope crypt key");

    private final String representation;

    ParamType(String representation) {
        this.representation = representation;
    }

    @Override
    public String toString() {
        return representation;
    }

}
