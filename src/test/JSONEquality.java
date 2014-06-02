package test;

import org.json.simple.JSONObject;

public class JSONEquality {
	public static void main(String[] args) {
		JSONObject j1 = new JSONObject();
		JSONObject j2 = new JSONObject();
		
		j1.put("ip", "127.0.0.1");
		j1.put("port", (int) 22024);
		j2.put("port", (int) 22024);
		j2.put("ip", "127.0.0.1");
		
		System.out.println("j1 equals to j2?\t" + j1.equals(j2));
		
	}
}
