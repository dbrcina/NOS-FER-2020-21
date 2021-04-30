package hr.fer.zemris.nos.lab2;

import hr.fer.zemris.nos.lab2.crypto.api.ParamType;
import hr.fer.zemris.nos.lab2.crypto.api.ResultsHandler;

public class Demo {

    public static void main(String[] args) throws Exception {
        ResultsHandler.putParam(ParamType.DESCRIPTION, "Test");
        ResultsHandler.writeResults("results.txt");
    }

}
