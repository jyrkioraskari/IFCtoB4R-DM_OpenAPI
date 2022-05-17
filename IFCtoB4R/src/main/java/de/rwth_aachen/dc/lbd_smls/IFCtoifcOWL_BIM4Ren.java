
package de.rwth_aachen.dc.lbd_smls;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetImpl;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.renderengine.RenderEngineException;

import com.openifctools.guidcompressor.GuidCompressor;

import be.ugent.IfcSpfReader;
import de.rwth_aachen.dc.lbd.BoundingBox;
import de.rwth_aachen.dc.lbd.IFCBoundingBoxes;
import de.rwth_aachen.dc.lbd_smls.geo.IFC_Geolocation;
import de.rwth_aachen.dc.lbd_smls.geo.WktLiteral;
import de.rwth_aachen.dc.lbd_smls.ns.BOT;
import de.rwth_aachen.dc.lbd_smls.ns.GEO;
import de.rwth_aachen.dc.lbd_smls.ns.IfcOWLNameSpace;
import de.rwth_aachen.dc.lbd_smls.ns.OPM;
import de.rwth_aachen.dc.lbd_smls.ns.PROPS;
import de.rwth_aachen.dc.lbd_smls.ns.Product;
import de.rwth_aachen.dc.lbd_smls.ns.SMLS;
import de.rwth_aachen.dc.lbd_smls.ns.SOT;
import de.rwth_aachen.dc.lbd_smls.ns.UNIT;
import de.rwth_aachen.dc.lbd_smls.utils.FileUtils;
import de.rwth_aachen.dc.lbd_smls.utils.IfcOWLUtils;
import de.rwth_aachen.dc.lbd_smls.utils.RDFUtils;
import de.rwth_aachen.dc.lbd_smls.utils.rdfpath.InvRDFStep;
import de.rwth_aachen.dc.lbd_smls.utils.rdfpath.RDFStep;

/*
 *  Copyright (c) 2022 Jyrki Oraskari (Jyrki.Oraskari@gmail.fi)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * <img src="https://jyrkioraskari.github.io/IFCtoLBD/doc-graphs/Overview.PNG">
 * <P>
 * <img src=
 * "https://jyrkioraskari.github.io/IFCtoLBD/doc-graphs/IFCtoLBDConverter_class_diagram.png">
 */

public class IFCtoifcOWL_BIM4Ren {
	private Model ifcowl_model;
	private Optional<String> ontURI = Optional.empty();

	public Model convertToModel(String ifc_filename, String uriBase) {

		if (!uriBase.endsWith("#") && !uriBase.endsWith("/"))
			uriBase += "#";

		ifcowl_model = readAndConvertIFC(ifc_filename, uriBase); // Before: readInOntologies(ifc_filename);

		return ifcowl_model;
	}

	public Dataset convertToDataset(String ifc_filename, String uriBase) {
		Model m=convertToModel(ifc_filename, uriBase);
		Dataset lbd_dataset = DatasetFactory.create();
	    lbd_dataset.addNamedModel(uriBase, m);
		return lbd_dataset;
	}

	
	/**
	 * 
	 * The method converts an IFC STEP formatted file and returns an Apache Jena RDF
	 * memory storage model that contains the generated RDF triples.
	 * 
	 * Apache Jena: https://jena.apache.org/index.html
	 * 
	 * The generated temporsary file is used to reduce the temporary memory need and
	 * make it possible to convert larger models.
	 * 
	 * Sets the this.ontURI class variable. That is used to create the right ifcOWL
	 * version based ontology base URI that is used to create the ifcOWL version
	 * based peroperties and class URIs-
	 * 
	 * @param ifc_file the absolute path (For example: c:\ifcfiles\ifc_file.ifc) for
	 *                 the IFC file
	 * @param uriBase  the URL beginning for the elements in the ifcOWL TTL output
	 * @return the Jena Model that contains the ifcOWL attribute value (Abox)
	 *         output.
	 */
	public Model readAndConvertIFC(String ifc_file, String uriBase) {
		try {
			IfcSpfReader rj = new IfcSpfReader();
			File tempFile = File.createTempFile("ifc", ".ttl");
			try {
				Model m = ModelFactory.createDefaultModel(); //
															// super slow
				m.setNsPrefix("rdf", RDF.uri);
				m.setNsPrefix("rdfs", RDFS.uri);
				m.setNsPrefix("owl", OWL.getURI());
				m.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
				m.setNsPrefix("inst", uriBase);

				this.ontURI = rj.convert(ifc_file, tempFile.getAbsolutePath(), uriBase);
				//File t2 = filterContent(tempFile);
				//RDFDataMgr.read(m, t2.getAbsolutePath());
				RDFDataMgr.read(m, tempFile.getAbsolutePath());

				return m;
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				tempFile.deleteOnExit();
			}

		} catch (Exception e) {
			e.printStackTrace();

		}
		System.out.println("IFC-RDF conversion not done");
		return ModelFactory.createDefaultModel();
	}
	@SuppressWarnings("unused")
	private File filterContent(File whole_content_file) {
		File tempFile = null;
		int state = 0;
		try {
			tempFile = File.createTempFile("ifc", ".ttl");
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
				try (BufferedReader br = new BufferedReader(new FileReader(whole_content_file))) {
					String line;
					String[] triple = new String[3];
					for (int i = 0; i < 3; i++)
						triple[i] = "";
					while ((line = br.readLine()) != null) {
						String trimmed = line.trim();
						if (!line.contains("@prefix") && !trimmed.startsWith("#")) {
							int len = trimmed.length();
							if (len > 0) {
								List<String> t;
								if (trimmed.endsWith(".") || trimmed.endsWith(";"))
									t = split(trimmed.substring(0, trimmed.length() - 1));
								else
									t = split(trimmed.substring(0, trimmed.length()));
								if (state == 0) {
									for (int i = 0; i < t.size(); i++)
										triple[i] = t.get(i);

									if (trimmed.endsWith("."))
										state = 0;
									else
										state = 1;
									if (t.size() == 3) {
										StringBuffer sb = new StringBuffer();
										sb.append(t.get(0));
										sb.append(" ");
										sb.append(t.get(1));
										sb.append(" ");
										sb.append(t.get(2));
										sb.append(" .");
										line = sb.toString();
									} else
										continue;
								} else {
									for (int i = 0; i < t.size(); i++)
										triple[2 - i] = t.get(t.size() - 1 - i);

									StringBuffer sb = new StringBuffer();
									sb.append(triple[0]);
									sb.append(" ");
									sb.append(triple[1]);
									sb.append(" ");
									sb.append(triple[2]);
									sb.append(" .");
									line = sb.toString();

									if (trimmed.endsWith("."))
										state = 0;
								}
							}
						}
						line = new String(line.getBytes(), StandardCharsets.UTF_8);
						line = line.replace("\\\\", "\\");

						// UTF-8 fix for French double encoding
						line = line.replace("\\X\\0D", "");
						line = line.replace("\\X\\0A", "");

						line = line.replace("\\X2\\00A0\\X0\\", "");
						// LATIN letters
						line = line.replace("\\X2\\00C0\\X0\\", "À");
						line = line.replace("\\X2\\00C1\\X0\\", "Á");
						line = line.replace("\\X2\\00C2\\X0\\", "Â");
						line = line.replace("\\X2\\00C3\\X0\\", "Ã");
						line = line.replace("\\X2\\00C4\\X0\\", "Ä");
						line = line.replace("\\X2\\00C5\\X0\\", "Å");
						line = line.replace("\\X2\\00C6\\X0\\", "Æ");
						line = line.replace("\\X2\\00C7\\X0\\", "Ç");
						line = line.replace("\\X2\\00C8\\X0\\", "È");
						line = line.replace("\\X2\\00C9\\X0\\", "É");
						line = line.replace("\\X2\\00CA\\X0\\", "Ê");
						line = line.replace("\\X2\\00CB\\X0\\", "Ë");
						line = line.replace("\\X2\\00CC\\X0\\", "Ì");
						line = line.replace("\\X2\\00CD\\X0\\", "Í");
						line = line.replace("\\X2\\00CE\\X0\\", "Î");
						line = line.replace("\\X2\\00CF\\X0\\", "Ï");

						line = line.replace("\\X2\\00D0\\X0\\", "Ð");
						line = line.replace("\\X2\\00D1\\X0\\", "Ñ");
						line = line.replace("\\X2\\00D2\\X0\\", "Ò");
						line = line.replace("\\X2\\00D3\\X0\\", "Ó");
						line = line.replace("\\X2\\00D4\\X0\\", "Ô");
						line = line.replace("\\X2\\00D5\\X0\\", "Õ");
						line = line.replace("\\X2\\00D6\\X0\\", "Ö");
						line = line.replace("\\X2\\00D7\\X0\\", "×");
						line = line.replace("\\X2\\00D8\\X0\\", "Ø");
						line = line.replace("\\X2\\00D9\\X0\\", "Ù");
						line = line.replace("\\X2\\00DA\\X0\\", "Ú");
						line = line.replace("\\X2\\00DB\\X0\\", "Û");
						line = line.replace("\\X2\\00DC\\X0\\", "Ü");
						line = line.replace("\\X2\\00DD\\X0\\", "Ý");
						line = line.replace("\\X2\\00DE\\X0\\", "Þ");
						line = line.replace("\\X2\\00DF\\X0\\", "ß");

						line = line.replace("\\X2\\00E0\\X0\\", "à");
						line = line.replace("\\X2\\00E1\\X0\\", "á");
						line = line.replace("\\X2\\00E2\\X0\\", "â");
						line = line.replace("\\X2\\00E3\\X0\\", "ã");
						line = line.replace("\\X2\\00E4\\X0\\", "ä");
						line = line.replace("\\X2\\00E5\\X0\\", "å");
						line = line.replace("\\X2\\00E6\\X0\\", "æ");
						line = line.replace("\\X2\\00E7\\X0\\", "ç");
						line = line.replace("\\X2\\00E8\\X0\\", "è");
						line = line.replace("\\X2\\00E9\\X0\\", "é");
						line = line.replace("\\X2\\00EA\\X0\\", "ê");
						line = line.replace("\\X2\\00EB\\X0\\", "ê");
						line = line.replace("\\X2\\00EC\\X0\\", "ì");
						line = line.replace("\\X2\\00ED\\X0\\", "í");
						line = line.replace("\\X2\\00EE\\X0\\", "î");
						line = line.replace("\\X2\\00EF\\X0\\", "ï");

						line = line.replace("\\X2\\00F0\\X0\\", "ð");
						line = line.replace("\\X2\\00F1\\X0\\", "ñ");
						line = line.replace("\\X2\\00F2\\X0\\", "ò");
						line = line.replace("\\X2\\00F3\\X0\\", "ó");
						line = line.replace("\\X2\\00F4\\X0\\", "ô");
						line = line.replace("\\X2\\00F5\\X0\\", "õ");
						line = line.replace("\\X2\\00F6\\X0\\", "ö");
						line = line.replace("\\X2\\00F7\\X0\\", "÷");
						line = line.replace("\\X2\\00F8\\X0\\", "ø");
						line = line.replace("\\X2\\00F9\\X0\\", "ù");
						line = line.replace("\\X2\\00FA\\X0\\", "ú");
						line = line.replace("\\X2\\00FB\\X0\\", "û");
						line = line.replace("\\X2\\00FC\\X0\\", "ü");
						line = line.replace("\\X2\\00FD\\X0\\", "ý");
						line = line.replace("\\X2\\00FE\\X0\\", "þ");
						line = line.replace("\\X2\\00FF\\X0\\", "ÿ");

						line = line.replace("\\", "\\\\");
						line = line.replace("\\\\\"", "\\\"");

						if (line.contains("inst:IfcFace"))
							continue;
						if (line.contains("inst:IfcPolyLoop"))
							continue;
						if (line.contains("inst:IfcCartesianPoint"))
							continue;
						if (line.contains("inst:IfcOwnerHistory"))
							continue;
						if (line.contains("inst:IfcRelAssociatesMaterial"))
							continue;

						if (line.contains("inst:IfcExtrudedAreaSolid"))
							continue;
						if (line.contains("inst:IfcCompositeCurve"))
							continue;
						if (line.contains("inst:IfcSurfaceStyleRendering"))
							continue;
						if (line.contains("inst:IfcStyledItem"))
							continue;
						if (line.contains("inst:IfcShapeRepresentation"))
							continue;

						writer.write(line.trim());
						writer.newLine();
					}
					writer.flush();

				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		return tempFile;
	}

	@SuppressWarnings("deprecation")
	private List<String> split(String s) {
		List<String> ret = new ArrayList<>();
		int state = 0;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (state) {
			case 2:
				if (c == '\"' || c == '\'')
					state = 0;
				sb.append(c);
				break;
			case 1:
				if (c == '\"' || c == '\'') {
					ret.add(sb.toString());
					sb = new StringBuffer();
					sb.append(c);
					state = 2;
				} else if (!Character.isSpace(c)) {
					ret.add(sb.toString());
					sb = new StringBuffer();
					sb.append(c);
					state = 0;
				}
				break;
			case 0:
				if (c == '\"' || c == '\'') {
					sb.append(c);
					state = 2;
				} else if (Character.isSpace(c))
					state = 1;
				else
					sb.append(c);
				break;
			}
		}
		if (sb.length() > 0)
			ret.add(sb.toString());
		return ret;
	}

	  
	
	/* remove prefixed and translate https to http */
	private String setIFC_NS(String txt, String ontology_URI) {
		if (!ontology_URI.endsWith("#") && !ontology_URI.endsWith("/"))
			ontology_URI += "#";
		StringBuffer sb = new StringBuffer();
		String[] lines = txt.split("\n");
		for (String l : lines) {
			if (l.startsWith("@prefix")) {
				String[] parts = l.split(" ");
				if (parts[1].trim().equals("ifc:"))
					sb.append("@prefix ifc: <" + ontology_URI + "> .\n");
				else				if (parts[1].trim().equals("ifc4:"))
					sb.append("@prefix ifc4: <" + ontology_URI + "> .\n");
				else

					sb.append(l + "\n");
			} else {
				sb.append(l + "\n");
			}
		}
		return sb.toString();
	}
}
