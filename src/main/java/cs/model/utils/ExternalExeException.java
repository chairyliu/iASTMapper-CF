package cs.model.utils;

public class ExternalExeException extends RuntimeException {
    public ExternalExeException(String info){
        super("execute external command error:" + info);
    }
}
