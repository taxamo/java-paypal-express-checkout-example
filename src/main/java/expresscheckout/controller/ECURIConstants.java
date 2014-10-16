package expresscheckout.controller;

public class ECURIConstants {

    public static final String LOCALHOST = "http://localhost:3009";
    public static final String TAXAMO = "http://localhost:3007";

    public static final String SUCCESS_LINK = "/success-checkout";
    public static final String CANCEL_LINK = "/cancel-checkout";

    public static final String PAYPAL_NVP = "https://api-3t.sandbox.paypal.com/nvp";

    public static final String POST_CONFIRM = "/api/v1/transactions/{transactionKey}/confirm?private_token={private_token}";
    public static final String GET_TRANSACTION = "/api/v1/transactions/{transactionKey}?private_token={private_token}";
}
