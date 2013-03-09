package com.example.jsongettest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.R.bool;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

	Button buttonGetHttp;
	Button buttonGetJson;
	TextView textViewStatus;
	String read;

	// flag, hogy ne fusson két hálózatos szál egyszerre
	private static boolean running = false;
	private static Object syncObject = new Object();
	Handler handler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		buttonGetHttp = (Button) findViewById(R.id.button1);
		buttonGetJson = (Button) findViewById(R.id.button2);
		textViewStatus = (TextView) findViewById(R.id.textView1);
		buttonGetJson.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				read = "json";
				readFromServer(read);
			}
		});
		buttonGetHttp.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				read = "html";
				readFromServer(read);
			}

		});

		// Handler, ami majd módosítani fogja a UI-t
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == 0) {
					Bundle b = msg.getData();
					// Kiírja a kapott szöveget
					textViewStatus.setText(b.getString("text"));
				} else if (msg.what == 1) {
					Bundle b = msg.getData();
					// Kiírja a kapott szöveget html-ként formázva
					textViewStatus.setText(Html.fromHtml(b.getString("text")));
				} else if (msg.what == 100) {
					textViewStatus.setText("NEM TUD KAPCSOLÓDNI A SZERVERHEZ");
				}
				super.handleMessage(msg);
			}
		};
	}

	// Olvasás a szervertõl
	private void readFromServer(final String s) {
		// Külön szálban, hogy ne blokkolódjon
		new Thread() {
			public void run() {
				synchronized (syncObject) {
					if (!running) {
						running = true;
						HttpClient httpclient = new DefaultHttpClient();
						// Az emulátorról elérhetõ localhost cím
						String URL = "http://10.0.2.2:8080/onlab1test/myServlet1?param=";
						HttpGet httpGet = new HttpGet(URL + s);
						HttpResponse response;
						HttpEntity entity;
						InputStream instream;
						try {
							response = httpclient.execute(httpGet);
							entity = response.getEntity();
							instream = entity.getContent();
							processResponse(instream, s);

						} catch (ClientProtocolException e) {
							// Hiba van, küld egy üzenetet, ami a UI-ra kiírja,
							// hogy hiba
							Message msg = handler.obtainMessage();
							msg.what = 100;
							// elküldi az üzenetet, majd a handler módosítja a
							// UI-t
							handler.sendMessage(msg);
							e.printStackTrace();
						} catch (IOException e) {
							// Hiba van, küld egy üzenetet, ami a UI-ra kiírja,
							// hogy hiba
							Message msg = handler.obtainMessage();
							msg.what = 100;
							// elküldi az üzenetet, majd a handler módosítja a
							// UI-t
							handler.sendMessage(msg);
							e.printStackTrace();
						}
						running = false;
					}
				}
			}
		}.start();
	}

	// Választ feldolgozó függvény
	private void processResponse(final InputStream aIS, final String s) {
		// ha html gombot nyomtuk
		if (s.equals("html")) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					aIS));
			StringBuilder sb = new StringBuilder();
			String line = null;
			try {
				// kiolvassa sorrol sorra
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
				// Beállítja az üzenetet a handler számára
				Message msg = handler.obtainMessage();
				msg.what = 1;
				Bundle b = new Bundle();
				b.putString("text", sb.toString());
				msg.setData(b);
				// elküldi az üzenetet, majd a handler módosítja a UI-t
				handler.sendMessage(msg);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (aIS != null) {
					try {
						aIS.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			// ha json gombot nyomtuk
		} else if (s.equals("json")) {
			JSONObject jsonObj;
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					aIS));
			StringBuilder sb = new StringBuilder();
			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
				StringBuilder temp = new StringBuilder();
				String str = sb.toString();
				jsonObj = new JSONObject(str);
				// Kiolvassa a megfelelõ kulcsokhoz tartozó értékeket
				temp.append("Keresztnév: " + jsonObj.get("Keresztnev"));
				temp.append("\n");
				temp.append("Vezetéknév: " + jsonObj.get("Vezeteknev"));
				// Beállítja az üzenetet a handler számára
				Message msg = handler.obtainMessage();
				msg.what = 0;
				Bundle b = new Bundle();
				b.putString("text", temp.toString());
				msg.setData(b);
				handler.sendMessage(msg);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (aIS != null) {
					try {
						aIS.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		}
	}

}
