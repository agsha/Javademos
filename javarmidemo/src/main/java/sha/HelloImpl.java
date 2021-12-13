package sha;

import java.rmi.RemoteException;

/**
 * Created by sharath.g on 17/04/16.
 */
public class HelloImpl implements  Hello {
    protected HelloImpl() throws RemoteException {
        super();
    }

    @Override
    public String echo(String what) {
        return "Hello "+what;

    }

    @Override
    public String exceptionExample() throws RemoteException, MyException {
        a1();
        return "ff";
    }

    public void a1() throws MyException {
        a2();
    }
    public void a2() throws MyException {
        a3();
    }
    public void a3() throws MyException {
        a4();
    }
    public void a4() throws MyException {
        throw new MyException("Server side Exception");

    }
}
