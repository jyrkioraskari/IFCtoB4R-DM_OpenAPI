 package de.rwth_aachen.dc.ifc2lbd.rest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.glassfish.jersey.media.multipart.FormDataParam;

import de.rwth_aachen.dc.lbd_smls.IFCtoLBDConverter_BIM4Ren;
import de.rwth_aachen.dc.lbd_smls.IfcOWLtoLBDConverter_BIM4Ren;

/*
 * Jyrki Oraskari, 2020
 */

@Path("/")
public class IFCtoB4R_OpenAPI {

	
	
	/**
	 * IFC to B4R-DM for BIM4Ren Converts an IFC file into into the Linked Building
	 * Data RDF (BOT+SMLS)
	 * 
	 * https://bim4ren.eu/
	 * 
	 * + Geometry: Bounding Boxes
	 * 
	 * All commonly used IFC versions are supported. 
	 * 
	 * https://www.buildingsmart.org/standards/bsi-standards/industry-foundation-classes/
	 * 
	 * 
	 * OpenAPI interface of the IFC-B4R DM converter accepts an IFC STEP formatted file as an input. 
	 * HTTP POST protocol is used where two content-types for input data are accepted. 
	 * They are plain text-based application/ifc that was defined in BIM4Ren WP2 D2.2., 
	 * and MULTIPART_FORM_DATA Multimedia Internet Message Extensions (MIME).
	 * 
	 * The proposed IFC-B4R DM converter can handle IFC files from all actively used IFC schemas, 
	 * from IFC2x3 TC1 till IFC4x3.  The converter first uses the existing IFC-to-RDF converter 
	 * internally to transform the original IFC file to a temporary ifcOWL Abox graph 
	 * according to the ifcOWL ontology. 
	 * 
	 * When the intermediate ifcOWL Abox graph is ready, the converter iteratively creates new LBD nodes 
	 * into a separate RDF graph starting from the ifcOWL nodes that are of RDF types referred at 
	 * the Building Topology Ontology ifcOWL Alignment specification . The converter follows 
	 * the controlled graph traversal using Java coded RDF path templates: it begins from 
	 * the IfcSite instance node and traverses the ifcOWL graph towards its IfcBuilding instance 
	 * nodes, while at the same time looking for the IFC properties of the IfcSite instance. 
	 * In a third phase, it queries all instances of IfcBuildingStorey of the previously 
	 * found IfcBuilding cases, together with the properties of these same IfcBuilding instances. 
	 * This process is continued until all IFC building elements, and their properties are found.
	 * 
	 * The converter also adds the SMLS  unit types of measurement in property set values when 
	 * the unit is known in the IFC model. The converter infers these in three phases. In 
	 * the first phase, the ifcowl:IfcSIUnit statements are read from the ifcOWL model and 
	 * collected in an internal data map. In the next phase, the property sets are read, and 
	 * the nominalValue_IfcPropertySingleValue and rdf:type are saved. There are specialized 
	 * RDF paths for ifcowl:hasString, ifcowl:hasDouble, ifcowl:hasInteger, ifcowl:hasBoolean, 
	 * and ifcowl:hasLogical. All other data types are copied as they are in ifcOWL.  
	 * In the last phase, when the property is referred in an element, and a corresponding 
	 * LBD graph is created, the unit type is added using the smls:unit property and the Base 
	 * Units Ontology Version 2.0 - Qudt  values unit:M for metre, unit:M2 for square metre, 
	 * unit:M3 for cubic metre, and unit:RAD for Radian.
	 * 
	 * Since IFC models tend to be huge and using the Apache Jena RDF engine is memory intensive 
	 * the converter saves memory in the following manner: IFC-to-RDF converter creates the 
	 * ifcOWL transformation line based creating RDF triples in a streaming way. When the phase 
	 * is finished, the output is given as a file for the ifcOWL – B4R-DM conversion phase. 
	 * The file is read in as Turtle FLAT file triple by triple and as the ifcOWL standard is defined 
	 * so that the property names also contain the inherited class of the element that is used to 
	 * filter geometry and IfcOwnerHistory components from the graph. Because the B4R-DM model 
	 * is an order of magnitude smaller than the ifcOWL Abox model, the reduced memory consumption 
	 * is notable. When the standard IFC-to-RDF translator is used, it is given its output in Turtle 
	 * Blocks format. The converter transforms that into Turtle flat in stream base to allow easy filtering.
	 * 
	 * In case there would be a need to downstream the modifications that are made into the B4R-DM model 
	 * back to ifcOWL or to access the data contained in the ifcOWL model from the B4R-DM graph, the 
	 * corresponding elements in the models are connected using owl:sameAS statements. The converter adds 
	 * them when both models are co-existing in the memory, and a new Linked Building Data RDF resource 
	 * is created from the ifcOWL counterpart. 
	 * 
	 * Furthermore, the bounding boxes for the elements are given. For this, the ifcOpenShell geometry 
	 * engine is used. Of the x,y, and z 3D co-ordinate values minimum, and maximum values are calculated 
	 * for each element and they are attached to the associated IFC elements in the B4R-DM graph using 
	 * the geo:hasGeometry and geo:asWKT triples of GeoSPARQL 1.0 . The bounding box is represented as 
	 * the Well-Known Text format  Multipoint. 
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
	 * @param ifcOWLFile anifcOWL Abox Turtle formatted file as form parameter. If the file does not confirm with the ifcOWL specification, an empty output is given.
 
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