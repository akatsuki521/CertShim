package org.CertShim;

import javax.net.ssl.SSLSession;

/**
 * Each CA alternative method should implement this interface.
 */
public interface SSLCheckable {
    boolean check(SSLSession session);
}
