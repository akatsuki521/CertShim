This is CertShim Java version.

Motivation:

According to "The Most Dangerous Code in the Word: Validating SSL certificates in Non-Browser Software", JSSE API is misused in several security critical applications. By default, JSSE doesn't turning on hostname verification. Thus the applications is vulnerable to Man-In-The-Middle(MITM) attack.
This project is aimed at correcting SSL(JSSE) API misuse and provide additional method to validate a certificate. Thus protect users from MITM attack.

Implementation:

CertShim focus on X509TrustManagerImpl, which is the default implementation to verify certificates. User's are free to add other target implementation where it is denoted by the comment. It changed the behavior upon JVM loads the target class, turning on hostname verification by default and put another verification function call in the method body. The additional verification method is called "Convergence". User can add their own verification method by implementing the Checkable interface. All the checking are done in parallel.

Install instruction:

1. run "sudo -v" to give install file permission for 5 mins.
2. run "sudo apt-get install sqlite3" if you don't have it.
3. In the CertShim folder, run "./install.sh" to compile and install.
4. Add "alias CertShim='java -javaagent:$HOME/.CertShim/ag.jar'" to your environment variable files. This is not required but suggested.
Done.
Now you can use CertShim by typing "CertShim classNameHere"
or "java -javaagent:$HOME/.CertShim/ag.jar classNameHere" if you didn't set up the alias.
