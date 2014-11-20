/*
CertShim Java Version.
*/
package org.CertShim;

import javassist.*;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;


public class Agent {
    public static void premain(String agentArgs, Instrumentation inst){
        System.out.println("Agent starts.");
        inst.addTransformer(new CertShimTrans());
    }
}

class CertShimTrans implements ClassFileTransformer{
    @Override
    public byte[] transform(ClassLoader classLoader, String className, Class<?> arg2, ProtectionDomain arg3, byte[] bytes)
        throws IllegalClassFormatException{

        //System.out.println("Invoked class name: "+className);
        final String tar="javax.net.ssl.X509ExtendedTrustManager";
        ClassPool pool=ClassPool.getDefault();
        CtClass curClass, supClass;
        try{

            curClass=pool.get(className.replace('/','.'));
            supClass=curClass.getSuperclass();
            if(supClass.getName().equals(tar)){
                System.out.println("Find X509ExtendTrustedManager implementation: "+className);
            }else{
                return null;
            }
            CtClass targetClass=curClass;
            String insertedCode="if($3==null) {$3=\"HTTPS\"; System.out.println(\"Host Name Verification Enabled.\");}";
            CtMethod method=targetClass.getDeclaredMethod("checkIdentity");
            method.insertBefore(insertedCode);
            System.out.println("Host Name Verification Enforced.");
            insertedCode="if($4){org.CertShim.CertShimMain.check($3);}";
            method=targetClass.getDeclaredMethod("checkTrusted");
            method.insertBefore(insertedCode);
            return targetClass.toBytecode();

        }catch(NotFoundException nfe){
            //e.printStackTrace();
            System.out.println("Unhandled class is "+className);
            return null;
        }catch(CannotCompileException cce){
            System.out.println("Can't compile when dealing with "+className);
            return null;
        }catch(IOException ioe){
            System.out.println("IO Exception in "+className);
        }

        return null;

    }
}