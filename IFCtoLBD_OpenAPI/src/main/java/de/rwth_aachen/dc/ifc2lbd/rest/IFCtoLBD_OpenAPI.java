package de.rwth_aachen.dc.ifc2lbd.rest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.bimserver.client.BimServerClient;
import org.bimserver.client.json.JsonBimServerClientFactory;
import org.bimserver.interfaces.objects.SObjectState;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.interfaces.objects.SRevision;
import org.bimserver.interfaces.objects.SSerializerPluginConfiguration;
import org.bimserver.shared.ChannelConnectionException;
import org.bimserver.shared.UsernamePasswordAuthenticationInfo;
import org.bimserver.shared.exceptions.BimServerClientException;
import org.bimserver.shared.exceptions.PublicInterfaceNotFoundException;
import org.bimserver.shared.exceptions.ServiceException;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.lbd.ifc2lbd.IFCtoLBDConverter;
import org.lbd.ifc2lbd.IfcOWLtoLBDConverter;

import de.rwth_aachen.dc.ifc2lbd.BimServerPasswords;
import de.rwth_aachen.dc.lbd_smls.IFCtoLBDConverter_BIM4Ren;
import de.rwth_aachen.dc.lbd_smls.IfcOWLtoLBDConverter_BIM4Ren;

/*
 * Jyrki Oraskari, 2020
 */

@Path("/")
public class IFCtoLBD_OpenAPI {

//	/**
//	 * Converts a BIMServer project into RDF (LBD_SMLS) 
//	 * NOTE: A running BIMServer installation is needed for this to function.
//	 * ---------------------------------------------------------------------
//	 * @param project_name BIMServer project name
//	 * @return Returnd RDF output. Formats are:  JSON-LD,  RDF/XML, and TTL
//	 */
//	@POST
//	@Path("/convertBIMProject2SMLS/{project_name}")
//	@Produces(MediaType.APPLICATION_OCTET_STREAM)
//	public Response convert(@PathParam("id") String project_name) {
//		try {
//			File ifFile = downloadLastRelease(project_name);
//
//			StringBuilder result_string = new StringBuilder();
//			extractLBD_SMLS(ifFile, result_string, RDFFormat.JSONLD_COMPACT_PRETTY);
//			return Response.ok(result_string.toString(), MediaType.TEXT_PLAIN).build();
//
//		} catch (Exception e) {
//			//e.printStackTrace();
//		}
//
//		return Response.noContent().build();
//	}

	/**
     * General IFCtoLBD OPM Level 3  
	 * Converts an IFC file into into the Linked Building Data  (BOT
	 * https://w3c-lbd-cg.github.io/bot/)
	 * 
	 * @param ifcFile an IFC file
	 * @return Returnd RDF output. Formats are:  JSON-LD,  RDF/XML, and TTL
	 */
	@POST
	@Path("/convertIFCtoLBD")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({"text/turtle", "application/ld+json", "application/rdf+xml"})
	public Response convertIFCtoLBD(@HeaderParam(HttpHeaders.ACCEPT) String accept_type,@FormDataParam("ifcFile") InputStream ifcFile) {
		try {
			File tempIfcFile = File.createTempFile("ifc2lbd-", ".ifc");
			tempIfcFile.deleteOnExit();

			Files.copy(ifcFile, tempIfcFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			IOUtils.closeQuietly(ifcFile);
			if (accept_type.equals("application/ld+json")) {
				StringBuilder result_string = new StringBuilder();
				extractIFCtoLBD(tempIfcFile, result_string, RDFFormat.JSONLD_COMPACT_PRETTY);
				return Response.ok(result_string.toString(), "application/ld+json").build();
			} else if (accept_type.equals("application/rdf+xml")) {
				StringBuilder result_string = new StringBuilder();
				extractIFCtoLBD(tempIfcFile, result_string, RDFFormat.RDFXML);
				return Response.ok(result_string.toString(), "application/rdf+xml").build();
			} else {
				StringBuilder result_string = new StringBuilder();
				extractIFCtoLBD(tempIfcFile, result_string, RDFFormat.TURTLE_PRETTY);
				return Response.ok(result_string.toString(), "text/turtle").build();

			}


		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.noContent().build();
	}

	/**
     * General IfcOwltoLBD OPM Level 3  
	 * Converts an IfcOWL file into into the Linked Building Data RDF (BOT
	 * https://w3c-lbd-cg.github.io/bot/)
	 * 
	 * @param ifcOWLFile an IfcOWL TTL formatted file
	 * @return Returnd RDF output. Formats are:  JSON-LD,  RDF/XML, and TTL
	 */
	@POST
	@Path("/convertIfcOWLtoLBD")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({"text/turtle", "application/ld+json", "application/rdf+xml"})
	public Response convertIfcOWLtoLBD(@HeaderParam(HttpHeaders.ACCEPT) String accept_type, @FormDataParam("ifcOWLFile") InputStream ifcOWLFile) {
		try {
			File tempIfcOWLFile = File.createTempFile("ifc2lbd-", ".ttl");
			tempIfcOWLFile.deleteOnExit();

			Files.copy(ifcOWLFile, tempIfcOWLFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			IOUtils.closeQuietly(ifcOWLFile);
			
			if (accept_type.equals("application/ld+json")) {
				StringBuilder result_string = new StringBuilder();
				extractIfcOWLtoLBD(tempIfcOWLFile, result_string, RDFFormat.JSONLD_COMPACT_PRETTY);
				return Response.ok(result_string.toString(), "application/ld+json").build();
			} else if (accept_type.equals("application/rdf+xml")) {
				StringBuilder result_string = new StringBuilder();
				extractIfcOWLtoLBD(tempIfcOWLFile, result_string, RDFFormat.RDFXML);
				return Response.ok(result_string.toString(), "application/rdf+xml").build();
			} else {
				StringBuilder result_string = new StringBuilder();
				extractIfcOWLtoLBD(tempIfcOWLFile, result_string, RDFFormat.TURTLE_PRETTY);
				return Response.ok(result_string.toString(), "text/turtle").build();

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.noContent().build();
	}

	
	

	/**
	 * IFCtoLBD for BIM4Ren  
	 * Converts an IFC file into into the Linked Building Data RDF (BOT+SMLS)
	 * 
	 * https://bim4ren.eu/
	 * 
	 *  + Geometry:  Bounding Boxes
	 * @param ifcFile an IFC file
	 * @return JSONLD formatted output
	 */
	@POST
	@Path("/convertIFCtoLBD_for_BIM4Ren")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({"text/turtle", "application/ld+json", "application/rdf+xml"})
	public Response convertIFCtoSMLS(@HeaderParam(HttpHeaders.ACCEPT) String accept_type,
			@FormDataParam("ifcFile") InputStream ifcFile) {
		try {
			File tempIfcFile = File.createTempFile("ifc2lbd-", ".ifc");
			tempIfcFile.deleteOnExit();

			Files.copy(ifcFile, tempIfcFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			IOUtils.closeQuietly(ifcFile);
			if (accept_type.equals("application/ld+json")) {
				StringBuilder result_string = new StringBuilder();
				extractIFCtoLBD_SMLS(tempIfcFile, result_string, RDFFormat.JSONLD_COMPACT_PRETTY);
				System.out.println(result_string.toString());
				return Response.ok(result_string.toString(), "application/ld+json").build();
			} else if (accept_type.equals("application/rdf+xml")) {
				StringBuilder result_string = new StringBuilder();
				extractIFCtoLBD_SMLS(tempIfcFile, result_string, RDFFormat.RDFXML);
				System.out.println(result_string.toString());
				return Response.ok(result_string.toString(), "application/rdf+xml").build();
			} else {
				StringBuilder result_string = new StringBuilder();
				extractIFCtoLBD_SMLS(tempIfcFile, result_string, RDFFormat.TURTLE_PRETTY);
				System.out.println(result_string.toString());
				return Response.ok(result_string.toString(), "text/turtle").build();

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.noContent().build();
	}
	
	
	/**
	 * IfcOWLtoLBD for BIM4Ren  
	 * Converts an IfcOWL file into into the Linked Building Data RDF ((BOT+SMLS)
	 * 
	 * https://bim4ren.eu/
	 * 
	 * @param ifcOWLFile an IfcOWL TTL formatted file
	 * @return Returnd RDF output. Formats are:  JSON-LD,  RDF/XML, and TTL
	 */
	@POST
	@Path("/convertIfcOWtoLBD_for_BIM4Ren")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({"text/turtle", "application/ld+json", "application/rdf+xml"})
	public Response convertIfcOWLtoSMLS(@HeaderParam(HttpHeaders.ACCEPT) String accept_type, @FormDataParam("ifcOWLFile") InputStream ifcOWLFile) {
		try {
			File tempIfcOWLFile = File.createTempFile("ifc2lbd-", ".ttl");
			tempIfcOWLFile.deleteOnExit();

			Files.copy(ifcOWLFile, tempIfcOWLFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			IOUtils.closeQuietly(ifcOWLFile);
			
			if (accept_type.equals("application/ld+json")) {
				StringBuilder result_string = new StringBuilder();
				extractIfcOWLtoBD_SMLS(tempIfcOWLFile, result_string, RDFFormat.JSONLD_COMPACT_PRETTY);
				return Response.ok(result_string.toString(), "application/ld+json").build();
			} else if (accept_type.equals("application/rdf+xml")) {
				StringBuilder result_string = new StringBuilder();
				extractIfcOWLtoBD_SMLS(tempIfcOWLFile, result_string, RDFFormat.RDFXML);
				return Response.ok(result_string.toString(), "application/rdf+xml").build();
			} else {
				StringBuilder result_string = new StringBuilder();
				extractIfcOWLtoBD_SMLS(tempIfcOWLFile, result_string, RDFFormat.TURTLE_PRETTY);
				return Response.ok(result_string.toString(), "text/turtle").build();

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.noContent().build();
	}

	


	private void extractIFCtoLBD(File ifcFile, StringBuilder result_string, RDFFormat rdfformat) {
		IFCtoLBDConverter lbdconverter = new IFCtoLBDConverter("https://dot.dc.rwth-aachen.de/IFCtoLBDset", false, 3);
		Model m = lbdconverter.convert(ifcFile.getAbsolutePath());

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

	
	private void extractIfcOWLtoLBD(File ifcOWLFile, StringBuilder result_string, RDFFormat rdfformat) {
		IfcOWLtoLBDConverter lbdconverter = new IfcOWLtoLBDConverter(false, 3);
		Model m = lbdconverter.convert(ifcOWLFile.getAbsolutePath());

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
	private void extractIFCtoLBD_SMLS(File ifcFile, StringBuilder result_string, RDFFormat rdfformat) {
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

	
	private void extractIfcOWLtoBD_SMLS(File ifcOwlFile, StringBuilder result_string, RDFFormat rdfformat) {
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
	
	private File downloadLastRelease(String projectName) {
		try {
			JsonBimServerClientFactory factory = new JsonBimServerClientFactory("http://localhost:8080");
			BimServerClient client = factory.create(
					new UsernamePasswordAuthenticationInfo(BimServerPasswords.user, BimServerPasswords.password));

			List<SProject> projects = client.getServiceInterface().getAllReadableProjects();
			byte[] data = null;
			for (SProject p : projects) {
				if (p.getState() == SObjectState.ACTIVE)
					if (p.getLastRevisionId() >= 0 && p.getName().equals(projectName)) {

						System.out.println(p.getName() + " " + p.getState().name());
						SRevision revision = client.getServiceInterface().getRevision(p.getLastRevisionId());

						SSerializerPluginConfiguration serializerByContentType = client.getServiceInterface()
								.getSerializerByContentType("application/ifc");
						ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
						client.download(revision.getOid(), serializerByContentType.getOid(), outputStream);
						data = outputStream.toByteArray();
						System.out.println("len: " + data.length);

						File tempFile = File.createTempFile("ifc2lbd", ".ifc");
						FileOutputStream fo = new FileOutputStream(tempFile);
						fo.write(data);
						fo.close();
						return tempFile;
					}
			}
		} catch (BimServerClientException e) {
			e.printStackTrace();
		} catch (ServiceException e) {
			e.printStackTrace();
		} catch (ChannelConnectionException e) {
			e.printStackTrace();
		} catch (PublicInterfaceNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}