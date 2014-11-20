package org.CertShim;

import java.net.Socket;

/**
 * Each CA alternative method should implement this interface.
 */
public interface SSLCheckable {
    boolean check(String host, int port);
}
