package de.rwth_aachen.dc.ifc2lbd.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
			try {
				File configDir = new File(System.getProperty("catalina.base"), "conf");
				File configFile = new File(configDir, "bim4ren.properties");
				if(!configFile.exists())
				{
					System.err.println("bim4ren.properties dowe not exist in: <tomcat>/conf");
					return null;
				}
				InputStream stream = new FileInputStream(configFile);
				Properties props = new Properties();
				props.load(stream);
				if(props.getProperty("stardog_server")==null)
					return null;
				sg = new B4RStardogConnection.StardogContext();
				
				
				sg.stardog_server = props.getProperty("stardog_server");
				sg.stardog_user = props.getProperty("stardog_user");
				sg.stardog_password = props.getProperty("stardog_password");

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sg;
	}

	public void sendModel(Model m) {
		StardogContext sg=getContext();
		if(sg==null)
			return;
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
