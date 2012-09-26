package com.streamhub.tools.proxy;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.streamhub.e2e.EndToEndFramework;

@SuppressWarnings("serial")
public class ProxyControllerServlet extends HttpServlet {
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String pathInfo = request.getPathInfo();
		String action = "Path info '" + pathInfo + "' not understood. Did nothing to";
		Proxy streamingServerProxy = EndToEndFramework.getInstance().getStreamingServerProxy();
		Proxy streamingServerProxyTwo = EndToEndFramework.getInstance().getStreamingServerProxyTwo();

		if (pathInfo.contains("startTwo")) {
			if (streamingServerProxyTwo.isStopped()) {
				streamingServerProxyTwo.start();
			}
			action = "Started";
		} else if (pathInfo.contains("stopTwo")) {
			if (!streamingServerProxyTwo.isStopped()) {
				streamingServerProxyTwo.stop();
			}
			action = "Stopped";
		} else if (pathInfo.contains("start")) {
			if (streamingServerProxy.isStopped()) {
				streamingServerProxy.start();
			}
			action = "Started";
		} else if (pathInfo.contains("stop")) {
			if (!streamingServerProxy.isStopped()) {
				streamingServerProxy.stop();
			}
			action = "Stopped";
		}

		outputResponse(request, response, action);
	}

	private void outputResponse(HttpServletRequest request, HttpServletResponse response, String action) throws IOException {
		ServletOutputStream outputStream = response.getOutputStream();
		addNoCacheHeaders(response);
		response.setContentType("text/html");
		outputStream.println("<html><body>" + action + " proxy</body></html>");
		outputStream.flush();
		outputStream.close();
		request.getInputStream().close();
	}

	private void addNoCacheHeaders(HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-store, no-cache");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Expires", "Thu, 1 Jan 1970 00:00:00 GMT");
	}
}