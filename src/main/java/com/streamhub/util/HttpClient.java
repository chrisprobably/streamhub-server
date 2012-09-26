package com.streamhub.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

public class HttpClient {
	public static void enableTrustAllCerts() {
		TrustManager[] managers = new TrustManager[] { new AcceptAllTrustManager() };
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, managers, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HostnameVerifier hv = new HostnameVerifier() {
				public boolean verify(String arg0, SSLSession arg1) {
					return true;
				}
			};
			HttpsURLConnection.setDefaultHostnameVerifier(hv);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String get(URL url) throws IOException {
		URLConnection urlConnection = url.openConnection();
		urlConnection.setUseCaches(false);
		urlConnection.setRequestProperty("Connection", "close");
		return getResponse(urlConnection);
	}

	public static Map<String, List<String>> getHeaderFields(URL url) throws IOException {
		URLConnection urlConnection = url.openConnection();
		return urlConnection.getHeaderFields();
	}

	private static String getResponse(URLConnection urlConnection) throws IOException {
		BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
		StringBuilder builder = new StringBuilder();

		int in;

		try {
			while ((in = inputStream.read()) != -1) {
				builder.append((char) in);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return builder.toString();
	}

	public static byte[] getAsBytes(URL url) throws IOException {
		URLConnection urlConnection = url.openConnection();
		urlConnection.setUseCaches(false);
		urlConnection.setRequestProperty("Connection", "close");
		return getBytesResponse(urlConnection);
	}

	private static byte[] getBytesResponse(URLConnection urlConnection) throws IOException {
		int contentLength = urlConnection.getContentLength();
		InputStream raw = urlConnection.getInputStream();
		InputStream in = new BufferedInputStream(raw);
		byte[] data = new byte[contentLength];
		int bytesRead = 0;
		int offset = 0;
		while (offset < contentLength) {
			bytesRead = in.read(data, offset, data.length - offset);
			if (bytesRead == -1)
				break;
			offset += bytesRead;
		}
		in.close();

		if (offset != contentLength) {
			throw new IOException("Only read " + offset + " bytes; Expected " + contentLength + " bytes");
		}
		return data;
	}
}