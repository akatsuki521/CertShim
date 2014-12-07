This is CertShim Java version.
Here is the instruction to install CertShim:
1. run "sudo -v" to give install file permission for 5 mins.
2. run "sudo apt-get install sqlite3" if you don't have it.
3. In the CertShim folder, run "./install.sh" to compile and install.
4. Add "alias='java -javaagent:$HOME/.CertShim/ag.jar'" to your environment variable files. This is not required but suggested.
Done.
Now you can use CertShim by typing "CertShim classNameHere"
or "java -javaagent:$HOME/.CertShim/ag.jar classNameHere" if you didn't set up the alias.