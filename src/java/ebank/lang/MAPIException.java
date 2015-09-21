package ebank.lang;

public class MAPIException extends Exception {
    private String errorCode = null;
    private String errorMsg = "";

    public MAPIException(String errorCode) {
        super( String.format("[%s]", errorCode) );
        this.errorCode = errorCode;
    }

    public MAPIException(String errorCode, String message) {
        super( String.format("[%s]%s", errorCode, message) );
        this.errorCode = errorCode;
    }

    public MAPIException(String errorCode, Throwable cause) {
        super( String.format("%s: [%s]", cause.getMessage(), errorCode), cause );
        this.errorCode = errorCode;
    }

    public MAPIException(String errorCode, String errorMsg, Throwable cause) {
        super( String.format("%s: [%s]%s", cause.getMessage(), errorCode, errorMsg), cause );
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    public String getErrorCode() {
        return errorCode;
    }
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
    public String getErrorMsg() {
        return errorMsg;
    }
}
