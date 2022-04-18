package com.HHMS;


import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MQTT_PUB {

	public static void main(String[] args) {
     
		try
		{
			
			    final String serverUrl   = "tcp://220.118.147.52:7883";     
		        final String clientId    = "RW_WATCH_01";
		        final String message     = "ID || HD ^^ EQID || c3163f072f50745a ^^ HNID || 5 ^^ LAT || 33.237252 ^^ LNG || 126.515601 ^^ HR || 90 ^^ TS || 1648099351515";  // example data
		        //final String tenant      = "<<tenant_ID>>";
		        final String username    = "rwit";
		        final String password    = "5be70721a1a11eae0280ef87b0c29df5aef7f248";
		        final String topic = "RW/JD/DS"; //  RW/JD/DI TODO chanag!!!!

		     	        
                // MQTT connection options
		        final MqttConnectOptions options = new MqttConnectOptions();
		        //options.setUserName(tenant + "/" + username);
		        options.setCleanSession(true);
				options.setKeepAliveInterval(1000);
				options.setAutomaticReconnect(true);				
				options.setConnectionTimeout(1000);
		        options.setUserName(username);
		        options.setPassword(password.toCharArray());

		        final MqttClient client = new MqttClient(serverUrl, clientId, new MemoryPersistence());
                client.connect(options);
                
                /*
                 //message publish
    		     client.publish(topic, (message).getBytes(), 0, false);
    		     
    	        */
		        
                
               /*
                * TEST 
                * message Send
                */
                Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(new Runnable() {
                    public void run () {
                        try {
                        	
                        	if(!client.isConnected())
                        	{
                        		client.connect();
                        		
                        	}                        	
                        	
                         
                            System.out.println("Sending message...");
                            client.publish(topic, message.getBytes(), 0, false); //message Send
                            
                            try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
                            
                        	if(!client.isConnected())
                        	{
                        		client.connect();
                        		
                        	}   
                            
                            //System.out.println("ReSending message...");
                            //client.publish("RWIT/JD02", (temp + "-" + message).getBytes(), 0, false);
                            
                            System.out.println("Sending done...");
                            
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }
                }, 1, 1, TimeUnit.SECONDS);
		        

                
                //client.disconnect(); 
                //client.close(); ;
                 
			
		}
		catch(Exception ex)
		{
			System.out.println(ex.getMessage());
		}
			

	}//end-of-main

}//end-of-class
