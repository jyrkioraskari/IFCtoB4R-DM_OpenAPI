package de.rwth_aachen.dc.ifc2lbd.service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.core.Response;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import de.rwth_aachen.dc.lbd_smls.IFCtoLBDConverter_BIM4Ren;

public class HandleIFC {
	public Response handle(String accept_type, File tempIfcFile,String projectID) {
		B4RStardogConnection stardog_connection=new B4RStardogConnection();
		if(accept_type==null)
		{
			StringBuilder result_string = new StringBuilder();
			Model m=extract(tempIfcFile, result_string, RDFFormat.TURTLE_PRETTY,projectID);
			stardog_connection.sendModel(m);
			return Response.ok(result_string.toString(), "text/turtle").build();
		}
		else
		if (accept_type.equals("application/ld+json")) {
			StringBuilder result_string = new StringBuilder();
			Model m=extract(tempIfcFile, result_string, RDFFormat.JSONLD_COMPACT_PRETTY,projectID);
			stardog_connection.sendModel(m);
			return Response.ok(result_string.toString(), "application/ld+json").build();
		} else if (accept_type.equals("application/rdf+xml")) {
			StringBuilder result_string = new StringBuilder();
			Model m=extract(tempIfcFile, result_string, RDFFormat.RDFXML,projectID);
			stardog_connection.sendModel(m);
			return Response.ok(result_string.toString(), "application/rdf+xml").build();
		} else {
			StringBuilder result_string = new StringBuilder();
			Model m=extract(tempIfcFile, result_string, RDFFormat.TURTLE_PRETTY,projectID);
			stardog_connection.sendModel(m);
			return Response.ok(result_string.toString(), "text/turtle").build();

		}
	}


	
	private Model extract(File ifcFile, StringBuilder result_string, RDFFormat rdfformat,String projectID) {
		IFCtoLBDConverter_BIM4Ren lbdconverter = new IFCtoLBDConverter_BIM4Ren();
		Model m = lbdconverter.convert(ifcFile.getAbsolutePath(), "https://b4r/"+projectID+"/");

		OutputStream ttl_output = new OutputStream() {
			private StringBuilder string = new StringBuilder();

			@Override
			public void write(int b) throws IOException {
				this.string.append((char) b);
			}

			public String toString() {
				return this.string.toString();
			}
		};
		RDFDataMgr.write(ttl_output, m, rdfformat);
		result_string.append(ttl_output.toString());
		return m;
	}

}
