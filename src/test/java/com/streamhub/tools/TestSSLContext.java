package com.streamhub.tools;

import java.io.File;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.KeyStore.Builder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

public class TestSSLContext {
	private static final String KEYSTORE_LOCATION = ".keystore";
	private static final String ALGORITHM = "SunX509";
	private static final String PROTOCOL = "SSL";
	private static final char[] PASSWORD = "password".toCharArray();

	public static SSLContext newInstance() throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
		SSLContext context = SSLContext.getInstance(PROTOCOL);
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(ALGORITHM);
		Builder builder = Builder.newInstance(KeyStore.getDefaultType(), null, new File(KEYSTORE_LOCATION), new KeyStore.PasswordProtection(PASSWORD));
		KeyStore keystore = builder.getKeyStore();
		kmf.init(keystore, PASSWORD);
		context.init(kmf.getKeyManagers(), null, null);
		return context;
	}
}
