package sha;

public class MyException extends Exception {
    public MyException(String theName) {
        this.theName = theName;
    }

    String theName;

}
