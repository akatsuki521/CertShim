package org.CertShim;

/**
 * The main frame of CertShim.
 */
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.concurrent.*;


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
        ArrayList<Future<Boolean>> futureResults=new ArrayList<Future<Boolean>>();
        Boolean[] finalResults=new Boolean[checkings.size()];
        //Keep adding if there more module.
        ExecutorService threadPool= Executors.newFixedThreadPool(checkings.size());
        for(CheckThread curThread: checkings){
            futureResults.add(threadPool.submit(curThread));
        }
        threadPool.shutdown();
        for(int i=0; i<finalResults.length; i++){
            try {
                finalResults[i] = futureResults.get(i).get();
            }catch(InterruptedException | ExecutionException e){
                System.out.println(e);
            }
        }
    }
}
class CheckThread implements Callable<Boolean>{
    SSLCheckable checkingFunction;
    String host;
    String port;
    CheckThread(SSLCheckable checkingFunction, String host, String port){
        this.checkingFunction=checkingFunction;
        this.host=host;
        this.port=port;
    }
    @Override
    public Boolean call(){
        return checkingFunction.check(host, port);
    }
}

