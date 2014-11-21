package org.CertShim;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import sun.security.provider.certpath.OCSP;

/**
 * Check if the target certificate is revoked.
 */
public class RevokeCheck implements SSLCheckable {
    public boolean check(SSLSession session){
        Certificate[] certs;
        Certificate peerCert;
        Certificate issuerCert;
        /*TODO find a way to get issuer's certificate.*/
        try {
            certs = session.getPeerCertificates();
            peerCert=certs[0];
        }catch (SSLPeerUnverifiedException e){
            System.out.println("Not a valid SSL connection");
            return false;
        }
        return OCSP.check(peerCert, issuerCert);
    }
}
