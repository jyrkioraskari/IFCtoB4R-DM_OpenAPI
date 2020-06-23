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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
import org.lbd.ifc2lbd.smls.IFCtoLBDConverter_BIM4Ren;

import de.rwth_aachen.dc.ifc2lbd.BimServerPasswords;


/*
 * Jyrki Oraskari, 2020
 */

@Path("/")
public class IFCtoLBD_SMLS_OpenAPI {

	/**
	 * 
	 * 
	 * A HTTP GET interface to test the REST API connection.
	 * 
	 * @return Hello from IFCtoLBD_SMLS_OpenAPI! It works.
	 */
	@GET
	@Path("/hello")
	@Produces(MediaType.TEXT_PLAIN)
	public String hello() {
		return "Hello from IFCtoLBD_OpenAPI! It works. ";
	}

	
	
	/**
	 * Converts a BIMServer project into RDF (LBD_SMLS)
	 * 
	 * @param project_name  BIMServer project name
	 * @return Returns a JSONLD formatted output
	 */
	@POST
	@Path("/convertBIMProject2SMLS/{project_name}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response convert(@PathParam("id") String project_name) {
		try {
			File ifFile=downloadLastRelease(project_name);

			StringBuilder result_string=new StringBuilder();
			extractLBD_SMLS(ifFile, result_string);
			return Response.ok(result_string.toString(), MediaType.TEXT_PLAIN).build();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.noContent().build();
	}

	
	
	
	/**
	 * Converts an IFC file into into RDF (LBD BOT https://w3c-lbd-cg.github.io/bot/)
	 * 
	 * @param ifcFile an IFC file
	 * @return  TTL formatted LBD output
	 */
	@POST
	@Path("/convertIFCtoLBD")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public Response convertIFCtoLBD(
			@FormDataParam("ifcFile") InputStream ifcFile) {
		try {
			File tempIfcFile = File.createTempFile("ifc2lbd-", ".ifc");
			tempIfcFile.deleteOnExit();

			Files.copy(ifcFile, tempIfcFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			IOUtils.closeQuietly(ifcFile);
			StringBuilder result_string=new StringBuilder();
			extractLBD(tempIfcFile, result_string);
			System.out.println(result_string.toString());
			return Response.ok(result_string.toString(), MediaType.TEXT_PLAIN).build();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.noContent().build();
	}
	
	/**
	 * Converts an IFC file into into RDF (LBD BOT+SMLS)
	 * 
	 * @param ifcFile an IFC file
	 * @return  JSONLD formatted output
	 */
	@POST
	@Path("/convertIFCtoSMLS")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public Response convertIFCtoSMLS(
			@FormDataParam("ifcFile") InputStream ifcFile) {
		try {
			File tempIfcFile = File.createTempFile("ifc2lbd-", ".ifc");
			tempIfcFile.deleteOnExit();

			Files.copy(ifcFile, tempIfcFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			IOUtils.closeQuietly(ifcFile);
			StringBuilder result_string=new StringBuilder();
			extractLBD_SMLS(tempIfcFile, result_string);
			System.out.println(result_string.toString());
			return Response.ok(result_string.toString(), MediaType.TEXT_PLAIN).build();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.noContent().build();
	}

	private void extractLBD(File ifcFile, StringBuilder result_string) {
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
		//m.write(System.out, "TTL");
		//m.write(ttl_output, "TTL");
		RDFDataMgr.write(ttl_output, m, RDFFormat.TTL);
		result_string.append(ttl_output.toString());
	}

	
	private void extractLBD_SMLS(File ifcFile, StringBuilder result_string) {
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
		//m.write(System.out, "TTL");
		//m.write(ttl_output, "TTL");
		RDFDataMgr.write(ttl_output, m, RDFFormat.JSONLD_COMPACT_PRETTY);
		result_string.append(ttl_output.toString());
	}
	
	private  File downloadLastRelease(String projectName)
	{
		try {
			JsonBimServerClientFactory factory = new JsonBimServerClientFactory("http://localhost:8080");
			BimServerClient client = factory.create(
					new UsernamePasswordAuthenticationInfo(BimServerPasswords.user, BimServerPasswords.password));

			List<SProject> projects = client.getServiceInterface().getAllReadableProjects();
			byte[] data = null;
			for (SProject p : projects) {
				if(p.getState()==SObjectState.ACTIVE)
				if (p.getLastRevisionId() >= 0 && p.getName().equals(projectName))
				{
					
					System.out.println(p.getName()+" "+p.getState().name());
					SRevision revision = client.getServiceInterface().getRevision(p.getLastRevisionId());
					

					SSerializerPluginConfiguration serializerByContentType = client.getServiceInterface()
							.getSerializerByContentType("application/ifc");
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					client.download(revision.getOid(), serializerByContentType.getOid(), outputStream);
					data = outputStream.toByteArray();
					System.out.println("len: "+data.length);
					
					File tempFile = File. createTempFile("ifc2lbd", ".ifc");
					FileOutputStream fo=new FileOutputStream(tempFile);
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