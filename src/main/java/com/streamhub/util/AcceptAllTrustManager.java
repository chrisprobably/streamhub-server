package com.streamhub.util;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

public class AcceptAllTrustManager implements X509TrustManager {
	public boolean isClientTrusted(X509Certificate[] chain) {
		return true;
	}

	public boolean isHostTrusted(X509Certificate[] chain) {
		return true;
	}

	public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
	}

	public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
	}

	public X509Certificate[] getAcceptedIssuers() {
		return null;
	}
}
