package com.rover.spike;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import messages.Complete;
import messages.Method;
import messages.Task;
import messages.Task.AgentTypes;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TaskRepository {

	boolean debugFlag = true;
	String repositoryFolderPath = "";
	Map<String,Task> taskDefinitions;

	public void markDone(String taskId){
		System.out.print("Task: " + taskId + " complete.\n");
		taskDefinitions.remove(taskId);
	}

	public boolean complete(){
		return taskDefinitions.isEmpty();
	}

	public TaskRepository(String repositoryFolderPath)
	{
		this.repositoryFolderPath = repositoryFolderPath;
		this.taskDefinitions= new HashMap<String,Task>();
		File folder = new File(repositoryFolderPath);
		for(File i : folder.listFiles()){
			ReadTaskDescriptions(i);
		}
	}

	public void ReadTaskDescriptions(File file)
	{
		try {
			FileInputStream fis = null;
			fis = new FileInputStream(file);
			//Get the DOM Builder Factory
			DocumentBuilderFactory factory =  DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(fis);
			NodeList nodeList = document.getDocumentElement().getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				//We have encountered a <Taems> tag.
				Node node = nodeList.item(i);
				if (node instanceof Element) {
					Task t = ParseTask(node);
					this.taskDefinitions.put(t.label, t);
					System.out.print("Task " + t.label+ " added to repository\n");
				}
			}
		} 
		catch (IOException e) {}
		catch(ParserConfigurationException e){}
		catch(SAXException e){}
	}

	public Task GetTask(String name)
	{
		if (!this.taskDefinitions.containsKey(name))
		{
			System.err.print("Possible Error: Task " + name + " not found in repository\n");
		}
		return this.taskDefinitions.get(name);
	}

	static int uid = 0;
	private Task ParseTask(Node node)
	{
		String taskId;
		String taskName;
		String qafStringValue;
		boolean recurring = false;
		boolean isTask = node.getNodeName()=="Task";
		taskId = node.getAttributes().getNamedItem("id").getNodeValue() + uid++;
		taskName = node.getAttributes().getNamedItem("name").getNodeValue();
		if (isTask) {
			Node recurringNode = node.getAttributes().getNamedItem("recurring");
			if (recurringNode!=null)
				recurring = recurringNode.getNodeValue().toString().toLowerCase().equalsIgnoreCase("true");
			qafStringValue = node.getAttributes().getNamedItem("qaf").getNodeValue().toString().toLowerCase();
		}
		AgentTypes type = AgentTypes.AMBULANCE;
		if (taskName.equalsIgnoreCase("Police"))
		{
			type = AgentTypes.POLICE;
		}
		else if (taskName.equalsIgnoreCase("Ambulance"))
		{
			type = AgentTypes.AMBULANCE;
		}
		Task task = new Task(taskId, type);
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node childNode = children.item(i);
			if (childNode.getNodeName().equalsIgnoreCase("Method"))
			{
				messages.Method childMethod = ParseMethod(childNode);
				task.addNode(childMethod);
			}
			else if (childNode.getNodeName().equalsIgnoreCase("Task"))
			{
				Task childTask = ParseTask(childNode);
				task.addNode(childTask);
			}
		}
		return task;
	}

	private messages.Method ParseMethod(Node node)
	{
		String methodName = node.getAttributes().getNamedItem("name").getNodeValue();
		int quality = Integer.parseInt(node.getAttributes().getNamedItem("Quality").getNodeValue());
		int duration = Integer.parseInt(node.getAttributes().getNamedItem("Duration").getNodeValue());
		int xCoord = Integer.parseInt(node.getAttributes().getNamedItem("XCoord").getNodeValue());
		int yCoord = Integer.parseInt(node.getAttributes().getNamedItem("YCoord").getNodeValue());
		Method method = new Method(methodName,(double)xCoord,(double)yCoord,System.nanoTime(),quality,duration);
		return method;
	}

	
	
}
