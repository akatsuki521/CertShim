package org.CertShim;

/**
 * The main frame of CertShim.
 */
import javax.net.ssl.SSLSocket;
import java.net.Socket;
import java.security.cert.CertificateException;


public class CertShimMain{
    static void check(Socket socket) throws CertificateException{
        if(!socket.isConnected()||!(socket instanceof SSLSocket)){
            System.out.println("Not a SSL connection. Checking not performed.");
            return;
        }
        //boolean converge=JConvergence.check();
    }
}
class CheckThread extends Thread{
    SSLCheckable checkingFunction;
    boolean result;
    Socket socket;
    CheckThread(SSLCheckable checkingFunction, Socket socket){
        this.checkingFunction=checkingFunction;
        this.socket=socket;
    }
    @Override
    public void run(){
        result=checkingFunction.check(socket);
    }
    boolean getResult(){
        return result;
    }
}
