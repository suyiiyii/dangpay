package top.suyiiyii.su.exception;

public class Http_404_NotFoundException extends BaseHttpException {
    public Http_404_NotFoundException(String message) {
        super(message);
    }

    public Http_404_NotFoundException() {
        super("404 Not Found");
    }
}
