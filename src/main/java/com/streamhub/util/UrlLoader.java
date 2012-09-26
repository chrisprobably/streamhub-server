package com.streamhub.util;

import java.net.MalformedURLException;
import java.net.URL;

public class UrlLoader {
	public static URL load(String urlString) throws MalformedURLException {
		if (urlString.startsWith("classpath:")) {
			urlString = urlString.replaceFirst("classpath:", "");
			URL resource = UrlLoader.class.getResource(urlString);
			
			if (resource == null) {
				resource = UrlLoader.class.getResource("/" + urlString);
			}
			
			return resource;
		}
		
		return new URL(urlString);
	}
}
