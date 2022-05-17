 package de.rwth_aachen.dc.ifc2lbd.rest;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.multipart.FormDataParam;

import com.google.gson.Gson;

import de.rwth_aachen.dc.ifc2lbd.data_model.Service;
import de.rwth_aachen.dc.ifc2lbd.service.HandleIFC;
import de.rwth_aachen.dc.ifc2lbd.service.HandleIFC2IfcOWL;
import de.rwth_aachen.dc.ifc2lbd.service.HandleIfcOWL;

/*
 * Jyrki Oraskari, 2020, 2021, 2022
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
		services.add(new Service("IFCtoifcOWL DM", "IFCtoifcOWL DM", "BIM4REN", null, "http://"+uriInfo.getBaseUri().getHost()+"/IFCtoB4R_OpenAPI/api/convert_IFC-ifcOWL", null, null));
	    String json = new Gson().toJson(services);
		return Response.ok(json, "application/json").build();

	}
	
	
	/**
	 * IFC to B4R-DM for BIM4Ren Converts an IFC file into into the Linked Building
	 * Data RDF (BOT+SMLS)
	 * 	 
	 * @param ifcFile an IFC STEP file formatted file. The format is specified in ISO 10303-21:2016.
	 * @return Returns RDF output. Format is: TTL
	 */

	@POST
	@Path("/convert_IFC-B4R")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({ "text/turtle", "application/ld+json", "application/rdf+xml" })
	public Response convertIFCtoB4RDefault(@HeaderParam(HttpHeaders.ACCEPT) String accept_type,
			@FormDataParam("ifcFile") InputStream ifcFile) {
		try {
			File tempIfcFile = File.createTempFile("ifc2lbd-", ".ifc");
			tempIfcFile.deleteOnExit();

			Files.copy(ifcFile, tempIfcFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			ifcFile.close();
			return new HandleIFC().handle("text/turtle", tempIfcFile,"default");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.noContent().build();
	}
	

	/**
	 * IFC to ifcOWL for BIM4Ren Converts an IFC file into into the Linked Building
	 * Data ifcOWL RDF
	 * 
	 * It is used to create the baseURL for the model.	 
	 * 
	 * @param ifcFile an IFC STEP file formatted file. The format is specified in ISO 10303-21:2016.
	 * @return Returns RDF output. Format is: TTL
	 */

	@POST
	@Path("/convert_IFC-ifcOWL")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({ "text/turtle", "application/ld+json", "application/rdf+xml" })
	public Response convertIFCtoifcOWLDefault(@HeaderParam(HttpHeaders.ACCEPT) String accept_type,
			@FormDataParam("ifcFile") InputStream ifcFile) {
		try {
			File tempIfcFile = File.createTempFile("ifc2lbd-", ".ifc");
			tempIfcFile.deleteOnExit();

			Files.copy(ifcFile, tempIfcFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			ifcFile.close();
			return new HandleIFC2IfcOWL().handle("text/turtle", tempIfcFile,"default");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.noContent().build();
	}
	
	
	/**
	 * IFC to B4R-DM for BIM4Ren Converts an IFC file into into the Linked Building
	 * Data RDF (BOT+SMLS)
	 * 
	 * @param ifcFile an IFC STEP file formatted file. The format is specified in ISO 10303-21:2016.
	 * @return Returns RDF output. Formats are: JSON-LD, RDF/XML, and TTL
	 */

	@POST
	@Path("/convert_IFC-B4R-general")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({ "text/turtle", "application/ld+json", "application/rdf+xml" })
	public Response convertIFCtoB4R(@HeaderParam(HttpHeaders.ACCEPT) String accept_type,
			@FormDataParam("ifcFile") InputStream ifcFile) {
		try {
			File tempIfcFile = File.createTempFile("ifc2lbd-", ".ifc");
			tempIfcFile.deleteOnExit();

			Files.copy(ifcFile, tempIfcFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			ifcFile.close();
			return new HandleIFC().handle(accept_type, tempIfcFile,"default");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.noContent().build();
	}
	
	
	/**
	 * IFC to B4R-DM for BIM4Ren Converts an IFC file into into the Linked Building
	 * Data RDF (BOT+SMLS)
	 * 
	 * @param ifcFile an IFC STEP file formatted file. The format is specified in ISO 10303-21:2016.
	 * @param projectID is used to create the baseURL for the model.	 
	 * @return Returns RDF output. Formats are: JSON-LD, RDF/XML, and TTL
	 */

	@POST
	@Path("/convert_IFC-B4R/{projectID}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({ "text/turtle", "application/ld+json", "application/rdf+xml" })
	public Response convertIFCtoB4R_project(@HeaderParam(HttpHeaders.ACCEPT) String accept_type,
			@FormDataParam("ifcFile") InputStream ifcFile,@PathParam("projectID") String projectID) {
		try {
			File tempIfcFile = File.createTempFile("ifc2lbd-", ".ifc");
			tempIfcFile.deleteOnExit();

			Files.copy(ifcFile, tempIfcFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			ifcFile.close();
			return new HandleIFC().handle(accept_type, tempIfcFile,projectID.trim());

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
	 * @return Returns RDF output. Format is: TTL
	 */
	@POST
	@Path("/convert_IFC-B4R")
	@Consumes({ MediaType.TEXT_PLAIN, "application/ifc" })
	@Produces({ "text/turtle", "application/ld+json", "application/rdf+xml" })
	public Response convertIFCtoB4RnDefault(@HeaderParam(HttpHeaders.ACCEPT) String accept_type, String ifc_step_content) {
		try {
			File tempIfcFile = File.createTempFile("ifc2lbd-", ".ifc");
			FileWriter tmpWriter = new FileWriter(tempIfcFile);
			tmpWriter.write(ifc_step_content);
			tmpWriter.close();
			tempIfcFile.deleteOnExit();

			return new HandleIFC().handle("text/turtle", tempIfcFile,"default");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.noContent().build();
	}

	
	/**
	 * IFC to ifcOWL for BIM4Ren Converts an IFC file into into the Linked Building
	 * Data ifcOWL RDF
	 * 
	 * https://bim4ren.eu/
	 * 

	 * IFC STEP file as input specified in BIM4Ren D2.2. The content is a IFC STEP file formatted file. 
	 * The format is specified in ISO 10303-21:2016. All commonly used IFC versions are supported.
	 * 
	 * @return Returns RDF output. Format is: TTL
	 */
	@POST
	@Path("/convert_IFC-B4R")
	@Consumes({ MediaType.TEXT_PLAIN, "application/ifc" })
	@Produces({ "text/turtle", "application/ld+json", "application/rdf+xml" })
	public Response convertIFCtoifcOWLDefault(@HeaderParam(HttpHeaders.ACCEPT) String accept_type, String ifc_step_content) {
		try {
			File tempIfcFile = File.createTempFile("ifc2lbd-", ".ifc");
			FileWriter tmpWriter = new FileWriter(tempIfcFile);
			tmpWriter.write(ifc_step_content);
			tmpWriter.close();
			tempIfcFile.deleteOnExit();

			return new HandleIFC2IfcOWL().handle("text/turtle", tempIfcFile,"default");

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
	 * @return Returns RDF output. Formats are: JSON-LD, RDF/XML, and TTL
	 */
	@POST
	@Path("/convert_IFC-B4R-general")
	@Consumes({ MediaType.TEXT_PLAIN, "application/ifc" })
	@Produces({ "text/turtle", "application/ld+json", "application/rdf+xml" })
	public Response convertIFCtoB4Rn(@HeaderParam(HttpHeaders.ACCEPT) String accept_type, String ifc_step_content) {
		try {
			File tempIfcFile = File.createTempFile("ifc2lbd-", ".ifc");
			FileWriter tmpWriter = new FileWriter(tempIfcFile);
			tmpWriter.write(ifc_step_content);
			tmpWriter.close();
			tempIfcFile.deleteOnExit();

			return new HandleIFC().handle(accept_type, tempIfcFile,"default");

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
	 * @param projectID is used to create the baseURL for the model.	 
	 * @return Returns RDF output. Formats are: JSON-LD, RDF/XML, and TTL
	 */
	@POST
	@Path("/convert_IFC-B4R/{projectID}")
	@Consumes({ MediaType.TEXT_PLAIN, "application/ifc" })
	@Produces({ "text/turtle", "application/ld+json", "application/rdf+xml" })
	public Response convertIFCtoB4Rn_project(@HeaderParam(HttpHeaders.ACCEPT) String accept_type, String ifc_step_content,@PathParam("projectID") String projectID) {
		try {
			File tempIfcFile = File.createTempFile("ifc2lbd-", ".ifc");
			FileWriter tmpWriter = new FileWriter(tempIfcFile);
			tmpWriter.write(ifc_step_content);
			tmpWriter.close();
			tempIfcFile.deleteOnExit();

			return new HandleIFC().handle(accept_type, tempIfcFile,projectID.trim());

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
 
	 * @return Returns RDF output. Formats are: JSON-LD, RDF/XML, and TTL
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

			return new HandleIfcOWL().handle(accept_type, tempIfcOWLFile);

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
	 * @return Returns RDF output. Formats are: JSON-LD, RDF/XML, and TTL
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

			return new HandleIfcOWL().handle(accept_type, tempIfcOWLFile);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.noContent().build();
	}





}