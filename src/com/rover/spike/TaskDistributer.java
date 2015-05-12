package com.rover.spike;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.Set;

import messages.Complete;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class TaskDistributer {

	private static void printUsage(){
		System.out.print("-b <broker address, eg: tcp://192.168.0.100:1883>\n");
		System.out.print("-c <client id string name>\n");
		System.out.print("-f <list of folder containing batches of tasks, must be last arg>\n");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LinkedList<String> taskRepoFldr = new LinkedList<String>();
		int qos             = 1;
		String broker       = "tcp://192.168.0.100:1883";
		String clientId     = "taskDist";
		int batches			= 1;
        MemoryPersistence persistence = new MemoryPersistence();
		//input args
		for(int i=0;i<args.length;i++){
			if(args[i].compareTo("-b")==0){
				if(++i < args.length){
					broker = args[i++];
				}else{
					printUsage();
					break;
				}
			}
			if(args[i].compareTo("-c")==0){
				if(++i < args.length){
					clientId = args[i++];
				}else{
					printUsage();
					break;
				}
			}

			if(args[i].compareTo("-n")==0){
				if(++i < args.length){
					batches = Integer.parseInt(args[i++]);
				}else{
					printUsage();
					break;
				}
			}
			if(args[i].compareTo("-f")==0){
				if(!(i+1 < args.length)){
					printUsage();
					break;
				}
				while(++i < args.length){
					taskRepoFldr.add(args[i++]);
				}
				break;
			}
		}
		if(taskRepoFldr.isEmpty()){
			System.err.print("No input folders\n");
			System.exit(-1);
		}
			
		try {

            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: "+broker);
            sampleClient.connect(connOpts);
            System.out.println("Connected");
    		for(int i=0;i<batches;i++){
    			String current = taskRepoFldr.pop();
    			TaskRepository taskBatch = new TaskRepository(current);
    			Set<String> tasksIds = taskBatch.taskDefinitions.keySet();
                
                for(String task : tasksIds){
                	ByteArrayOutputStream b = new ByteArrayOutputStream();
                	try {
                		ObjectOutputStream o = new ObjectOutputStream(b);
                		o.writeObject(taskBatch.GetTask(task));
                		o.close();
                	} catch (IOException e) {
                		e.printStackTrace();
                	}

                	byte bytes[]=b.toByteArray();

                	System.out.println("Publishing task: "+ task);
                	MqttMessage message = new MqttMessage(bytes);
                	message.setQos(qos);
                	sampleClient.publish("tasklist", message);
                	System.out.println("Message published");
                }
                

    			sampleClient.setCallback(taskBatch);
                sampleClient.subscribe("complete");
                
                while(!taskBatch.complete()){
                	try {
                		Thread.sleep(1000);
                	} catch (InterruptedException e) {
                		e.printStackTrace();
                	}
                }
                
                sampleClient.unsubscribe("complete");
                sampleClient.setCallback(null);
    			taskRepoFldr.addLast(current);
    		}
            
            sampleClient.disconnect();
            System.out.println("Disconnected");
            System.exit(0);
        } catch(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
        }
		

	}

}
