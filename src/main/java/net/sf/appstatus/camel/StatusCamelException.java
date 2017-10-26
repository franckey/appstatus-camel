package net.sf.appstatus.camel;

public class StatusCamelException extends Exception {
    private static final long serialVersionUID = 1L;

    public StatusCamelException() {
        super();
    }

    public StatusCamelException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public StatusCamelException(String message, Throwable cause) {
        super(message, cause);
    }

    public StatusCamelException(String message) {
        super(message);
    }

    public StatusCamelException(Throwable cause) {
        super(cause);
    }

}
