package com.HHMS;

public class MQTT_SUB {
		

	public static void main(String[] args) throws InterruptedException {

		   RwMqttClient mqttClient1 = new RwMqttClient();
		   mqttClient1.init();
		   		
	       
		   new Thread( ()->{
	            try {
	            	
	            	if(!mqttClient1.isConnected())
                	{
	            		mqttClient1.connect();
                		
                	}               	
	            	
	                Thread.sleep(1000);
	                System.out.println("2) connect Msg Broker ....");
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        } ).start(); 
	        
	        
		  		  
       
	}//end-of-main

}//end-of-class
