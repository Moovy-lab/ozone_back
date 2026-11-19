package oryon.springboot.ozone.exception;

public class TooManyAttemptsException extends RuntimeException{
    private final String code;

    public TooManyAttemptsException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
