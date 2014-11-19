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
        boolean converge=JConvergence.check();
    }
}
