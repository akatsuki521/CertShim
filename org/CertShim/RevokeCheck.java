package org.CertShim;

/**
 * Created by hanjiajun on 11/20/14.
 */
public class RevokeCheck implements SSLCheckable {
    public boolean check(String host, String port){
        return true;
    }
}
