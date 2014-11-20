package org.CertShim;

/**
 * The main frame of CertShim.
 */
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


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
        String port=""+session.getPeerPort();
        ArrayList<CheckThread> checkings=new ArrayList<CheckThread>();
        checkings.add(new CheckThread(new JConverge(), host, port));
        //Keep adding if there more module.
        ExecutorService threadPool= Executors.newFixedThreadPool(checkings.size());
        for(CheckThread curThread: checkings){
            threadPool.execute(curThread);
        }
        threadPool.shutdown();

    }
}
class CheckThread extends Thread{
    SSLCheckable checkingFunction;
    boolean result;
    String host;
    String port;
    CheckThread(SSLCheckable checkingFunction, String host, String port){
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
