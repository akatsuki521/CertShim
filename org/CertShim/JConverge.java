package org.CertShim;

import java.util.*;
import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import java.sql.*;
import org.json.*;
import java.security.cert.*;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.codec.binary.*;
import org.apache.commons.codec.digest.*;
import org.apache.commons.lang3.StringUtils;

public class JConverge implements SSLCheckable {
    static Connection db;
    static Connection userdb;
    static List<JSONObject> notaries;
    static JSONObject config;
    static ConcurrentHashMap<String, Object[]> results = new ConcurrentHashMap<String, Object[]>();

    static Boolean debug = true;

//    public static void main(String[] args){
//        JConverge jc = new JConverge();
//        System.out.println( jc.check(args[0], args[1]) );
//    }

    public boolean check(SSLSession session) {
        /* Initialize */
        System.out.println("JConverge Starts.");
        try {
            init();
        } catch (Exception e) {
            if (debug)
                System.out.println("[-] Initialization failed");
            e.printStackTrace();
            System.exit(1);
        }
        /* Bail if we have wrong number of args */

        String host = session.getPeerHost();
        String port = ""+session.getPeerPort();
        String fingerprint = "";
            try {
                fingerprint = getFingerprint(host, port);
            } catch (CertificateEncodingException | IOException e) {
                if (debug)
                    System.out.println("[-] Failed to retrieve fingerprint");
                e.printStackTrace();
                System.exit(1);
            }

        /* Perform lookup */
        if (debug)
            System.out.println(String.format("[+] Notary lookup: %s:%s - %s",
                    host, port, fingerprint));
        boolean result = notarize(host, port, fingerprint);
        System.out.println(result);
        /* We're golden */
        if (result) {
            System.out.println("Yeah!");
            System.exit(0);
        }

        /* We're fucked */
        System.out.println("Fuck!");
        System.exit(1);
        return result;
    }

    public void init() throws Exception {
        String configPath = "/usr/local/etc/converge/converge.config";

        /* Load config */
        String data = new Scanner(new File(configPath)).useDelimiter("\\Z")
                .next();

        config = new JSONObject(data);

        /* Set up the dbs for caching... */
        Class.forName("org.sqlite.JDBC");

        db = DriverManager.getConnection(String.format("jdbc:sqlite:%s",
                config.getString("db")));

        userdb = DriverManager.getConnection(String.format("jdbc:sqlite:%s",
                config.getString("userdb")));

        /* Read our enabled notaries */
        notaries = new ArrayList<JSONObject>();
        String path = config.getString("notaries-dir");
        String files, bundlePath;
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                files = listOfFiles[i].getName();
                // if (files.equals(".DS_Store")) // STUPID MAC FILE
                // continue;
                bundlePath = path + files;
                data = new Scanner(new File(bundlePath)).useDelimiter("\\Z")
                        .next();
                notaries.add(new JSONObject(data));
            }
        }
    }

    /* Performs local x509 retrieval */
    public String getFingerprint(String host, String port)
            throws IOException, CertificateEncodingException {

        /* Open up our connection */
        String httpsUrl = String.format("https://%s:%s", host,port);
        System.out.println(httpsUrl);
        URL url = new URL(httpsUrl);
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.getResponseCode();

        /* Get out x509 cert */
        Certificate[] certs = con.getServerCertificates();
        String fingerprint = DigestUtils.sha1Hex(certs[0].getEncoded());

        /* Add colons as per convergence spec */
        fingerprint = StringUtils.join(fingerprint.split("(?<=\\G..)"), ":");
        return fingerprint.toUpperCase();
    }

    public boolean notarize(String host, String port, String fingerprint) {
        /* First, check our local cache */
        boolean result;
        try {
            result = cacheCheck(host, port, fingerprint);

            /* Cache hit, we're happy */
            if (result) {
                if (debug)
                    System.out.println(String.format(
                            "[+] Cache hit: %s:%s - %s", host, port,
                            fingerprint));
                return true;
            }
        } catch (SQLException e) {
            if (debug)
                System.out.println("[-] Failed to check local cache");
        }
        /* Cache miss, perform remote check */
        remoteCheck(host, port, fingerprint);
        ConcurrentHashMap<String, Object[]> remoteResult = results;

        /* Update our caches */
        try {
            cacheUpdate(remoteResult, host, port);
        } catch (SQLException e) {
            if (debug)
                System.out.println("[-] Failed to update local cache");
        }

        /* Count success/failures */
        int good = 0, bad = 0;
        Collection<Object[]> values = remoteResult.values();
        for (Object[] value : values) {
            if ((boolean) value[0])
                good += 1;
            else
                bad += 1;
        }
        if (debug)
            System.out.println(String.format(
                    "[+] Notary hits: %d\n[+] Notary misses: %d", good, bad));

        if (config.getString("notary-agreement").equalsIgnoreCase("consensus")) {
            if (bad > 0)
                return false;
        } else if (config.getString("notary-agreement").equalsIgnoreCase(
                "majority")) {
            if (good > bad)
                return true;
        }
        return false;
    }

    public boolean cacheCheck(String host, String port,
                                     String fingerprint) throws SQLException {
        return (_cacheCheck(host, port, fingerprint, db) > 0)
                || (_cacheCheck(host, port, fingerprint, userdb) > 0);
    }

    private int _cacheCheck(String host, String port,
                                   String fingerprint, Connection db) throws SQLException {
        String timestamp = Long.toString((System.currentTimeMillis() / 1000L)
                - 60L * 60L * 25L * 7L);
        String query = "SELECT COUNT(*) FROM fingerprints WHERE location = ? "
                + "AND fingerprint = ? AND timestamp > ?";
        PreparedStatement stmt = db.prepareStatement(query);
        stmt.setString(1, String.format("%s:%s", host, port));
        stmt.setString(2, fingerprint);
        stmt.setString(3, timestamp);
        int result = stmt.executeQuery().getInt(1);
        return result;
    }

    public void cacheUpdate(ConcurrentHashMap<String, Object[]> result,
                                   String host, String port) throws SQLException {
        _cacheUpdate(result, host, port, userdb);
        /* If we can update the global cache, do it */
        if ((new File(config.getString("db")).canWrite()))
            _cacheUpdate(result, host, port, db);
    }

    private void _cacheUpdate(ConcurrentHashMap<String, Object[]> result,
                                     String host, String port, Connection db) throws SQLException {
        for (Object[] r : result.values()) {
            if ((boolean) r[0]) {
                JSONArray data = ((JSONObject) r[1])
                        .getJSONArray("fingerprintList");
                for (int i = 0; i < data.length(); i++) {
                    JSONObject d = data.getJSONObject(i);
                    String fingerprint = d.getString("fingerprint");
                    String query = "INSERT OR IGNORE INTO fingerprints "
                            + "(location, fingerprint) VALUES (?, ?)";
                    PreparedStatement stmt = db.prepareStatement(query);
                    stmt.setString(1, String.format("%s:%s", host, port));
                    stmt.setString(2, fingerprint);
                    query = "UPDATE fingerprints SET timestamp = ? WHERE "
                            + "location = ?  AND fingerprint = ?";
                    stmt.execute();
                    stmt = db.prepareStatement(query);
                    stmt.setString(1,
                            Long.toString(System.currentTimeMillis() / 1000L));
                    stmt.setString(2, String.format("%s:%s", host, port));
                    stmt.setString(3, fingerprint);
                    stmt.execute();
                }
            }
        }
    }

    public class MyRunnable implements Runnable{
        private final JSONObject host;
        private final String fingerprint;
        private final String remoteHost;
        private final String remotePort;

        public MyRunnable(JSONObject host, String fingerprint, String remoteHost, String remotePort){
            this.host = host;
            this.fingerprint = fingerprint;
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
        }

        @Override
        public void run(){
            /* Build the notary request url */
            String url = String.format("https://%s:%s/target/%s+%s",
                    this.host.getString("host"), this.host.getInt("ssl_port"),
                    remoteHost, remotePort);
            System.out.println(url);
                /* Get the public key out of the cert */
            Certificate cert = null;
            try {
                cert = CertificateFactory.getInstance("X509")
                        .generateCertificate(
                                new ByteArrayInputStream(((String) host
                                        .get("certificate")).getBytes()));
            } catch (CertificateException e1) {
                return;
            }

            String response;
            try {
                String urlParameters = "fingerprint=" + fingerprint;
                URL request = new URL(url);
                HttpsURLConnection connection = (HttpsURLConnection) request
                        .openConnection();
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setInstanceFollowRedirects(false);
                connection.setUseCaches(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type",
                        "application/x-www-form-urlencoded");
                connection.setRequestProperty("charset", "utf-8");
                connection
                        .setRequestProperty(
                                "Content-Length",
                                ""
                                        + Integer.toString(urlParameters
                                        .getBytes().length));
                connection.connect();
                DataOutputStream wr = new DataOutputStream(
                        connection.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                    /*
                     * Weird bug: google.com+443 http response (409 conflict)
                     * url works fine with other clients
                     */
                if (connection.getResponseCode() != 200) {
                    markitZero(results, url);
                    return;
                }
                String str;
                response = "";
                InputStream input = connection.getInputStream();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(input));
                while ((str = br.readLine()) != null) {
                    response += str;
                }
                input.close();
                connection.disconnect();

            } catch (IOException e) {
                markitZero(results, url);
                return;
            }

                /* Rip out the signature */

            JSONObject data = new JSONObject(response);
            byte[] sigBytes = Base64.decodeBase64(data
                    .getString("signature"));
            // data.remove("signature");
            // String message = data.toString(); FAILS: order is messed up
            // message = message.replace(",", ", ").replace("\":", "\": ");
            String message = response.substring(0,
                    response.indexOf(", \"signature\""))
                    + "}";

                /* Check the sig to make up for not check the cert on connect */
            Signature sig;
            try {
                sig = Signature.getInstance("SHA1withRSA");

                sig.initVerify(cert);
                sig.update(message.getBytes());
                if (sig.verify(sigBytes))
                    markitGood(results, url, new JSONObject(response));
                else
                    markitZero(results, url);
            } catch (Exception e) {
                markitZero(results, url);
                return;
            }
        }
    }


    public void remoteCheck(String remoteHost, String remotePort, String fingerprint) {

        /* Note we don't check SSL validity upon request */
        disableCertificateValidation();

        /* Loop over all notaries */

        int hostNum = 0;
        for(JSONObject notary : notaries){
            JSONArray hosts = notary.getJSONArray("hosts");
            hostNum += hosts.length();
        }

        ExecutorService exec = Executors.newFixedThreadPool(hostNum + 1);

        for (JSONObject notary : notaries) {
            JSONArray hosts = notary.getJSONArray("hosts");

            /* Loop over all hosts in a notary */
            for (int i = 0; i < hosts.length(); i++) {
                JSONObject host = hosts.getJSONObject(i);
                Runnable worker = new MyRunnable(host, fingerprint, remoteHost, remotePort);
                exec.execute(worker);
            }
        }
        exec.shutdown();
        while(!exec.isTerminated()){
        }
    }

    public void markitGood(ConcurrentHashMap<String, Object[]> results, String url, JSONObject response) {
        Object[] value = { true, response };
        results.put(url, value);
    }

    public void markitZero(ConcurrentHashMap<String, Object[]> results, String url) {
        Object[] value = { false };
        results.put(url, value);
    }

    /* Source: https://gist.github.com/henrik242/1510165 */
    public void disableCertificateValidation() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            public void checkClientTrusted(X509Certificate[] certs,
                                           String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs,
                                           String authType) {
            }
        } };

        // Ignore differences between given hostname and certificate hostname
        HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (Exception e) {
        }
    }
}
