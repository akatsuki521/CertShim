package org.CertShim;

import java.net.Socket;

/**
 * Created by hanjiajun on 11/19/14.
 */
public interface SSLCheckable {
    boolean check(Socket socket);
}
