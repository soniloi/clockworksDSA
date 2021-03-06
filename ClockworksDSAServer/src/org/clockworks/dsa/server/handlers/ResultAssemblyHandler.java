package org.clockworks.dsa.server.handlers;

import java.io.IOException;
import java.util.List;

import org.clockworks.dsa.server.environment.Environment;
import org.clockworks.dsa.server.singletons.EnvironmentList;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Handler listening for pings from user (WOPPings)
 * Sends results if ready, responds with a not-ready message otherwise
 *
 */
public class ResultAssemblyHandler implements HttpHandler {

	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
		int statusCode = 0;
		String responseBody = "";
		Headers headers = httpExchange.getRequestHeaders();
		List<String> environmentIdList = headers.get("Environment-Id");
		try {
		    	
		    	// Handle case where the HttpExchange does not contain an environmentId header
			if(environmentIdList==null) {
				System.out.println("400: Missing required headers");
				statusCode = 400;
				responseBody = "Missing required headers.";
			}
			else {
				
				int environmentId = Integer.parseInt(environmentIdList.get(0));
				
				// A valid WOP Ping is a GET request
				if(httpExchange.getRequestMethod().equalsIgnoreCase("GET")) {
					
				    	Environment environment = EnvironmentList.sharedInstance().getEnvironmentById(environmentId);
					
				    	// An unknown environment ID, perhaps a duplicate (already returned) or corrupted WOP Ping
					if(environment==null) {
						System.out.println("404: Environment not found");
						statusCode = 404;
						responseBody = "Environment not found";
					}
					
					// Otherwise return results, if ready
					else {
						String results = environment.returnAssembledResult();
			
						if(results==null) {
							System.out.println("102: Process not ready");
							statusCode = 102;
							responseBody = "Process not ready";
						}
						else {
							statusCode = 200;
							responseBody = results;
							EnvironmentList.sharedInstance().deleteEnvironment(environment);
						}
					}
				}
				
				// Request is something other than a GET and thus invalid
				else {
					System.out.println("405: Method not allowed");
					statusCode = 405;
					responseBody = "Method not allowed";
				}
			}
		}
		catch(Exception e){
			statusCode = 500;
		}
		
		// Return version to send if there is no body (results not ready)
		if(statusCode==102){
			httpExchange.sendResponseHeaders(statusCode, -1);
			httpExchange.close();
		}
		
		// Return version to send if there is a body (results ready)
		else{
			httpExchange.sendResponseHeaders(statusCode, responseBody.length());
			httpExchange.getResponseBody().write(responseBody.getBytes());
			httpExchange.close();
		}
		
	}

}
