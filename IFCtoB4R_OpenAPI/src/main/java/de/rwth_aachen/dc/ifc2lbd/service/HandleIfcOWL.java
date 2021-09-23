package de.rwth_aachen.dc.ifc2lbd.service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.core.Response;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import de.rwth_aachen.dc.lbd_smls.IfcOWLtoLBDConverter_BIM4Ren;

public class HandleIfcOWL {
	public Response handle(String accept_type, File tempIfcOWLFile) {
		if (accept_type.equals("application/ld+json")) {
			StringBuilder result_string = new StringBuilder();
			extract(tempIfcOWLFile, result_string, RDFFormat.JSONLD_COMPACT_PRETTY);
			return Response.ok(result_string.toString(), "application/ld+json").build();
		} else if (accept_type.equals("application/rdf+xml")) {
			StringBuilder result_string = new StringBuilder();
			extract(tempIfcOWLFile, result_string, RDFFormat.RDFXML);
			return Response.ok(result_string.toString(), "application/rdf+xml").build();
		} else {
			StringBuilder result_string = new StringBuilder();
			extract(tempIfcOWLFile, result_string, RDFFormat.TURTLE_PRETTY);
			return Response.ok(result_string.toString(), "text/turtle").build();

		}
	}


	private Model extract(File ifcOwlFile, StringBuilder result_string, RDFFormat rdfformat) {
		IfcOWLtoLBDConverter_BIM4Ren lbdconverter = new IfcOWLtoLBDConverter_BIM4Ren();
		Model m = lbdconverter.convert(ifcOwlFile.getAbsolutePath());

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
