package com.tsystems.sbs.gitblitbranchsource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import hudson.Util;
import net.sf.json.JSONObject;

public class Connector {
	
	public static JSONObject connect(String apiUri) throws IOException {
		String apiUrl = Util.fixEmptyAndTrim(apiUri);
		
		//TODO: Does Gitblit require credentials?
		
		HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
		
		InputStream stream = connection.getInputStream();
		InputStreamReader isReader = new InputStreamReader(stream ); 

		//put output stream into a string
		BufferedReader br = new BufferedReader(isReader );
		
		StringBuilder jsonString = new StringBuilder();
		String line;
		while((line = br.readLine()) != null)
			jsonString.append(line);
		
		JSONObject response = JSONObject.fromObject(jsonString.toString()); 

		return response;
	}
	
}
