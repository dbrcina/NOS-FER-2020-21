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

    private static final ParamType[] values = values();

    private final String representation;

    ParamType(String representation) {
        this.representation = representation;
    }

    @Override
    public String toString() {
        return representation;
    }

    public static ParamType forRepresentation(String representation) {
        for (ParamType paramType : values) {
            if (paramType.representation.equals(representation)) {
                return paramType;
            }
        }
        throw new RuntimeException(String.format("'%s' is invalid param type!%n", representation));
    }

}
