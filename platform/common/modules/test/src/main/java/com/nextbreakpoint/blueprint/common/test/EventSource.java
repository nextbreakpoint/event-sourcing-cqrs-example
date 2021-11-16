package com.nextbreakpoint.blueprint.common.test;

import com.nextbreakpoint.blueprint.common.core.Environment;
import com.nextbreakpoint.blueprint.common.vertx.HttpClientFactory;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.VertxException;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpClient;

import java.net.MalformedURLException;
import java.util.HashMap;

public class EventSource {
	private final Environment environment;
	private final Vertx vertx;
	private final String serviceUrl;
	private final JsonObject config;
	private HttpClient client;
	private volatile boolean connecting;
	private volatile boolean connected;
	private String lastId;
	private Handler<String> messageHandler;
	private final HashMap<String, Handler<String>> eventHandlers;
	private SSEPacket currentPacket;
	private Handler<Void> closeHandler;

	public EventSource(Environment environment, Vertx vertx, String serviceUrl, JsonObject config) {
		this.environment = environment;
		this.vertx = vertx;
		this.serviceUrl = serviceUrl;
		this.config = config;
		eventHandlers = new HashMap<>();
	}

	public EventSource connect(String path, String lastEventId, Handler<AsyncResult<Void>> connectHandler) throws MalformedURLException {
		if (connected || connecting) {
			throw new VertxException("SSEConnection already connected");
		}

		connecting = true;

		if (client == null) {
			client = HttpClientFactory.create(environment, vertx, serviceUrl, config);
		}

		client.rxRequest(HttpMethod.GET, path)
				.flatMap(request -> {
					request.headers().add("Accept", "text/event-stream");
					request.setChunked(true);
					if (lastEventId != null) {
						request.headers().add("Last-Event-ID", lastEventId);
					}
					request.exceptionHandler(e -> {
						connecting = false;
						connected = false;
						connectHandler.handle(Future.failedFuture(e));
					});
					return request.rxSend();
				})
				.doOnSuccess(response -> {
					response.exceptionHandler(e -> {
						connecting = false;
						connected = false;
						connectHandler.handle(Future.failedFuture(e));
					});
					if (response.statusCode() != 200) {
						VertxException ex = new VertxException("Cannot connect");
						connectHandler.handle(Future.failedFuture(ex));
					} else {
						connecting = false;
						connected = true;
						if (closeHandler != null) {
							response.endHandler(closeHandler);
						}
						response.handler(this::handleMessage);
						connectHandler.handle(Future.succeededFuture());
					}
				})
				.toCompletable()
				.await();

		return this;
	}

	public EventSource close() {
		if (client != null) {
			client.rxClose()
				.toCompletable()
				.await();
			client = null;
		}
		connecting = false;
		connected = false;
		return this;
	}

	public EventSource onMessage(Handler<String> messageHandler) {
		this.messageHandler = messageHandler;
		return this;
	}

	public EventSource onEvent(String eventName, Handler<String> handler) {
		eventHandlers.put(eventName, handler);
		return this;
	}

	public EventSource onClose(Handler<Void> closeHandler) {
		this.closeHandler = closeHandler;
		return this;
	}

	public String lastId() {
		return lastId;
	}

	private void handleMessage(Buffer buffer) {
//		if (currentPacket == null) {
			currentPacket = new SSEPacket();
//		}
		boolean terminated = currentPacket.append(buffer);
		if (terminated) {
			// choose the right handler and call it
			Handler<String> handler = messageHandler;
			String header = currentPacket.headerName;
			if (header == null) {
				messageHandler.handle(currentPacket.toString());
				return;
			}
			switch (header) {
				case "event":
					handler = eventHandlers.get(currentPacket.headerValue);
					break;
				case "id":
					handler = messageHandler;
					lastId = currentPacket.headerValue;
					break;
				case "retry":
					// FIXME : we should automatically handle this ?
			}
			if (handler != null) {
				handler.handle(currentPacket.toString());
			}
		}
	}

	public boolean isConnected() {
		return connected;
	}

	public boolean isConnecting() {
		return connecting;
	}

	private class SSEPacket {
		/* Use constants, but hope this will never change in the future (it shouldn't) */
		private final static String END_OF_PACKET = "\n\n";
		private final static String LINE_SEPARATOR = "\n";
		private final static String FIELD_SEPARATOR = ":";

		private final StringBuilder payload;
		String headerName;
		String headerValue;

		SSEPacket() {
			payload = new StringBuilder();
		}

		boolean append(Buffer buffer) {
			String response = buffer.toString();
			boolean willTerminate = response.endsWith(END_OF_PACKET);
			String[] lines = response.split(LINE_SEPARATOR);
			for (int i = 0; i < lines.length; i++) {
				final String line = lines[i];
				int idx = line.indexOf(FIELD_SEPARATOR);
				if (idx == -1) {
					continue; // ignore line
				}
				final String type = line.substring(0, idx);
				final String data = line.substring(idx + 2);
				if (i == 0 && headerName == null && !"data".equals(type)) {
					headerName = type;
					headerValue = data;
				} else {
					payload.append(data).append(LINE_SEPARATOR);
				}
			}
			return willTerminate;
		}

		@Override
		public String toString() {
			return payload == null ? "" : payload.toString();
		}
	}
}