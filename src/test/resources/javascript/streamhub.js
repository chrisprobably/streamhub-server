/**
 * @fileOverview This file contains the main class for connecting to a StreamHub server.
 */

/**
 * @class The main class for connecting to a StreamHub Push Server and subscribing to topics.
 * 
 * @example
 * var hub = new StreamHub();
 * 
 * @constructor 
 * @description	Creates a new un-connected StreamHub
*/ 
var StreamHub = function() {
	this.pRequestQueue = [];
	this.mTopicToListener = {};
	this.sDomain = StreamHub.DomainUtils.extractDomain(document.domain);
	this.bIsResponseIFrameConnected = false;
	this.bHasRequestIFrameConnected = false;
	this.connectionMonitorId = null;
	this.oLogger = new StreamHub.NullLogger();
	this.sUid = new Date().valueOf();
	this.pConnectionListeners = [];
	this.bIsRequestInProgress = false;
};

StreamHub.POLLING_CONNECTION = "POLL";
StreamHub.STREAMING_CONNECTION = "STREAM";
StreamHub.WEB_SOCKET_CONNECTION = "WEBSOCKET";

StreamHub.prototype = {
	
	/**
	 * Connects to the server
	 * <p>
	 * The page that this call is made from must be under the same 
	 * domain that you are connecting to.  For example, assuming you make 
	 * the call from <code>www.stream-hub.com</code>, the URL must point to 
	 * a domain under <code>stream-hub.com</code>.  The most common idiom is 
	 * to serve the web content from <code>www.example.com</code> and host the 
	 * StreamHub server on <code>push.example.com</code>.
	 * 
	 * @example
	 * // Example - Connecting to a localhost server:
	 * var hub = new StreamHub();
	 * hub.connect("http://localhost:7979/");
	 * @example
	 * // Example - Serving the data from a subdomain
	 * var hub = new StreamHub();
	 * hub.connect("http://push.server.com/");
	 * @example
	 * // Example - Using a cross-domain proxy
	 * var hub = new StreamHub();
	 * hub.connect("http://www.server.com/proxy.php?request=");
	 * @example
	 * // Example - Configuring a single failover server
	 * var hub = new StreamHub();
	 * hub.connect({
	 *     serverList:         ["http://primary.example.com/",
	 *                          "http://backup.example.com/"],
	 *     failoverAlgorithm:  "priority"
	 * });
	 * @example
	 * // Example - Configuring reconnection behaviour
	 * var hub = new StreamHub();
	 * hub.connect({
	 *     serverList:                   ["http://primary.example.com/"],
	 *     initialReconnectDelayMillis:  500,
	 *     maxReconnectDelayMillis:      30000,
	 *     maxReconnectAttempts:         10,       
	 *     useExponentialBackOff:        true,
	 *     backOffMultiplier:            2   
	 * });
	 * @example
	 * // Example - All failover and configuration options
	 * var hub = new StreamHub();
	 * hub.connect({
	 *     serverList:                   ["https://push1.example.com/",
	 *                                    "https://push2.example.com/",
	 *                                    "https://push3.example.com/"],
	 *     failoverAlgorithm:            "random",
	 *     initialReconnectDelayMillis:  1000,
	 *     maxReconnectDelayMillis:      60000,
	 *     maxReconnectAttempts:         -1,       // no maximum
	 *     useExponentialBackOff:        true,
	 *     backOffMultiplier:            2   
	 * });
	 * 
	 * @param {String|Object}
	 * 					config			The URL of the server to connect to 
	 *		 							e.g. <code>http://localhost:7979/</code>
	 * 									as a string. Or an object specifying detailed 
	 * 									configuration options. Note: The trailing slash 
	 * 									is required on all URLs.
	 * 										
	 * @param {String[]}	[config.serverList] 						A list of server URLs e.g. <code>["http://primary.example.com/","http://backup.example.com/"]</code>
	 * @param {Number}		[config.initialReconnectDelayMillis=1000]   How long to wait before the first reconnect attempt (in ms)
	 * @param {Number}		[config.maxReconnectDelayMillis=-1]			The maximum amount of time we ever wait between reconnect attempts (in ms)
	 * @param {Number}		[config.maxReconnectAttempts=-1]			This is the maximum number of reconnect attempts. A value of <code>-1</code> indicates there is no maximum.
	 * @param {Boolean}		[config.useExponentialBackOff=false] 		Should an exponential backoff be used between reconnect attempts
	 * @param {Number}		[config.backOffMultiplier=1] 				The exponent used in the exponential backoff attempts
	 * @param {String}		[config.failoverAlgorithm=ordered] 			The algorithm used to pick a server from the serverList. Either "ordered" for a traversal from the current server in the list to the bottom, "random" for a random pick, or "priority" to always attempt from the top server in the list to the bottom.
	 * @param {String}		[config.connectionType=STREAM]				The connection type to prefer. Either StreamHub.POLLING_CONNECTION for polling or StreamHub.STREAMING_CONNECTION for streaming. 
	 * @param {String}		[config.staticUID] 							Use a static UID instead of a generated one. 
	 */
	connect : function(config) {
		this.sUrl = config;
		this.sConnectionType = StreamHub.STREAMING_CONNECTION;

		// TODO: Update WebSocket implementation before enabling WebSocket support
		if (false && window['WebSocket'] !== undefined && window['WebSocket'] !== null) {
			this.oConnection = new StreamHub.WebSocketConnection(this);
		} else {
			document.domain = this.sDomain;
			this.oConnection = new StreamHub.CometConnection(this);
		}
		
		if (config.serverList === undefined) {
			config = {
				serverList : [this.sUrl]
			};
			
			if (arguments.length == 2) {
				config.staticUID = arguments[1];
			}
		}
		
		if (config.serverList) {
			this.sUid = config.staticUID || this.sUid;
			this.sFailoverAlgorithm = (config.failoverAlgorithm === undefined) ? "ordered" : config.failoverAlgorithm;
			this.nReconnectDelayMillis = this.nInitialReconnectDelayMillis = (config.initialReconnectDelayMillis === undefined) ? 1000 : config.initialReconnectDelayMillis;
			this.nMaxReconnectDelayMillis = (config.maxReconnectDelayMillis === undefined) ? -1 : config.maxReconnectDelayMillis;
			this.nMaxReconnectAttempts = (config.maxReconnectAttempts === undefined) ? -1 : config.maxReconnectAttempts;
			this.bExponentialBackOff = (config.useExponentialBackOff === undefined) ? false : config.useExponentialBackOff;
			this.nBackOffMultiplier = (config.backOffMultiplier === undefined) ? 1 : config.backOffMultiplier;
			this.sConnectionType = (config.connectionType === undefined) ? StreamHub.STREAMING_CONNECTION : config.connectionType;
			this.nReconnectAttempts = 0;
			this.pServerList = config.serverList;
			this.nServerIndex = 0;
			this.sUrl = config.serverList[this.nServerIndex];
			if (this.sFailoverAlgorithm == "priority") {
				this.nServerIndex = -1;
			}	
		}
		
		// TODO: Update WebSocket implementation before enabling WebSocket support
		if (false && window['WebSocket'] !== undefined && window['WebSocket'] !== null) {
			this.sConnectionType = StreamHub.WEB_SOCKET_CONNECTION;
		}
		
		this.doConnect(this.sUrl);
	},
	
	/**
	 * Disconnects from the server
	 * 
	 * @example
	 * hub.disconnect();
	 */
	disconnect : function() {
		this.oConnection.disconnect();
	},

	/**
	 * Subscribes to a topic or multiple topics
	 * 
	 * @example
	 * // Example - subscribing to a single topic
	 * function updateListener(sTopic, oData) {
	 *     // ...
	 * }
	 * hub.subscribe("Topic", updateListener);
	 * @example
	 * // Example - subscribing to multiple topics
	 * function updateListener(sTopic, oData) {
	 *     // ...
	 * }
	 * hub.subscribe(["Topic-1", "Topic-2", "Topic-3"], updateListener);
	 * @example
	 * // Example - reading updates
	 * // Assuming the following data is published in Java code from the server:
	 * Payload payload = new JsonPayload("Topic");
	 * payload.addField("Symbol", "GOOG");
	 * payload.addField("Price", "451.13");
	 * server.publish("Topic", payload);
	 * 
	 * // The data can be retrieved in JavaScript:
	 * function updateListener(sTopic, oData) {
	 *     alert("Symbol: " + oData['Symbol'] + ", Price: " + oData['Price']);
	 *     // Prints "Symbol: GOOG, Price: 451.13"
	 * }
	 * @example
	 * // Example - enumerating all the fields received in an update
	 * function updateListener(sTopic, oData) {
	 *     var allFields = "";
	 *     for (var p in oData) {
	 *         allFields += "oData[" + p + "] = " + oData[p] + "\n";
	 *     }
	 *     alert("Update received: " + allFields);
	 * }
	 * 
	 * @param {String|String[]} oTopic A single topic as a String or multiple topics as a String array to subscribe to
	 * @param {Function} fListener A callback function to receive updates on this topic 
	 */
	subscribe : function(oTopic, fListener) {
		this.oConnection.subscribe(oTopic, fListener);
	},

	/**
	 * Unsubscribes from a topic or multiple topics
	 * 
	 * @example
	 * // Example - unsubscribing from a single topic
	 * hub.unsubscribe("Topic");
	 * @example
	 * // Example - unsubscribing from multiple topics
	 * hub.unsubscribe(["Topic-1", "Topic-2", "Topic-3"]);
	 * 
	 * @param {String|String[]} oTopic A single topic as a String or multiple topics as a String array to unsubscribe from
	 */
	unsubscribe : function(oTopic) {
		this.oConnection.unsubscribe(oTopic);
	},

	/**
	 * Publishes data on a particular topic.  The data contained in <code>sJson</code> will be 
     * sent back to the server.  The server may choose to publish this data to all 
     * subscribed clients or instead return a custom response to this client.
     * 
     * @example
     * // Example - publishing a chat message
     * hub.publish("ChatRoom", "{'nickname':'Chatty','message':'Hi room!'}");
     * @example
     * // Example - sending a trade
     * hub.publish("channels.trade", "{'symbol':'GOOG','side':'bid','price':'451.14','amount':'1000'}");
     * 
     * @param {String} sTopic	The topic to publish the data on
     * @param {String} sJson	A string of JSON containing the data to publish
	 */
	publish : function(sTopic, sJson) {
		this.oConnection.publish(sTopic, sJson);
	},
	
	/**
	 * Sets the logger for this StreamHub instance
	 * 
	 * @example
	 * var oLogger = new StreamHub.ElementLogger({elementId : "logMessages"});
	 * hub.setLogger(oLogger);
	 * 
	 * @param {Object} oLogger	The logger to set.  A logger is required to 
	 * 							have one method, called log which takes a String.
	 * @see StreamHub.ElementLogger
	 */
	setLogger : function(oLogger) {
		if (typeof oLogger.log !== "function") {
			alert("Logger must have a function called 'log'");
		} else {
			this.oLogger = oLogger;
		}
	},	
	
	/**
	 * Adds a listener to receive connection lost and established events
	 * 
	 * @example
	 * // Example - creating and adding a connection listener
	 * var connectionListener = new StreamHub.ConnectionListener();
	 * connectionListener.onConnectionEstablished = function () {
	 *     alert("Connection up");
	 * };
	 * connectionListener.onConnectionLost = function () {
	 *     alert("Connection down");
	 * };
	 * var hub = new StreamHub();
	 * hub.addConnectionListener(connectionListener);
	 * 
	 * @param {Object} oConnectionListener	The connection listener to add. It must 
	 * 										implement Streamhub.ConnectionListener.
	 * @see StreamHub.ConnectionListener
	 */
	addConnectionListener : function(oConnectionListener) {
		this.pConnectionListeners.push(oConnectionListener);
	},	
	
	/** @private */
	connectFrames : function(sUrl) {
		if (StreamHub.Browser.isIEFamily() ) {
			if (this.sConnectionType === StreamHub.STREAMING_CONNECTION) {
				this.eResponseIFrame = this.addIFrame({ id: "responseIFrame" + new Date().valueOf(), src: ""});
				this.connectResponseIFrame();
			}
			this.eRequestIFrame = this.addIFrame({ id: "requestFrame" + new Date().valueOf(), src: ""});
			this.connectRequestIFrame();
		} else {
			this.eContainerIFrame = this.addIFrame({ id: "containerIFrame" + new Date().valueOf(), src: this.sIFrameHtmlUrl });
		}
	},
	
	/** @private */
	doConnect : function(sUrl) {
		this.preConnectCleanUp();
		window.x = this.buildOnResponseData(this);
		
		if (this.sConnectionType === StreamHub.STREAMING_CONNECTION) {
			if (StreamHub.Browser.isFirefoxFamily()) {
				window.l = this.buildConnectionLost(this);
			} else {
				window.l = function() {};
			}
		} else if (this.sConnectionType === StreamHub.WEB_SOCKET_CONNECTION) {
			this.webSocketConnect(sUrl);
			return;
		}
		
		window.c = this.buildConnect(this);
		window.onunload = this.buildCleanUp(this);
		if (StreamHub.Browser.isIEFamily() || StreamHub.Browser.isFF3()) {
			window.r = this.buildOnRequestComplete(this);	
		}
		
		this.buildUrls(sUrl);
		this.connectFrames(sUrl);
		if (this.sConnectionType === StreamHub.STREAMING_CONNECTION) {
			this.startInitialConnectMonitor();
		}
	},
	
	/** @private */
	buildWebSocketOnMessage : function(oHub) {
		return function(evt) {
			try {
				oHub.oLogger.log("Web Socket Message Received: [" + evt.data + "]");
			} catch (e) {}

			var oData;
	
			try {
				eval("oData = " + evt.data);
			} catch (e) {
				oHub.oLogger.log("Error decoding message: [" + evt.data + "]");
			}
			
			if (oData.topic && typeof oHub.mTopicToListener[oData.topic] == "function") {
				oHub.mTopicToListener[oData.topic](oData.topic, oData);
			}
		};
	},
	
	/** @private */
	webSocketConnect : function(sUrl) {
		this.buildWebSocketUrls(sUrl);
		this.oLogger.log("Connecting Web Socket to [" + this.sResponseUrl + "]");
		var oHub = this;
		this.bIsWsOpen = false;
		this.ws = new WebSocket(this.sResponseUrl);
		this.ws.onopen = function() {
			oHub.bIsWsOpen = true;
			oHub.notifyConnectionEstablishedListeners();
			oHub.oConnection.onConnect();
		};
		this.ws.onmessage = this.buildWebSocketOnMessage(oHub);
		this.ws.onerror = function(error) {
			oHub.oLogger.log("Web Sockets Error: " + error);
		};
		this.ws.onclose = function() {
			oHub.bIsWsOpen = false;
			oHub.oLogger.log("Web Socket Connection Closed");
			oHub.notifyConnectionLostListeners();
		};
	},
	
	/** @private */
	logObject : function(oObject) {
		var sMsg = "";

		for (var p in oObject) {
			sMsg += "oObject." + p + " = [" + oObject[p] + "]<br />";
		}
		this.oLogger.log(sMsg);
	},
	
	/** @private */
	startInitialConnectMonitor : function () {
		var oHub = this;
		setTimeout(function() {
			if (! oHub.isResponseIFrameConnected() && oHub.bHasRequestIFrameConnected === false) {
				oHub.oLogger.log("Failing over to polling connection");
				oHub.cancelConnectionMonitor.apply(oHub);
				oHub.closeResponseChannel.apply(oHub);
				oHub.sConnectionType = StreamHub.POLLING_CONNECTION;
				oHub.startPolling.apply(oHub);
			}	
		}, 2000);
	},

	/** @private */
	buildConnect : function(oHub) {
		return function () {
			if (oHub.bConnectCalled === false) {
				oHub.bConnectCalled = true;
				var pFrames = oHub.eContainerIFrame.contentWindow.document.getElementsByTagName('frame');
				oHub.eRequestIFrame = pFrames[0];
				oHub.eResponseIFrame = pFrames[1];
				oHub.connectRequestIFrame.apply(oHub);
				if (oHub.sConnectionType === StreamHub.STREAMING_CONNECTION) {
					oHub.connectResponseIFrame.apply(oHub);
				}
			}
		};
	},
	
	/** @private */
	addSubscriptionListeners : function(oTopic, fListener) {
		if (oTopic instanceof Array) {
			for (var i = 0; i < oTopic.length; i++) {
				this.addTopicListener(oTopic[i], fListener);
			}
		} else {
			this.addTopicListener(oTopic, fListener);
		}
	},
	
	/** @private */
	buildSubscriptionList : function(oTopic) {
		var sSubscriptionTopic = oTopic;
		
		if (oTopic instanceof Array) {
			sSubscriptionTopic = "";
			
			for (var i = 0; i < oTopic.length; i++) {
				var sTopic = oTopic[i];
				sSubscriptionTopic += sTopic;
				
				if (i != (oTopic.length - 1)) {
					sSubscriptionTopic += ","; 
				}
			}
		}
		
		return sSubscriptionTopic;
	},
		
	/** @private */
	connectRequestIFrame : function() {
		this.oLogger.log("Connecting request iFrame to " + this.sRequestUrl);
		this.request(this.sRequestUrl, "Connect", this.buildConnectionResponse(this));
	},

	/** @private */
	connectResponseIFrame : function() {
		this.oLogger.log("Connecting response iFrame to " + this.sResponseUrl);
		if (StreamHub.Browser.isIEFamily()) {
			this.eResponseIFrame.src = this.sResponseUrl;
		} else {
			if (this.eContainerIFrame.contentWindow.connect === undefined) {
				this.oLogger.log("Could not connect to response iframe");
				this.reconnect();
				return;
			}
			
			this.eContainerIFrame.contentWindow.connect(this.sResponseUrl);
		}
		this.startConnectionMonitor();
	},

	/** @private */
	setOnLoad : function(eElement, fHandler) {
		if (eElement.addEventListener) {
			eElement.onload = fHandler;
		} else if (eElement.attachEvent) {
			eElement.onload = null;
			eElement.attachEvent('onload', fHandler);
		}
	},

	/** @private */
	addTopicListener : function(sTopic, fListener) {
		this.mTopicToListener[sTopic] = fListener;
	},
	
	/** @private */
	buildPollingHandler : function(oHub) {
		return function(sTopic, sResponse) {
			try {
				var pMessages = eval("(" + sResponse + ")");
				oHub.oLogger.log("Polling response is : " + sResponse + " for topic '" + sTopic + "'. Num messages: " + pMessages.length);
				for (var i = 0; i < pMessages.length; i++) {
					x(pMessages[i]);
				}
			} catch (e) {
				oHub.oLogger.log("Problem parsing poll response: " + e);
			}
		};
	},

	/** @private */
	buildSubscriptionResponse : function(oHub) {
		return function(sTopic, sResponse) {
			oHub.oLogger.log("Subscription response is : " + sResponse + " for topic '" + sTopic + "'");
		};
	},
	
	/** @private */
	buildUnSubscribeResponse : function(oHub) {
		return function(sTopic, sResponse) {
			oHub.oLogger.log("Unsubscribe response is : " + sResponse + " for topic '" + sTopic + "'");
		};
	},

	/** @private */
	buildPublishResponse : function(oHub) {
		return function(sTopic, sResponse) {
			oHub.oLogger.log("Publish response is : " + sResponse + " for topic '" + sTopic + "'");
		};
	},
	
	/** @private */
	buildConnectionResponse : function(oHub) {
		return function(sTopic, sResponse) {
			oHub.bHasRequestIFrameConnected = true;
			oHub.oLogger.log("Connection response is : " + sResponse);
			if (oHub.sConnectionType === StreamHub.POLLING_CONNECTION) {
				oHub.startPolling.apply(oHub);
			}
		};
	},
	
	/** @private */
	buildDisconnectionResponse : function(oHub) {
		return function(sTopic, sResponse) {
			oHub.oLogger.log("Disconnection response is : " + sResponse);
		};
	},
	
	/** @private */
	reconnect : function() {
		if (this.isDeliberateDisconnect !== true  && ! this.isResponseIFrameConnected() && (this.nMaxReconnectAttempts == -1 || this.nReconnectAttempts < this.nMaxReconnectAttempts)) {
			try {
				var oHub = this;
				clearTimeout(this.reconnectorId);
				
				if (this.bExponentialBackOff && this.nReconnectAttempts > 0) {
					this.nReconnectDelayMillis *= this.nBackOffMultiplier;
					
					if (this.nMaxReconnectDelayMillis != -1 && this.nReconnectDelayMillis > this.nMaxReconnectDelayMillis) {
						this.nReconnectDelayMillis = this.nMaxReconnectDelayMillis;
					}
				}
				
				this.oLogger.log("Attempting reconnect in " + this.nReconnectDelayMillis + "ms");
				
				this.reconnectorId = setTimeout(function() {
					if ((oHub.nMaxReconnectAttempts == -1 || oHub.nReconnectAttempts < oHub.nMaxReconnectAttempts) && ! oHub.isResponseIFrameConnected.apply(oHub)) {
						if (oHub.sFailoverAlgorithm == "priority") {
							oHub.nServerIndex = (oHub.nServerIndex + 1) % oHub.pServerList.length;
						}
						
						oHub.doReconnection.apply(oHub);
					}
				}, this.nReconnectDelayMillis);
			} catch (e) {
				this.oLogger.log("Error setting interval: " + e);
			}
		}
	},

	/** @private */
	doReconnection : function() {
		var sNextServerUrl = this.sUrl;
		
		if (this.nServerIndex !== undefined) {
			var nMaxLen = this.pServerList.length;
			
			if (this.sFailoverAlgorithm == "ordered") { 
				this.nServerIndex = (this.nServerIndex + 1)  % nMaxLen;
			} else if (this.sFailoverAlgorithm == "random") {
				this.nServerIndex = Math.floor(Math.random() * nMaxLen);
			}

			sNextServerUrl = this.pServerList[this.nServerIndex];
		}
		
		this.nReconnectAttempts++;
		this.oLogger.log("Reconnecting... Trying " + sNextServerUrl);
		this.doConnect(sNextServerUrl);
		this.reconnect();
	},
	
	/** @private */
	onReconnect : function() {
		try {
			clearTimeout(this.reconnectorId);
		} catch (e) {}

		if (this.sFailoverAlgorithm == "priority") {
			this.nServerIndex = -1;
		}
		
		this.nReconnectAttempts = 0;
		this.nReconnectDelayMillis = this.nInitialReconnectDelayMillis;
		this.notifyConnectionEstablishedListeners();
	},

	/** @private */
	buildConnectionLost : function(oHub) {
		return function() {
			oHub.oLogger.log("Lost connection to server");
			oHub.notifyConnectionLostListeners.apply(oHub);
			oHub.bIsResponseIFrameConnected = false;
			oHub.reconnect.apply(oHub);
		};
	},
	
	/** @private */
	notifyConnectionLostListeners : function() {
		for (var i = 0; i < this.pConnectionListeners.length; i++) {
			this.pConnectionListeners[i].onConnectionLost();	
		}
	},

	/** @private */
	notifyConnectionEstablishedListeners : function() {
		for (var i = 0; i < this.pConnectionListeners.length; i++) {
			this.pConnectionListeners[i].onConnectionEstablished();	
		}
	},

	/** @private */
	imageRequest : function(sUrl) {
		var eImg = document.createElement("img");
		eImg.src = sUrl;
		document.body.appendChild(eImg);
	},

	/** @private */
	request : function(sUrl, sTopic, fCallback) {
		this.pRequestQueue.push({ url: sUrl, callback: fCallback, topic: sTopic });

		if (this.isResponseIFrameConnected()) {
			this.processRequestQueue();
		}
	},

	/** @private */
	processRequestQueue : function() {
		if (this.pRequestQueue.length > 0 && ! this.bIsRequestInProgress) {
			this.bIsRequestInProgress = true;
			
			if (! StreamHub.Browser.isIEFamily() && ! StreamHub.Browser.isFF3()) {
				this.setOnLoad(this.eRequestIFrame, this.buildOnRequestComplete(this));
			}

			var sRequestUrl = this.pRequestQueue[0].url;
			this.oLogger.log("Requesting: " + sRequestUrl);

			if (StreamHub.Browser.isIEFamily()) {
				this.eRequestIFrame.src = sRequestUrl;
			} else {
				try {
					this.eContainerIFrame.contentWindow.request(sRequestUrl, this);
				} catch (e) {
					this.oLogger.log("Failed requesting: " + sRequestUrl + ". Error was: " + e + ". Trying again in 1 second.");
					this.bIsRequestInProgress = false;
					var oHub = this;
					setTimeout(function() {
						oHub.processRequestQueue.apply(oHub);
					}, 1000);
				}
			}
		}	
	},

	/** @private */
	addIFrame : function(config) {
		var oDocument = document;
		
		if (StreamHub.Browser.isIEFamily()) {
			this.createHtmlFile();
			oDocument = this.transferDoc;
		}
		
		var eIFrame = this.createIFrame(oDocument, config);
		oDocument.body.appendChild(eIFrame);
		return eIFrame;
	},
	
	/** @private */
	createElement : function(oDocument, sElement, sId) {
		var eElement = oDocument.createElement(sElement);
		eElement.id = sId;
		return eElement;
	},
	
	/** @private */
	createIFrame : function(oDocument, config) {
		var eIFrame = this.createElement(oDocument, "IFRAME", config.id);
		eIFrame.style.visibility = "hidden";
		eIFrame.style.display = "none";
		eIFrame.style.height = "0px";
		eIFrame.style.width = "0px";
		eIFrame.src = config.src;
		if (config.onLoadFn !== undefined) {
			this.setOnLoad(eIFrame, config.onLoadFn);
		}
		return eIFrame;		
	},
	
	/** @private */
	createHtmlFile : function() {
		if (this.transferDoc === undefined) {
			this.transferDoc = new ActiveXObject("htmlfile");
			this.transferDoc.open();
			this.transferDoc.write("<html>");
			this.transferDoc.write("<script>document.domain='"+this.sDomain+"';</script>");
			this.transferDoc.write("</html>");
			this.transferDoc.close();
			this.transferDoc.parentWindow.x = x;
			this.transferDoc.parentWindow.r = this.buildOnRequestComplete(this);
		}
	},

	/** @private */
	startPolling : function() {
		this.oLogger.log("Starting polling...");
		this.stopPolling();
		var oHub = this;
		this.oPollingId = setInterval(function() {
			oHub.request(oHub.sPollingUrl, "poll", oHub.buildPollingHandler(oHub));
		}, 1000);
	},
	
	/** @private */
	stopPolling : function() {
		if (this.oPollingId !== undefined) {
			clearInterval(this.oPollingId);
		}
	},
	
	/** @private */
	startConnectionMonitor : function() {
		if (StreamHub.Browser.isWebkitFamily()) {
			this.setOnLoad(this.eResponseIFrame, this.buildConnectionLost(this));
		} else {
			this.startDefaultConnectionMonitor(this);
		}
	},
	
	/** @private */
	startDefaultConnectionMonitor : function(oHub) {		
		if (oHub.connectionMonitorId == null) {
			oHub.connectionMonitorId = setInterval(function() {
				var readyState = oHub.getIFrameReadyState(oHub.eResponseIFrame);

				if (readyState == "complete") {
					clearInterval(oHub.connectionMonitorId);
					oHub.connectionMonitorId = null;
					(oHub.buildConnectionLost(oHub))();
				}
			}, 1000);
		}
	},
	
	/** @private */
	closeResponseChannel : function() {
		this.oLogger.log("Closing response channel");
		this.eResponseIFrame.src = this.sCloseResponseUrl;
	},
	
	/** @private */
	cancelConnectionMonitor : function() {
		if (StreamHub.Browser.isWebkitFamily()) {
			this.setOnLoad(this.eResponseIFrame, null);
		} else {
			clearInterval(this.connectionMonitorId);
			this.connectionMonitorId = null;
		}
	},
	
	/** @private */
	getIFrameReadyState : function(eIFrame) {
		if (StreamHub.Browser.isIEFamily()) {
			return eIFrame.readyState;
		}
		
		try {
			return eIFrame.readyState || eIFrame.contentWindow.document.readyState;
		} catch (e) {
			return "complete";		
		}
	},
	
	/** @private */
	isResponseIFrameConnected : function() {
		return this.bIsResponseIFrameConnected || this.sConnectionType === StreamHub.POLLING_CONNECTION;
	},

	/** @private */
	buildOnResponseData : function(oHub) {
		return function(oData) {
			try {
				oHub.oLogger.log("onResponseData via response iFrame : [" + oData + "]"); 
			} catch (e) {}
			
			if (oData.topic && typeof oHub.mTopicToListener[oData.topic] == "function") {
				oHub.mTopicToListener[oData.topic](oData.topic, oData);
			} else if (oHub.isResponseIFrameConnectionOk(oData)) {
				oHub.bIsResponseIFrameConnected = true;
				oHub.eBlankIFrame = oHub.addIFrame({ id: "blankIFrame" + new Date().valueOf(), src: "about:blank"});
				oHub.onReconnect.apply(oHub);
				oHub.processRequestQueue();
			}
		};
	},

	/** @private */
	isResponseIFrameConnectionOk : function(oData) {
		return oData.toString().indexOf("response OK") > -1;
	},

	/** @private */
	buildUrls : function(sUrl) {
		var sRand = new Date().valueOf();
		this.sIFrameHtmlUrl = sUrl + "iframe.html?uid=" + this.sUid + "&domain=" + this.sDomain + "&r=" + sRand; 
		this.sRequestUrl = sUrl + "request/?uid=" + this.sUid + "&domain=" + this.sDomain + "&r=" + sRand; 
		this.sResponseUrl = sUrl + "response/?uid=" + this.sUid + "&domain=" + this.sDomain + "&r=" + sRand;
		this.sPublishUrl = sUrl + "publish/?uid=" + this.sUid + "&domain=" + this.sDomain + "&r=" + sRand;
		this.sSubscribeUrl = sUrl + "subscribe/?uid=" + this.sUid + "&domain=" + this.sDomain + "&r=" + sRand;
		this.sUnSubscribeUrl = sUrl + "unsubscribe/?uid=" + this.sUid + "&domain=" + this.sDomain + "&r=" + sRand;
		this.sDisconnectUrl = sUrl + "disconnect/?uid=" + this.sUid + "&domain=" + this.sDomain + "&r=" + sRand;
		this.sCloseResponseUrl = sUrl + "closeresponse/?uid=" + this.sUid + "&domain=" + this.sDomain + "&r=" + sRand;
		this.sPollingUrl = sUrl + "poll/?uid=" + this.sUid + "&domain=" + this.sDomain + "&r=" + sRand;
	},

	/** @private */
	buildWebSocketUrls : function(sUrl) {
		var sRand = new Date().valueOf();
		sUrl = sUrl.replace(/^http(s)?:\/\//, "ws://");
		this.oLogger.log("sUrl = [" + sUrl + "]");
		if (sUrl.lastIndexOf("/") === sUrl.length - 1) {		
			sUrl = sUrl.substring(0, sUrl.length - 1);
			this.sResponseUrl = sUrl + "ws/";
		} else {
			this.sResponseUrl = sUrl + "ws/";
		}
	},
	
	/** @private */
	buildOnRequestComplete : function(oHub) {
		return function() {
			var oRequest = oHub.pRequestQueue[0];
			var sResponse = null;
			
			try {
				sResponse = oHub.eRequestIFrame.contentWindow.document.body.innerHTML;
			} catch(e) {
				oHub.oLogger.log("buildOnRequestComplete(): Error getting iFrame contents for url [" + oRequest.url + "]: " + e);
			}
			
			if (oRequest !== undefined && sResponse !== null) {
				oRequest.callback(oRequest.topic, sResponse);
				oHub.pRequestQueue.shift();
			}
			
			oHub.bIsRequestInProgress = false;
			oHub.processRequestQueue();
		};
	},

	/** @private */	
	preConnectCleanUp : function() {
		(this.buildCleanUp(this))();
	},
	
	/** @private */
	removeIFrame : function(eFrame) {
		if (eFrame !== undefined && eFrame !== null) {
			eFrame.src = "";
			if (eFrame.contentWindow !== undefined && eFrame.contentWindow !== null) {
				eFrame.contentWindow.document.close();
			}
			if (eFrame.parentNode !== undefined && eFrame.parentNode !== null) {
				var u = eFrame.parentNode.removeChild(eFrame);
			}
			eFrame = null;
		}
	},

	/** @private */	
	buildCleanUp : function(oHub) {
		return function() {
			try {
			    if (oHub.isDeliberateDisconnect === false) {
			    	oHub.imageRequest(oHub.sDisconnectUrl);	
			    }
			    
				oHub.bConnectCalled = false;
				
				if (oHub.pRequestQueue !== undefined && oHub.pRequestQueue.length > 0) {
					oHub.pRequestQueue = [];	
				}
				
				oHub.removeIFrame(oHub.eRequestIFrame);
				oHub.removeIFrame(oHub.eResponseIFrame);
				oHub.removeIFrame(oHub.eContainerIFrame);
				oHub.removeIFrame(oHub.eBlankIFrame);
				
				if (oHub.tranferDoc !== undefined) {
					oHub.transferDoc = null;
				}
				
				if (window.x) {
					window.x = null;
				}
				
				if (window.r) {
					window.r = null;
				}
				
				if (window.CollectGarbage) {
					CollectGarbage();
				}
			} catch (e) {
				try {
					oHub.oLogger.log("Error during cleanup '" + e + "'");
				} catch (doNothing) {}
			}
		}
	}
};

/**
 * @class This interface should be implemented by classes wanting to receive 
 * notifications when the connection to the StreamHub server is lost or established.
 * 
 * @example
 * // Example - implementing a connection listener
 * var connectionListener = new StreamHub.ConnectionListener();
 * connectionListener.onConnectionEstablished = function () {
 *     alert("Connection up");
 * };
 * connectionListener.onConnectionLost = function () {
 *     alert("Connection down");
 * };
 * 
 * @constructor 
 * @description	Creates a new ConnectionListener
*/ 
StreamHub.ConnectionListener = function() {};

StreamHub.ConnectionListener.prototype = {
	/**
	 * This method is called everytime a connection is established with the StreamHub server
	 */
	onConnectionEstablished : function () {},
	
	/**
	 * This method is called everytime the connection to the StreamHub server is lost
	 */
	onConnectionLost : function () {}	
};

/** @private */
StreamHub.Browser = {
	isWebkitFamily : function() {
		return navigator.userAgent.indexOf("WebKit/") > -1;
	},
	isIEFamily : function() {
		return navigator.userAgent.indexOf("MSIE") > -1;
	},
	isFirefoxFamily : function() {
		return navigator.userAgent.indexOf("Firefox/") > -1;
	},
	isFF3 : function() {
		return navigator.userAgent.indexOf("Firefox/3") > -1;
	},
	isSafari : function() {
		return navigator.userAgent.indexOf("Safari/") > -1 && navigator.userAgent.indexOf("Chrome/") < 0;
	},
	isChrome : function() {
		return navigator.userAgent.indexOf("Chrome/") > -1;
	},
	isIE6 : function() {
		return navigator.userAgent.indexOf("MSIE 6") > -1;
	},
	isIE7 : function() {
		return navigator.userAgent.indexOf("MSIE 7") > -1;
	},
	isIE8 : function() {
		return navigator.userAgent.indexOf("MSIE 8") > -1;
	}
};

/**
 * @class A logger which can log to any HTML element
 * 
 * @example
 * var logger = new StreamHub.ElementLogger({elementId : 'logMessagesDiv'});
 * 
 * 
 * @param config			Configuration options
 * @param config.elementId	The ID of the element to append log messages to
 * @constructor
 * @description	Creates a new ElementLogger
 */
StreamHub.ElementLogger = function(config) {
	this.oDoc = document;
	this.eLogContainer = this.oDoc.getElementById(config.elementId);
	
};
	
StreamHub.ElementLogger.prototype = {
	/**
	 * Logs a message
	 * 
	 * @example
	 * logger.log("A log message");
	 * 
	 * @param {String} message The message to log  
	 */
	log : function(message) {
		var eLogMessageDiv = this.oDoc.createElement("DIV");
		eLogMessageDiv.innerHTML = message;
		this.eLogContainer.appendChild(eLogMessageDiv);
	}
};

/** @private */
StreamHub.NullLogger = function() {};
StreamHub.NullLogger.prototype = {
	log : function() {}
};

/** @private */
StreamHub.CountLogger = function(config) {
	this.nCount = 0;
	this.eLogContainer = document.getElementById(config.elementId);
};

StreamHub.CountLogger.prototype =  {
	log : function(message) {
		this.eLogContainer.innerHTML = (++this.nCount) + " messages logged";
	}
};

/** @private */
StreamHub.WebSocketConnection = function(oHub) {
	this.oHub = oHub;
	this.pMessageQueue = [];
};

StreamHub.WebSocketConnection.prototype = {
	disconnect : function() {
		this.oHub.oLogger.log("Disconnecting");
		this.oHub.isDeliberateDisconnect = true;
		this.send("disconnect");
	},

	subscribe : function(oTopic, fListener) {
		this.oHub.addSubscriptionListeners(oTopic, fListener);
		var sSubscriptionTopic = this.oHub.buildSubscriptionList(oTopic);
		this.send("subscribe=" + sSubscriptionTopic);
	},

	unsubscribe : function(oTopic) {
		var sTopic = this.oHub.buildSubscriptionList(oTopic);
		this.send("unsubscribe=" + sTopic);
	},

	publish : function(sTopic, sJson) {
		var sPublishMessage = "publish(" + sTopic + "," + sJson + ")";
		this.send(sPublishMessage);
	},
	
	send : function(sMessage) {
		if (this.oHub.bIsWsOpen === true) {
			this.oHub.oLogger.log("Sending WebSocket message: [" + sMessage + "]");
			this.oHub.ws.send(sMessage);
		} else {
			this.oHub.oLogger.log("Queueing WebSocket message: [" + sMessage + "]");
			this.pMessageQueue.push(sMessage);
		}
	},
	
	onConnect : function() {
		this.oHub.oLogger.log("Web Socket connected");
		this.send("uid=" + this.oHub.sUid);
		this.sendQueuedMessages();
	},
	
	sendQueuedMessages : function () {
		for (var i = 0; i < this.pMessageQueue.length; i++) {
			this.send(this.pMessageQueue[i]);
		}
	}
};

/** @private */
StreamHub.CometConnection = function(oHub) {
	this.oHub = oHub;
};

StreamHub.CometConnection.prototype = {
	disconnect : function() {
		this.oHub.oLogger.log("Disconnecting");
		this.oHub.stopPolling.call(this.oHub);
		this.oHub.request(this.oHub.sDisconnectUrl, "disconnect", this.oHub.buildDisconnectionResponse(this.oHub));
		this.oHub.isDeliberateDisconnect = true;
	},

	subscribe : function(oTopic, fListener) {
		this.oHub.addSubscriptionListeners(oTopic, fListener);
		var sSubscriptionTopic = this.oHub.buildSubscriptionList(oTopic);
		var sSubscribeUrl = this.oHub.sSubscribeUrl + "&topic=" + sSubscriptionTopic;
		this.oHub.request(sSubscribeUrl, sSubscriptionTopic, this.oHub.buildSubscriptionResponse(this.oHub));
	},

	unsubscribe : function(oTopic) {
		var sTopic = this.oHub.buildSubscriptionList(oTopic);
		var sUnSubscribeUrl = this.oHub.sUnSubscribeUrl + "&topic=" + sTopic;
		this.oHub.request(sUnSubscribeUrl, sTopic, this.oHub.buildUnSubscribeResponse(this.oHub));
	},

	publish : function(sTopic, sJson) {
		var sPublishUrl = this.oHub.sPublishUrl + "&topic=" + sTopic + "&payload=" + encodeURIComponent(sJson);
		this.oHub.oLogger.log("Publishing to " + sPublishUrl);
		this.oHub.request(sPublishUrl, sTopic, this.oHub.buildPublishResponse(this.oHub));
	}
};

/** @private */
StreamHub.DomainUtils = {
	pTLDs : ['com', 'net', 'org', 'gov', 'co'],
	pNumbers : ['0','1','2','3','4','5','6','7','8','9'],
	
	extractDomain : function(sDomainString) {
		var lastCharacter = sDomainString.charAt(sDomainString.length-1);
		if (StreamHub.DomainUtils.isNumeric(lastCharacter)) {
			return sDomainString;
		}
		
		var pArray = sDomainString.split('.');
		var nSpliceIndex = pArray.length - 2;
		
		if (StreamHub.DomainUtils.isTLD(pArray[nSpliceIndex]) && pArray.length > 2) {
			nSpliceIndex = pArray.length - 3;
		}
		
		var nItemsLeft = pArray.length - nSpliceIndex;
		return pArray.splice(nSpliceIndex, nItemsLeft).join('.');
	},
	
	isNumeric : function(sChar) {
		for (var i = 0; i < StreamHub.DomainUtils.pNumbers.length; i++) {
			if (sChar === StreamHub.DomainUtils.pNumbers[i]) {
				return true;
			}		
		}
		
		return false;
	},
	
	isTLD : function(sString) {
		for (var i = 0; i < StreamHub.DomainUtils.pTLDs.length; i++) {
			if (sString === StreamHub.DomainUtils.pTLDs[i]) {
				return true;
			}		
		}	
		
		return false;		
	}
};
