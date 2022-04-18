package com.HHMS;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class RwMqttClient  implements MqttCallback {

	private MqttClient client;
	private MqttConnectOptions options;
	
    final String serverUrl   = "tcp://220.118.147.52:7883";     
    final String clientId    = "RW_WATCH_02"; 
    final String username    = "rwit";
    final String password    = "5be70721a1a11eae0280ef87b0c29df5aef7f248";
    final String[] topic = {"RW/JD/DS"};



	public  RwMqttClient init(){
		options = new MqttConnectOptions();
		options.setCleanSession(true);
		options.setKeepAliveInterval(1000);
		options.setAutomaticReconnect(true);				
		options.setConnectionTimeout(1000);
	    options.setUserName(username);
	    options.setPassword(password.toCharArray());
		try {
			client = new MqttClient(serverUrl, clientId, new MemoryPersistence());
			client.setCallback(this);			
			client.connect(options);
			client.subscribe(topic);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return this;
	}
	
	public boolean isConnected()
	{
		return client.isConnected();
	}

	public void connect() throws MqttSecurityException, MqttException
	{
		client.connect();
	}


	
	@Override
	public void connectionLost(Throwable arg0) {
		System.out.println("Connection lost! " + arg0.getMessage());
		
		/*
		if(!client.isConnected())
			try {
				client.connect();
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	     */
		
	}	
	
	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		try {
			System.out.println("Pub complete" + new String(token.getMessage().getPayload()));
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

   //Receive Message
	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception {
		System.out.println("-------------------------------------------------");
		System.out.println("topic :"  + topic);
		System.out.println("msg :"  + new String(msg.getPayload(),"UTF-8"));
		System.out.println("-------------------------------------------------");
		
	}
	
	
}//end-of-class