package com.tsystems.sbs.gitblitbranchsource;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import hudson.Util;

public class Connector {
	
//	private static final Map<Details, GitHub> gitblits = new HashMap<>();

	public static String connect(String apiUri) throws IOException {
		String apiUrl = Util.fixEmptyAndTrim(apiUri);
		
		//TODO: Does Gitblit require credentials?
		
		HttpURLConnection connection = (HttpURLConnection) new URL(apiUri).openConnection();
		StringBuffer response = new StringBuffer();
		response.append("Response code: ").append(connection.getResponseCode()).append(" Response message: ").append(connection.getResponseMessage());
		
		return response.toString();
	}
	
}
