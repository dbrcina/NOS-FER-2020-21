package hr.fer.zemris.nos.lab2.crypto.api;

import java.io.PrintWriter;

public class ResultsHandler {

    private static final Object[] params = new Object[ParamType.size];

    public static void putParam(ParamType param, Object data) {
        params[param.ordinal()] = data;
    }

    public static Object getParam(ParamType param) {
        return params[param.ordinal()];
    }

    public static void writeResults(String file) throws Exception {
        try (PrintWriter pwr = new PrintWriter(file)) {
            pwr.println("---BEGIN OS2 CRYPTO DATA---");
            for (int i = 0; i < params.length; i++) {
                Object param = params[i];
                if (param == null) continue;
                pwr.println(ParamType.forOrd(i));
                pwr.println(" ".repeat(4));
            }
            pwr.println("---END OS2 CRYPTO DATA---");
        }
    }

}
