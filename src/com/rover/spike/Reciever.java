package com.rover.spike;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import messages.Complete;
import messages.Task;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class Reciever implements MqttCallback {
	
	TaskRepository tr = null;
	PrintWriter writer = null;
	public Reciever(String filename){
		try { 
			writer = new PrintWriter(filename, "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void registerRepo(TaskRepository tr){
		this.tr = tr;
	}
	
	public void close(){
		writer.close();
	}

	@Override
	public void connectionLost(Throwable arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void messageArrived(String arg0, MqttMessage mesg){		
		try{
			ByteArrayInputStream b1 = new ByteArrayInputStream(mesg.getPayload());
			ObjectInputStream o1 = new ObjectInputStream(b1);
			Object unknownMsg = o1.readObject();

			if (unknownMsg.getClass() == Complete.class) {
				Complete done = (Complete) unknownMsg;
				writer.println(done.taskId+" :completed at: "+done.timeComplete + " :server time of:"+System.nanoTime());
				tr.markDone(done.taskId);
				writer.flush();
			}else if (unknownMsg.getClass() == Task.class) {
				Task task = (Task) unknownMsg;
				writer.println(task.label + " :sent at: " + System.nanoTime());
			}

		} catch (Exception e) {
			// This must be a location message - not in a class since this is send
			// from a c++ app.
			String msg[] = mesg.toString().split("\\s+");
			if (msg[0].contentEquals("GPS")) {
				double loc_x = Double.parseDouble(msg[1]);
				double loc_y = Double.parseDouble(msg[2]);
				if(arg0.compareToIgnoreCase("gps/1")==0){
					writer.print("WayPoint0: ");
				}else{
					writer.print("WayPoint1: ");
				}
				writer.println("x:"+loc_x+" :y:"+loc_y+" :atTime: " + System.nanoTime());
			}
		}
	}
	
}
