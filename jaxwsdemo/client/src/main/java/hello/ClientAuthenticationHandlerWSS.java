// Copyright (c) 2009-2011 Coverity, Inc. All rights reserved worldwide.
package hello;

import com.sun.xml.wss.ProcessingContext;
import com.sun.xml.wss.XWSSProcessor;
import com.sun.xml.wss.XWSSProcessorFactory;
import com.sun.xml.wss.XWSSecurityException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

/**
 * SOAP handler for user authentication using ws-security. Not used on the server
 * but is instead intended to be used by a client app to enable authentication on
 * the server for the various ces webservices to succeed. Note that there are no
 * dependencies on any ces code.
 *
 * @author delarde
 */
public class ClientAuthenticationHandlerWSS implements SOAPHandler<SOAPMessageContext> {

    public static final String WSS_AUTH_PREFIX = "wsse";
    public static final String WSS_AUTH_LNAME = "Security";
    public static final String WSS_AUTH_URI = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private XWSSProcessor xwssProcessor = null;

    public ClientAuthenticationHandlerWSS(String configFile) {
        InputStream xwssConfig = null;
        try {
            //read client side security config
            xwssConfig = ClassLoader.getSystemResourceAsStream(configFile);
            XWSSProcessorFactory factory = XWSSProcessorFactory.newInstance();
            xwssProcessor = factory.createProcessorForSecurityConfiguration(xwssConfig, new SecurityEnvironmentHandler());
        } catch (XWSSecurityException se) {
            throw new RuntimeException(se);
        } finally {
            try {
                if (xwssConfig != null) {
                    xwssConfig.close();
                }
            } catch (IOException ioe) {
                System.out.println("Error during init: " + ioe.getMessage());
            }
        }
    }

    @PostConstruct
    public void init() {
    }

    @PreDestroy
    public void destroy() {
    }

    @Override
    public boolean handleFault(SOAPMessageContext mc) {
        return true;
    }

    @Override
    public void close(MessageContext mc) {
    }

    @Override
    public Set<QName> getHeaders() {
        QName securityHeader = new QName(WSS_AUTH_URI, WSS_AUTH_LNAME, WSS_AUTH_PREFIX);
        HashSet<QName> headers = new HashSet<QName>();
        headers.add(securityHeader);
        return headers;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext smc) {
        boolean outbound = ((Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)).booleanValue();
        SOAPMessage msg = smc.getMessage();
        if (outbound) {
            try {
                ProcessingContext context = xwssProcessor.createProcessingContext(msg);
                context.setSOAPMessage(msg);
                SOAPMessage secureMsg = xwssProcessor.secureOutboundMessage(context);
                smc.setMessage(secureMsg);
            } catch (XWSSecurityException ex) {
                throw new RuntimeException(ex);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    //----------------------------------------------------
    // Callback Handler, PasswordValidator
    //----------------------------------------------------
    private class SecurityEnvironmentHandler implements CallbackHandler {

        @Override
        public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
        }
    }
}
