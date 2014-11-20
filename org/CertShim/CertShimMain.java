package org.CertShim;

/**
 * The main frame of CertShim.
 */
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.util.ArrayList;


public class CertShimMain{
    static void check(Socket socket) throws CertificateException{
        if(socket==null||!socket.isConnected()||!(socket instanceof SSLSocket)){
            System.out.println("Not a SSL connection. Checking not performed.");
            return;
        }
        SSLSocket sslSocket=(SSLSocket)socket;
        SSLSession session=sslSocket.getHandshakeSession();
        if(session==null){
            throw new CertificateException("No session. CertShim can't do verification.");
        }
        String host=session.getPeerHost();
        int port=session.getPeerPort();
        ArrayList<Thread> checkings=new ArrayList<Thread>();
        //checkings.add(new Thread(new JCovergence(host, port)));

    }
}
class CheckThread extends Thread{
    SSLCheckable checkingFunction;
    boolean result;
    String host;
    int port;
    CheckThread(SSLCheckable checkingFunction, String host, int port){
        this.checkingFunction=checkingFunction;
        this.host=host;
        this.port=port;
    }
    @Override
    public void run(){
        result=checkingFunction.check(host, port);
    }
    boolean getResult(){
        return result;
    }
}
