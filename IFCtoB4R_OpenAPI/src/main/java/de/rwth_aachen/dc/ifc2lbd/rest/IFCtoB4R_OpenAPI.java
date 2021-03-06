 package de.rwth_aachen.dc.ifc2lbd.rest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.google.gson.Gson;

import de.rwth_aachen.dc.lbd_smls.IFCtoLBDConverter_BIM4Ren;
import de.rwth_aachen.dc.lbd_smls.IfcOWLtoLBDConverter_BIM4Ren;

/*
 * Jyrki Oraskari, 2020, 2021
 */

@Path("/")
public class IFCtoB4R_OpenAPI {



	@GET
	@Path("/services")
	@Produces(MediaType.APPLICATION_JSON)
	public Response services_list(@Context UriInfo uriInfo) {
		List<Service> services = new ArrayList<>();
		System.out.println(uriInfo.getBaseUri().getHost());
	    System.out.println(uriInfo.getBaseUri().getPort());
		services.add(new Service("IFCtoB4R DM", "IFCtoB4R DM", "BIM4REN", null, "http://"+uriInfo.getBaseUri().getHost()+"/IFCtoB4R_OpenAPI/api/convert_IFC-B4R", null, null));
	    String json = new Gson().toJson(services);
		return Response.ok(json, "application/json").build();

	}
	

	/**
	 * IFC to B4R-DM for BIM4Ren Converts an IFC file into into the Linked Building
	 * Data RDF (BOT+SMLS)
	 * 
	 * 
	 * @param ifcFile an IFC STEP file formatted file. The format is specified in ISO 10303-21:2016.
	 * @return Returnd RDF output. Formats are: JSON-LD, RDF/XML, and TTL
	 */

	@POST
	@Path("/convert_IFC-B4R")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({ "text/turtle", "application/ld+json", "application/rdf+xml" })
	public Response convertIFCtoB4R(@HeaderParam(HttpHeaders.ACCEPT) String accept_type,
			@FormDataParam("ifcFile") InputStream ifcFile) {
		try {
			File tempIfcFile = File.createTempFile("ifc2lbd-", ".ifc");
			tempIfcFile.deleteOnExit();

			Files.copy(ifcFile, tempIfcFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			ifcFile.close();
			return handle_ifc(accept_type, tempIfcFile);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.noContent().build();
	}
	
	/**
	 * IFC to B4R-DM for BIM4Ren Converts an IFC file into into the Linked Building
	 * Data RDF (BOT+SMLS)
	 * 
	 * https://bim4ren.eu/
	 * 
	 * + Geometry: Bounding Boxes
	 * 
	 * IFC STEP file as input specified in BIM4Ren D2.2. The content is a IFC STEP file formatted file. 
	 * The format is specified in ISO 10303-21:2016. All commonly used IFC versions are supported.
	 * 
	 * @return Returnd RDF output. Formats are: JSON-LD, RDF/XML, and TTL
	 */
	@POST
	@Path("/convert_IFC-B4R")
	@Consumes({ MediaType.TEXT_PLAIN, "application/ifc" })
	@Produces({ "text/turtle", "application/ld+json", "application/rdf+xml" })
	public Response convertIFCtoB4Rn(@HeaderParam(HttpHeaders.ACCEPT) String accept_type, String ifc_step_content) {
		try {
			File tempIfcFile = File.createTempFile("ifc2lbd-", ".ifc");
			FileWriter tmpWriter = new FileWriter(tempIfcFile);
			tmpWriter.write(ifc_step_content);
			tmpWriter.close();
			tempIfcFile.deleteOnExit();

			return handle_ifc(accept_type, tempIfcFile);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.noContent().build();
	}

	/**
	 * IfcOWLtoLBD for BIM4Ren Converts an IfcOWL file into into the Linked Building
	 * Data RDF that uses the B4R-DM ontology
	 * 
	 * https://bim4ren.eu/
	 * 
	 * MultiPart Form data input
	 * 
	 * @param ifcOWLFile an ifcOWL Abox Turtle formatted file as form parameter. If the file does not confirm with the ifcOWL specification, an empty output is given.
 
	 * @return Returnd RDF output. Formats are: JSON-LD, RDF/XML, and TTL
	 */
	@POST
	@Path("/convert_ifcOWL-B4R")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({ "text/turtle", "application/ld+json", "application/rdf+xml" })
	public Response convertIfcOWLtoB4R(@HeaderParam(HttpHeaders.ACCEPT) String accept_type,
			@FormDataParam("ifcOWLFile") InputStream ifcOWLFile) {
		try {
			File tempIfcOWLFile = File.createTempFile("ifc2lbd-", ".ttl");
			tempIfcOWLFile.deleteOnExit();

			Files.copy(ifcOWLFile, tempIfcOWLFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			ifcOWLFile.close();

			return handle_ifcowl(accept_type, tempIfcOWLFile);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.noContent().build();
	}
	
	/**
	 * IfcOWLtoLBD for BIM4Ren Converts an IfcOWL file into into the Linked Building
	 * Data RDF that uses the B4R-DM ontology
	 * 
	 * https://bim4ren.eu/
	 * 
	 * ifcOWL Abox Turtle formatted file as input. If the file does not confirm with the ifcOWL specification, an empty output is given.
	 * 
	 * @return Returnd RDF output. Formats are: JSON-LD, RDF/XML, and TTL
	 */
	@POST
	@Path("/convert_ifcOWL-B4R")
	@Consumes({ MediaType.TEXT_PLAIN, "text/turtle"})
	@Produces({ "text/turtle", "application/ld+json", "application/rdf+xml" })
	public Response convertIfcOWLtoB4R(@HeaderParam(HttpHeaders.ACCEPT) String accept_type, String ifc_step_content) {
		try {
			File tempIfcOWLFile = File.createTempFile("ifc2lbd-", ".ttl");
			FileWriter tmpWriter = new FileWriter(tempIfcOWLFile);
			tmpWriter.write(ifc_step_content);
			tmpWriter.close();
			tempIfcOWLFile.deleteOnExit();

			return handle_ifcowl(accept_type, tempIfcOWLFile);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.noContent().build();
	}



	private Response handle_ifc(String accept_type, File tempIfcFile) {
		if (accept_type.equals("application/ld+json")) {
			StringBuilder result_string = new StringBuilder();
			extractIFCtoB4R(tempIfcFile, result_string, RDFFormat.JSONLD_COMPACT_PRETTY);
			System.out.println(result_string.toString());
			return Response.ok(result_string.toString(), "application/ld+json").build();
		} else if (accept_type.equals("application/rdf+xml")) {
			StringBuilder result_string = new StringBuilder();
			extractIFCtoB4R(tempIfcFile, result_string, RDFFormat.RDFXML);
			System.out.println(result_string.toString());
			return Response.ok(result_string.toString(), "application/rdf+xml").build();
		} else {
			StringBuilder result_string = new StringBuilder();
			extractIFCtoB4R(tempIfcFile, result_string, RDFFormat.TURTLE_PRETTY);
			System.out.println(result_string.toString());
			return Response.ok(result_string.toString(), "text/turtle").build();

		}
	}


	
	private void extractIFCtoB4R(File ifcFile, StringBuilder result_string, RDFFormat rdfformat) {
		IFCtoLBDConverter_BIM4Ren lbdconverter = new IFCtoLBDConverter_BIM4Ren();
		Model m = lbdconverter.convert(ifcFile.getAbsolutePath(), "https://dot.dc.rwth-aachen.de/IFCtoLBDset");

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
	}

	private Response handle_ifcowl(String accept_type, File tempIfcOWLFile) {
		if (accept_type.equals("application/ld+json")) {
			StringBuilder result_string = new StringBuilder();
			extractIfcOWLtoB4R(tempIfcOWLFile, result_string, RDFFormat.JSONLD_COMPACT_PRETTY);
			return Response.ok(result_string.toString(), "application/ld+json").build();
		} else if (accept_type.equals("application/rdf+xml")) {
			StringBuilder result_string = new StringBuilder();
			extractIfcOWLtoB4R(tempIfcOWLFile, result_string, RDFFormat.RDFXML);
			return Response.ok(result_string.toString(), "application/rdf+xml").build();
		} else {
			StringBuilder result_string = new StringBuilder();
			extractIfcOWLtoB4R(tempIfcOWLFile, result_string, RDFFormat.TURTLE_PRETTY);
			return Response.ok(result_string.toString(), "text/turtle").build();

		}
	}


	private void extractIfcOWLtoB4R(File ifcOwlFile, StringBuilder result_string, RDFFormat rdfformat) {
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
	}

}