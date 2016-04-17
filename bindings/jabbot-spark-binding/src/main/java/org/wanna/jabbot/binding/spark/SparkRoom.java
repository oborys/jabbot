package org.wanna.jabbot.binding.spark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wanna.jabbot.binding.AbstractRoom;
import org.wanna.jabbot.binding.BindingListener;
import org.wanna.jabbot.binding.config.RoomConfiguration;
import org.wanna.jabbot.binding.messaging.DefaultResource;
import org.wanna.jabbot.binding.messaging.Message;
import org.wanna.jabbot.binding.DefaultBindingMessage;
import org.wanna.jabbot.binding.messaging.body.BodyPart;
import org.wanna.jabbot.binding.messaging.body.TextBodyPart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Stack;
import java.net.URI;

/**
 * @author tsearle <tsearle>
 * @since 2016-03-08
 */
public class SparkRoom extends AbstractRoom<Object> implements Runnable {
	private final static Logger logger = LoggerFactory.getLogger(SparkRoom.class);
	private RoomConfiguration configuration;
	private final List<BindingListener> listeners;
	private com.ciscospark.Room room;
	private com.ciscospark.Spark spark;
	private com.ciscospark.SparkServlet servlet;
	private boolean useWebhook;
	private String webhookUrl;


	public SparkRoom(SparkBinding connection,List<BindingListener> listeners) {
		super(connection);
		this.listeners = (listeners==null?new ArrayList<BindingListener>() : listeners);
		spark = connection.spark;
		servlet = connection.sparkServlet;
		useWebhook = connection.useWebhook;
		webhookUrl = connection.webhookUrl;

	}

	@Override
	public void run()
	{
		logger.info("SPARK Room started");
		String lastId = null;
		while (true) {
			Stack<com.ciscospark.Message> msgList = new Stack<com.ciscospark.Message>();
			
			while (true) {
				String nextId = null;
				try {
					Iterator<com.ciscospark.Message> msgs = spark.messages()
						.queryParam("roomId", room.getId())
						.queryParam("max", "20")	

						.iterate();
					while(msgs.hasNext()) {
						com.ciscospark.Message imsg = msgs.next();

						if (lastId == null) {
							nextId = imsg.getId();
							break;
						}

						if (nextId == null) {
							nextId = imsg.getId();
						}

						if (lastId.equals(imsg.getId())) {
							//caugh up
							break;
						}

						logger.info("Message Received (" + imsg.getPersonEmail() + ") " + imsg.getText());
						msgList.push(imsg);
					}

					lastId = nextId;

					if (!msgList.empty()) break;
				} catch (Exception e) {
					logger.error("Error in message handling!" , e);
				}
				try {
					Thread.sleep(5000);
				} catch(Exception e) {}
			}


			while(!msgList.empty()) {
				com.ciscospark.Message msg = msgList.pop();
				dispatchMessage(msg);
			}
		}
	}

	@Override
	public boolean sendMessage(Message message) {
		com.ciscospark.Message msg = new com.ciscospark.Message();
		msg.setRoomId(room.getId());
		msg.setText(message.getBody());
		spark.messages().post(msg);
		return true;
	}

	@Override
	public boolean join(final RoomConfiguration configuration) {
		this.configuration = configuration;

		Iterator<com.ciscospark.Room> rooms = spark.rooms().iterate();
		while(rooms.hasNext()) {
			com.ciscospark.Room rm = rooms.next();
			logger.debug("comparing room " + rm.getTitle() 
					+ " to " + configuration.getName());
			if(rm.getTitle().equals(configuration.getName())) {
				room = rm;
				break;
			}
		}

		if (room == null) {
			logger.info("Room " + configuration.getName() + " not found. Creating!");
			room = new com.ciscospark.Room();
			room.setTitle(configuration.getName());
			room = spark.rooms().post(room);
			logger.info("Room created id: " + room.getId());
		}
		if (!useWebhook) {
			new Thread(this).start();
		} else {
			com.ciscospark.Webhook hook = new com.ciscospark.Webhook();
			hook.setName("midori hook");
			hook.setTargetUrl(URI.create(webhookUrl));
			hook.setResource("messages");
			hook.setEvent("created");
			hook.setFilter("roomId=" + room.getId());
			spark.webhooks().post(hook);
			logger.info("created webhook " + hook.getName() + " id: " + hook.getId());

			servlet.addListener( new com.ciscospark.WebhookEventListener() {
					public void onEvent(com.ciscospark.WebhookEvent event) {
							if (event.getData().getRoomId().equals(room.getId())) {
								com.ciscospark.Message msg = event.getData();
								if(msg.getText() == null) {
									logger.info("Getting full message for " + event.getData().getId());
									msg = spark.messages().path("/"+event.getData().getId()).get();
								} else {
									logger.info("Message already in webhook, delivering");
								}
								dispatchMessage(msg);
							}
					}
				});
		}

			

		return true;
	}

	private void dispatchMessage(com.ciscospark.Message msg) {
		for (BindingListener listener : listeners) {
		    DefaultBindingMessage message = new DefaultBindingMessage();
		    message.addBody(new TextBodyPart(msg.getText()));
		    message.setSender(new DefaultResource(this.getRoomName(),msg.getPersonEmail()));
		    message.setDestination(new DefaultResource(this.getRoomName(),null));
		    message.setRoomName(this.getRoomName());
		    listener.onMessage(message);
		}
	}

	@Override
	public String getRoomName() {
		return configuration.getName();
	}
}
