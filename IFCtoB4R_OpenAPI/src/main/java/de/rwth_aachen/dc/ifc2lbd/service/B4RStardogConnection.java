package de.rwth_aachen.dc.ifc2lbd.service;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.jena.rdf.model.Model;

import com.complexible.stardog.api.Connection;
import com.complexible.stardog.api.ConnectionConfiguration;
import com.complexible.stardog.jena.SDJenaFactory;

public class B4RStardogConnection {
	static class StardogContext {
		String stardog_server;
		String stardog_user;
		String stardog_password;
	}

	static private StardogContext sg = null;

	static StardogContext getContext() {
		if (sg == null) {
			sg = new B4RStardogConnection.StardogContext();
			InitialContext context;
			try {
				context = new InitialContext();
				Context xmlNode = (Context) context.lookup("java:comp/env");
				sg.stardog_server = (String) xmlNode.lookup("stardog_server");
				sg.stardog_user = (String) xmlNode.lookup("stardog_user");
				sg.stardog_password = (String) xmlNode.lookup("stardog_password");
			} catch (NamingException e) {
				e.printStackTrace();
			}
		}
		return sg;
	}

	public void sendModel(Model m) {
		StardogContext sg=getContext();
		Connection aConn = ConnectionConfiguration.to("b4r") // the name of the db to connect to
				.server(sg.stardog_server).credentials(sg.stardog_user,sg.stardog_password) // credentials to use while
																							// connecting
				.connect();
		aConn.begin();
		Model b4r_model = SDJenaFactory.createModelWithNamespaces(aConn);
		b4r_model.add(m);
		aConn.commit();
	}

}
