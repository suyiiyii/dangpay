package top.suyiiyii.su.exception;

public class BaseHttpException extends RuntimeException {
    BaseHttpException(String message) {
        super(message);
    }
}
