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
        SSLSession session=sslSocket.getSession();
        if(session==null){
            System.out.println("Null session");
            throw new CertificateException("No session. CertShim can't do verification.");
        }
//        String host=session.getPeerHost();
//        String port=""+session.getPeerPort();
        ArrayList<CheckThread> checkings=new ArrayList<>();
        //checkings.add(new CheckThread(new JConverge(), session));
        /*
        *
        * If you have further modules, just keep adding here.
        *
        */
        ArrayList<Future<Boolean>> futureResults=new ArrayList<Future<Boolean>>();
        Boolean[] finalResults=new Boolean[checkings.size()];
        ExecutorService threadPool= Executors.newFixedThreadPool(checkings.size());
        for(CheckThread curThread: checkings){
            futureResults.add(threadPool.submit(curThread));
        }
        threadPool.shutdown();
        int counter=0;
        for(int i=0; i<finalResults.length; i++){
            try {
                finalResults[i] = futureResults.get(i).get();
                if(finalResults[i]){
                    counter++;
                }
            }catch(InterruptedException | ExecutionException e){
                System.out.println(e);
            }
        }
        /*TODO Handle the produced results*/
        System.out.format("CertShim: There are %d out of %d verification passed.\n", counter, finalResults.length);
    }
}
class CheckThread implements Callable<Boolean>{
    SSLCheckable checkingFunction;
    SSLSession session;
    CheckThread(SSLCheckable checkingFunction, SSLSession session){
        this.checkingFunction=checkingFunction;
        this.session=session;
    }
    @Override
    public Boolean call(){
        return checkingFunction.check(session);
    }
}

