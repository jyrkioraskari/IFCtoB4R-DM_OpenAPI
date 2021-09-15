package de.rwth_aachen.dc.ifc2lbd.rest;

import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;

public class FusekiConnection {

	public FusekiConnection()
	{
		RDFConnectionRemoteBuilder ds = RDFConnectionRemote.create()
	            .destination("https://lbd.arch.rwth-aachen.de:8443/fuseki/BIM4Ren/update");
		
		try (RDFConnection conn = ds.build();) {
            //conn.load("data.ttl") ;
            conn.update("PREFIX dc: <http://purl.org/dc/elements/1.1/>\r\n" + 
            "INSERT DATA\r\n" + 
            "{ \r\n" + 
            "  <http://example/book1> dc:title \"A new book\" ;\r\n" + 
            "                         dc:creator \"A.N.Other\" .\r\n" + 
            "}") ;
            //conn.put("data.n3");
            conn.commit();
            } ;
        
		
	}
	public static void main(String[] args) {
		
          new FusekiConnection();
	}

}
