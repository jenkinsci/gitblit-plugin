package com.tsystems.sbs.gitblitbranchsource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import net.sf.json.JSONObject;

public class Connector {
	
	public static JSONObject connect(String apiUri) throws IOException {
		URL apiUrl = new URL(apiUri);
		
		//TODO: Does Gitblit require credentials?
		
		HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
		
		InputStream stream = connection.getInputStream();
		InputStreamReader isReader = new InputStreamReader(stream, "UTF-8"); 

		//put output stream into a string
		BufferedReader br = new BufferedReader(isReader );
		
		try {
			StringBuilder jsonString = new StringBuilder();
			String line;
			while((line = br.readLine()) != null)
				jsonString.append(line);
			
			JSONObject response = JSONObject.fromObject(jsonString.toString()); 
		
			return response;
		} finally {
			try { stream.close(); } catch (IOException e) { e.printStackTrace(); }
			try { isReader.close(); } catch (IOException e) {e.printStackTrace(); }
			try { br.close(); } catch (IOException e) {e.printStackTrace(); }
		}
	}
	
}
