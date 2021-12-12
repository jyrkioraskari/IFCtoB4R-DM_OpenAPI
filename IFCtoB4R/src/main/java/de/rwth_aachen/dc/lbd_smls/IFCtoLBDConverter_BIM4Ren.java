
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
 *  Copyright (c) 2021 Jyrki Oraskari (Jyrki.Oraskari@gmail.fi)
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

public class IFCtoLBDConverter_BIM4Ren {
	private Model ifcowl_model;
	private Model ontology_model = null;
	private Map<String, List<Resource>> ifcowl_product_map;
	private String uriBase;

	private Optional<String> ontURI = Optional.empty();
	private IfcOWLNameSpace ifcOWL;

	// URI-property set
	private Map<String, PropertySet_B4R> propertysets;

	private Model lbd_general_output_model;
	
	private IFCBoundingBoxes bounding_boxes = null;

	public Model convertToModel(String ifc_filename, String uriBase) {
		System.out.println("convert");
		this.propertysets = new HashMap<>();
		this.ifcowl_product_map = new HashMap<>();

		if (!uriBase.endsWith("#") && !uriBase.endsWith("/"))
			uriBase += "#";
		this.uriBase = uriBase;

		try {
			System.out.println("Set the bounding box generator");
			this.bounding_boxes = new IFCBoundingBoxes(new File(ifc_filename));
		} catch (RenderEngineException | DeserializeException | IOException e) {
			e.printStackTrace();
		}
		ontology_model = ModelFactory.createDefaultModel();
		String ifc_model_file_base = ifc_filename.substring(0, ifc_filename.lastIndexOf("."));

		ifcowl_model = readAndConvertIFC(ifc_filename, uriBase); // Before: readInOntologies(ifc_filename);

		System.out.println("read ontologies");
		readInOntologies(ifc_filename);
		writeModel(ifcowl_model, ifc_model_file_base + "_ifcowl_model.ttl");
		System.out.println("create product mapping");
		createIfcLBDProductMapping();
		System.out.println("pmapping done");

		this.lbd_general_output_model = ModelFactory.createDefaultModel();
		// writeModel(ifcowl_model, ifc_model_file_base + "_ifcowl_model.ttl");
		addNamespaces(uriBase);

		if (this.ontURI.isPresent())
			ifcOWL = new IfcOWLNameSpace(this.ontURI.get());
		else {
			System.out.println("No ifcOWL ontology available.");
			return lbd_general_output_model;
		}
		System.out.println("handle property set data");
		handlePropertySetData();
		System.out.println("execution");
		execution();
		
		convert_SOT();
		//analyse_SOT2(ifcowl_model, this.ontURI.get());
		//writeModel(lbd_general_output_model, ifc_model_file_base + "_BOT_SMLS_model.ttl");
		return lbd_general_output_model;
	}

	public Dataset convertToDataset(String ifc_filename, String uriBase) {
		Model m=convertToModel(ifc_filename, uriBase);
		Dataset lbd_dataset = DatasetFactory.create();
	    lbd_dataset.addNamedModel(uriBase, m);
		return lbd_dataset;
	}

	public static void writeModel(Model m, String target_file) {
		OutputStreamWriter fo = null;
		try {
			fo = new OutputStreamWriter(new FileOutputStream(new File(target_file)),
					Charset.forName("UTF-8").newEncoder());

			m.write(fo, "TTL");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (fo != null)
				try {
					fo.close();
				} catch (IOException e) {
				}
		}
	}

	Set<Resource> has_geometry = new HashSet<>();

	private void execution() {
		System.out.println("Conversion execution");
		IfcOWLUtils.listSites(ifcOWL, ifcowl_model).stream().map(rn -> rn.asResource()).forEach(site -> {
			Resource sio = createformattedURI(site, lbd_general_output_model, "Site");
			String guid_site = IfcOWLUtils.getGUID(site, this.ifcOWL);
			String uncompressed_guid_site = GuidCompressor.uncompressGuidString(guid_site);

			addAttrributes(lbd_general_output_model, site.asResource(), sio);
			sio.addProperty(RDF.type, BOT.site);

			addBoundingBox(sio, guid_site);

			IfcOWLUtils.listPropertysets(site, ifcOWL).stream().map(rn -> rn.asResource()).forEach(propertyset -> {
				PropertySet_B4R p_set = this.propertysets.get(propertyset.getURI());
				if (p_set != null) {
					p_set.connect(sio, uncompressed_guid_site);
				}
			});

			IfcOWLUtils.listBuildings(site, ifcOWL).stream().map(rn -> rn.asResource()).forEach(building -> {
				if (!RDFUtils.getType(building.asResource()).get().getURI().endsWith("#IfcBuilding")) {
					System.err.println("Not an #IfcBuilding"); //TODO right
					return;
				}
				System.out.println("Building: " + building.asResource().getURI());

				Resource bo = createformattedURI(building, lbd_general_output_model, "Building");
				String guid_building = IfcOWLUtils.getGUID(building, this.ifcOWL);
				String uncompressed_guid_building = GuidCompressor.uncompressGuidString(guid_building);

				addAttrributes(lbd_general_output_model, building, bo);

				bo.addProperty(RDF.type, BOT.building);
				addBoundingBox(bo, guid_building);
				sio.addProperty(BOT.hasBuilding, bo);

				IfcOWLUtils.listPropertysets(building, ifcOWL).stream().map(rn -> rn.asResource())
						.forEach(propertyset -> {
							PropertySet_B4R p_set = this.propertysets.get(propertyset.getURI());
							if (p_set != null) {
								p_set.connect(bo, uncompressed_guid_building);
							}
						});

				IfcOWLUtils.listStoreys(building, ifcOWL, this.lbd_general_output_model).stream()
						.map(rn -> rn.asResource()).forEach(storey -> {

							if (!RDFUtils.getType(storey.asResource()).get().getURI().endsWith("#IfcBuildingStorey")) {
								System.err.println("No an #IfcBuildingStorey");
								return;
							}

							Resource so = createformattedURI(storey, lbd_general_output_model, "Storey");
							String guid_storey = IfcOWLUtils.getGUID(storey, this.ifcOWL);
							String uncompressed_guid_storey = GuidCompressor.uncompressGuidString(guid_storey);

							addAttrributes(lbd_general_output_model, storey, so);

							bo.addProperty(BOT.hasStorey, so);
							addBoundingBox(so, guid_storey);
							so.addProperty(RDF.type, BOT.storey);

							IfcOWLUtils.listPropertysets(storey, ifcOWL).stream().map(rn -> rn.asResource())
									.forEach(propertyset -> {
										PropertySet_B4R p_set = this.propertysets.get(propertyset.getURI());
										if (p_set != null)
											p_set.connect(so, uncompressed_guid_storey);
									});

							IfcOWLUtils.listContained_StoreyElements(storey, ifcOWL).stream().map(rn -> rn.asResource())
									.forEach(element -> {
										if (RDFUtils.getType(element.asResource()).get().getURI().endsWith("#IfcSpace"))
											return;
										connectElement(so, element);
									});

							IfcOWLUtils.listStoreySpaces(storey.asResource(), ifcOWL).stream().forEach(space -> {
								if (!RDFUtils.getType(space.asResource()).get().getURI().endsWith("#IfcSpace"))
									return;
								System.out.println("Space: " + space.asResource().getURI());
								Resource spo = createformattedURI(space.asResource(), lbd_general_output_model,
										"Space");
								String guid_space = IfcOWLUtils.getGUID(space.asResource(), this.ifcOWL);
								String uncompressed_guid_space = GuidCompressor.uncompressGuidString(guid_space);
								addAttrributes(lbd_general_output_model, space.asResource(), spo);

								so.addProperty(BOT.hasSpace, spo);
								addBoundingBox(spo, guid_space);
								spo.addProperty(RDF.type, BOT.space);

								IfcOWLUtils.listContained_SpaceElements(space.asResource(), ifcOWL).stream()
										.map(rn -> rn.asResource()).forEach(element -> {
											connectElement(spo, element);
										});

								IfcOWLUtils.listAdjacent_SpaceElements(space.asResource(), ifcOWL).stream()
										.map(rn -> rn.asResource()).forEach(element -> {
											connectElement(spo, BOT.adjacentElement, element);
										});

								IfcOWLUtils.listPropertysets(space.asResource(), ifcOWL).stream()
										.map(rn -> rn.asResource()).forEach(propertyset -> {
											PropertySet_B4R p_set = this.propertysets.get(propertyset.getURI());
											if (p_set != null) {
												p_set.connect(spo, uncompressed_guid_space);
											}
										});
							});
						});
			});
		});

		try {
			addGeolocation2BOT();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	private void addBoundingBox(Resource sp, String guid) {

		try {
			BoundingBox bb = this.bounding_boxes.getBoundingBox(guid);
			if (bb != null && has_geometry.add(sp)) {
				Resource sp_blank = this.lbd_general_output_model.createResource();
				sp.addProperty(GEO.hasGeometry, sp_blank);
				sp_blank.addLiteral(GEO.asWKT, bb.toString());
			}
		} catch (Exception e) { // Just in case IFCOpenShell does not function under Tomcat
			e.printStackTrace();
		}

	}

	private final Map<String, String> unitmap = new HashMap<>();

	/**
	 * Collects the PropertySet data from the ifcOWL model and creates a separate
	 * Apache Jena Model that contains the converted representation of the property
	 * set content.
	 * 
	 * @param props_level             The levels described in
	 *                                https://github.com/w3c-lbd-cg/lbd/blob/gh-pages/presentations/props/presentation_LBDcall_20180312_final.pdf
	 * @param hasPropertiesBlankNodes If the nameless nodes are used.
	 */
	private void handlePropertySetData() {

		Resource ifcproject = IfcOWLUtils.getIfcProject(ifcOWL, ifcowl_model);

		RDFStep[] project_units_path = { new RDFStep(ifcOWL.getUnitsInContext_IfcProject()),
				new RDFStep(ifcOWL.getUnits_IfcUnitAssignment()) };

		System.out.println("UNITS!!");
		if (ifcproject != null) {
			List<RDFNode> units = RDFUtils.pathQuery(ifcproject, project_units_path);
			System.out.println("units size: " + units.size());
			for (RDFNode ru : units) {
				System.out.println("ru: " + ru);
				RDFStep[] namedUnit_path = { new RDFStep(ifcOWL.getUnitType_IfcNamedUnit()) };
				List<RDFNode> r1 = RDFUtils.pathQuery(ru.asResource(), namedUnit_path);

				String named_unit = null;
				for (RDFNode l1 : r1)
					named_unit = l1.asResource().getLocalName().substring(0,
							l1.asResource().getLocalName().length() - 4);

				RDFStep[] siUnit_prefix_path = { new RDFStep(ifcOWL.getPrefix_IfcSIUnit()) };
				List<RDFNode> runit_pref = RDFUtils.pathQuery(ru.asResource(), siUnit_prefix_path);

				String si_prefix = null;
				for (RDFNode lpref : runit_pref)
					si_prefix = lpref.asResource().getLocalName();

				RDFStep[] siUnit_path = { new RDFStep(ifcOWL.getName_IfcSIUnit()) };
				List<RDFNode> runit_name = RDFUtils.pathQuery(ru.asResource(), siUnit_path);
				String si_unit = null;
				for (RDFNode lname : runit_name)
					si_unit = lname.asResource().getLocalName();

				if (si_prefix != null)
					si_unit = si_prefix + " " + si_unit;

				if (named_unit != null && si_unit != null) {
					System.out.println("SI UNIT: " + named_unit + " - " + si_unit);
					unitmap.put(named_unit.toLowerCase(), si_unit);
				}
			}
		}

		IfcOWLUtils.listPropertysets(ifcOWL, ifcowl_model).stream().map(rn -> rn.asResource()).forEach(propertyset -> {

			RDFStep[] pname_path = { new RDFStep(ifcOWL.getName_IfcRoot()), new RDFStep(ifcOWL.getHasString()) };

//			if (RDFUtils.pathQuery(propertyset, pname_path).get(0).isLiteral()
//					&& RDFUtils.pathQuery(propertyset, pname_path).get(0).asLiteral().getString().startsWith("Pset")) 
			{
				System.out.println("here: !" + propertyset + "! " + RDFUtils.pathQuery(propertyset, pname_path));
				String psetName = RDFUtils.pathQuery(propertyset, pname_path).get(0).asLiteral().getString();

				final List<RDFNode> propertyset_name = new ArrayList<>();
				RDFUtils.pathQuery(propertyset, pname_path).forEach(name -> propertyset_name.add(name));
				System.out.println("pset: " + propertyset);
				RDFStep[] path = { new RDFStep(ifcOWL.getHasProperties_IfcPropertySet()) };
				RDFUtils.pathQuery(propertyset, path).forEach(propertySingleValue -> {

					RDFStep[] name_path = { new RDFStep(ifcOWL.getName_IfcProperty()),
							new RDFStep(ifcOWL.getHasString()) };
					final List<RDFNode> property_name = new ArrayList<>();
					RDFUtils.pathQuery(propertySingleValue.asResource(), name_path)
							.forEach(name -> property_name.add(name));

					if (property_name.size() == 0)
						return; // =
					final List<RDFNode> property_value = new ArrayList<>();
					final List<RDFNode> property_unit = new ArrayList<>();
					final List<RDFNode> property_type = new ArrayList<>();

					RDFStep[] unit_path = { new RDFStep(ifcOWL.getUnit_IfcPropertySingleValue()),
							new RDFStep(ifcOWL.getName_IfcSIUnit()) };
					RDFUtils.pathQuery(propertySingleValue.asResource(), unit_path)
							.forEach(unit -> property_unit.add(unit)); // if this optional property exists, it has the
																		// priority

					RDFStep[] type_path = { new RDFStep(ifcOWL.getNominalValue_IfcPropertySingleValue()),
							new RDFStep(RDF.type) };
					RDFUtils.pathQuery(propertySingleValue.asResource(), type_path)
							.forEach(type -> property_type.add(type));

					RDFStep[] value_pathS = { new RDFStep(ifcOWL.getNominalValue_IfcPropertySingleValue()),
							new RDFStep(ifcOWL.getHasString()) };
					RDFUtils.pathQuery(propertySingleValue.asResource(), value_pathS)
							.forEach(value -> property_value.add(value));

					RDFStep[] value_pathD = { new RDFStep(ifcOWL.getNominalValue_IfcPropertySingleValue()),
							new RDFStep(ifcOWL.getHasDouble()) };
					RDFUtils.pathQuery(propertySingleValue.asResource(), value_pathD)
							.forEach(value -> property_value.add(value));

					RDFStep[] value_pathI = { new RDFStep(ifcOWL.getNominalValue_IfcPropertySingleValue()),
							new RDFStep(ifcOWL.getHasInteger()) };
					RDFUtils.pathQuery(propertySingleValue.asResource(), value_pathI)
							.forEach(value -> property_value.add(value));

					RDFStep[] value_pathB = { new RDFStep(ifcOWL.getNominalValue_IfcPropertySingleValue()),
							new RDFStep(ifcOWL.getHasBoolean()) };
					RDFUtils.pathQuery(propertySingleValue.asResource(), value_pathB)
							.forEach(value -> property_value.add(value));

					RDFStep[] value_pathL = { new RDFStep(ifcOWL.getNominalValue_IfcPropertySingleValue()),
							new RDFStep(ifcOWL.getHasLogical()) };
					RDFUtils.pathQuery(propertySingleValue.asResource(), value_pathL)
							.forEach(value -> property_value.add(value));

					RDFNode pname = property_name.get(0);
					PropertySet_B4R ps = this.propertysets.get(propertyset.getURI());
					if (ps == null) {
						if (!propertyset_name.isEmpty())
							ps = new PropertySet_B4R(this.uriBase, lbd_general_output_model, this.ontology_model,
									propertyset_name.get(0).toString(), unitmap);
						else
							ps = new PropertySet_B4R(this.uriBase, lbd_general_output_model, this.ontology_model, "",
									unitmap);
						this.propertysets.put(propertyset.getURI(), ps);
					}
					if (property_type.size() > 0) {
						RDFNode ptype = property_type.get(0);
						ps.putPnameType(pname.toString(), ptype);
					}
					if (property_unit.size() > 0) {
						RDFNode punit = property_unit.get(0);
						ps.putPnameUnit(pname.toString(), punit);
					}

					if (property_value.size() > 0) {
						RDFNode pvalue = property_value.get(0);
						if (!pname.toString().equals(pvalue.toString())) {
							if (pvalue.toString().trim().length() > 0) {
								if (pvalue.isLiteral()) {
									String val = pvalue.asLiteral().getLexicalForm();
									if (val.equals("-1.#IND"))
										pvalue = ResourceFactory.createTypedLiteral(Double.NaN);
								}
								ps.putPnameValue(pname.toString(), pvalue);
								if (property_type.size() > 0)
									ps.putPsetPropertyRef(pname);
							}
						}
					} else {
						ps.putPnameValue(pname.toString(), propertySingleValue);
						ps.putPsetPropertyRef(pname);
						RDFUtils.copyTriples(0, propertySingleValue, lbd_general_output_model);
					}

				});

			}
		});
	}

	/**
	 * Adds the used RDF namespaces for the Jena Models
	 * 
	 * @param uriBase
	 * @param props_level
	 * @param hasBuildingElements
	 * @param hasBuildingProperties
	 */
	private void addNamespaces(String uriBase) {
		SMLS.addNameSpace(lbd_general_output_model);
		UNIT.addNameSpace(lbd_general_output_model);
		GEO.addNameSpace(lbd_general_output_model);

		BOT.addNameSpace(lbd_general_output_model);

		Product.addNameSpace(lbd_general_output_model);
		PROPS.addNameSpace(lbd_general_output_model);
		PROPS.addNameSpace(lbd_general_output_model);

		OPM.addNameSpacesL3(lbd_general_output_model);

		lbd_general_output_model.setNsPrefix("rdf", RDF.uri);
		lbd_general_output_model.setNsPrefix("rdfs", RDFS.uri);
		lbd_general_output_model.setNsPrefix("owl", OWL.getURI());
		lbd_general_output_model.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
		lbd_general_output_model.setNsPrefix("inst", uriBase);
		// lbd_general_output_model.setNsPrefix("geo",
		// "http://www.opengis.net/ont/geosparql#");

	}

	private void connectElement(Resource bot_resource, Resource ifc_element) {
		Optional<String> predefined_type = IfcOWLUtils.getPredefinedData(ifc_element);
		Optional<Resource> ifcowl_type = RDFUtils.getType(ifc_element);
		Optional<Resource> bot_type = Optional.empty();
		if (ifcowl_type.isPresent()) {
			bot_type = getLBDProductType(ifcowl_type.get().getLocalName());
		}

		if (bot_type.isPresent()) {
			Resource eo = createformattedURI(ifc_element, this.lbd_general_output_model, bot_type.get().getLocalName());
			String guid = IfcOWLUtils.getGUID(ifc_element, this.ifcOWL);
			String uncompressed_guid = GuidCompressor.uncompressGuidString(guid);
			addBoundingBox(eo, guid);
			Resource lbd_property_object = this.lbd_general_output_model.createResource(eo.getURI());
			if (predefined_type.isPresent()) {
				Resource product = this.lbd_general_output_model
						.createResource(bot_type.get().getURI() + "-" + predefined_type.get());
				lbd_property_object.addProperty(RDF.type, product);
			}
			lbd_property_object.addProperty(RDF.type, bot_type.get());
			eo.addProperty(RDF.type, BOT.element);
			bot_resource.addProperty(BOT.containsElement, eo);

			IfcOWLUtils.listPropertysets(ifc_element, ifcOWL).stream().map(rn -> rn.asResource())
					.forEach(propertyset -> {
						PropertySet_B4R p_set = this.propertysets.get(propertyset.getURI());
						if (p_set != null)
							p_set.connect(eo, uncompressed_guid);
					});
			addAttrributes(this.lbd_general_output_model, ifc_element, eo);

			IfcOWLUtils.listHosted_Elements(ifc_element, ifcOWL).stream().map(rn -> rn.asResource())
					.forEach(ifc_element2 -> {
						connectElement(eo, BOT.hasSubElement, ifc_element2);
					});

			IfcOWLUtils.listAggregated_Elements(ifc_element, ifcOWL).stream().map(rn -> rn.asResource())
					.forEach(ifc_element2 -> {
						connectElement(eo, BOT.hasSubElement, ifc_element2);
					});
		}
	}

	/**
	 * For a RDF LBD resource, creates the targetted object for the given property
	 * and adds a triple that connects them with the property. The literals of the
	 * elements and and the hosted elements are added as well.
	 * 
	 * @param bot_resource   The Jena Resource in the LBD output model in the Apacje
	 *                       model
	 * @param bot_property   The LBD ontology property
	 * @param ifcowl_element The corresponding ifcOWL elemeny
	 */
	private void connectElement(Resource bot_resource, Property bot_property, Resource ifcowl_element) {
		Optional<String> predefined_type = IfcOWLUtils.getPredefinedData(ifcowl_element);
		Optional<Resource> ifcowl_type = RDFUtils.getType(ifcowl_element);
		Optional<Resource> lbd_product_type = Optional.empty();
		if (ifcowl_type.isPresent()) {
			lbd_product_type = getLBDProductType(ifcowl_type.get().getLocalName());
		}

		if (lbd_product_type.isPresent()) {
			Resource lbd_object = createformattedURI(ifcowl_element, this.lbd_general_output_model,
					lbd_product_type.get().getLocalName());
			Resource lbd_property_object = this.lbd_general_output_model.createResource(lbd_object.getURI());

			if (predefined_type.isPresent()) {
				Resource product = this.lbd_general_output_model
						.createResource(lbd_product_type.get().getURI() + "-" + predefined_type.get());
				lbd_property_object.addProperty(RDF.type, product);
			}

			lbd_property_object.addProperty(RDF.type, lbd_product_type.get());
			lbd_object.addProperty(RDF.type, BOT.element);

			addAttrributes(this.lbd_general_output_model, ifcowl_element, lbd_object);
			bot_resource.addProperty(bot_property, lbd_object);
			IfcOWLUtils.listHosted_Elements(ifcowl_element, ifcOWL).stream().map(rn -> rn.asResource())
					.forEach(ifc_element2 -> {
						if (lbd_object.getLocalName().toLowerCase().contains("space"))
							System.out
									.println("hosts2: " + ifcowl_element + "-->" + ifc_element2 + " bot:" + lbd_object);
						connectElement(lbd_object, BOT.hasSubElement, ifc_element2);
					});

			IfcOWLUtils.listAggregated_Elements(ifcowl_element, ifcOWL).stream().map(rn -> rn.asResource())
					.forEach(ifc_element2 -> {
						connectElement(lbd_object, BOT.hasSubElement, ifc_element2);
					});
		} else {
			System.err.println("No type: " + ifcowl_element);
		}

	}

	Set<Resource> handledSttributes4resource = new HashSet<>();

	/**
	 * Creates and adds the literal triples from the original ifcOWL resource under
	 * the new LBD resource.
	 * 
	 * @param output_model The Apache Jena model where the conversion output is
	 *                     written
	 * @param r            The oroginal ifcOWL resource
	 * @param bot_r        The correspoinding resource in the output model. The LBD
	 *                     resource.
	 */
	private void addAttrributes(Model output_model, Resource r, Resource bot_r) {
		if (!handledSttributes4resource.add(r)) // Tests if the attributes are added already
			return;
		String guid = IfcOWLUtils.getGUID(r, this.ifcOWL);
		addBoundingBox(bot_r, guid);
		String uncompressed_guid = GuidCompressor.uncompressGuidString(guid);
		final AttributeSet_SMLS connected_attributes = new AttributeSet_SMLS(this.uriBase, output_model, this.unitmap);
		r.listProperties().forEachRemaining(s -> {
			String ps = s.getPredicate().getLocalName();
			Resource attr = s.getObject().asResource();
			Optional<Resource> atype = RDFUtils.getType(attr);
			if (ps.startsWith("tag_"))
				ps = "batid";
			final String property_string = ps; // Just to make variable final (needed in the following stream)
			if (atype.isPresent()) {
				if(atype.get().getLocalName()==null)
				   System.out.println("atype: "+atype.get());
				if (atype.get().getLocalName().equals("IfcLabel")) {
					attr.listProperties(ifcOWL.getHasString()).forEachRemaining(attr_s -> {
						if (attr_s.getObject().isLiteral()
								&& attr_s.getObject().asLiteral().getLexicalForm().length() > 0) {
							connected_attributes.putAnameValue(property_string, attr_s.getObject(), atype);
						}
					});

				} else if (atype.get().getLocalName().equals("IfcIdentifier")) {
					attr.listProperties(ifcOWL.getHasString()).forEachRemaining(
							attr_s -> connected_attributes.putAnameValue(property_string, attr_s.getObject(), atype));
				} else {
					attr.listProperties(ifcOWL.getHasString()).forEachRemaining(
							attr_s -> connected_attributes.putAnameValue(property_string, attr_s.getObject(), atype));
					attr.listProperties(ifcOWL.getHasInteger()).forEachRemaining(
							attr_s -> connected_attributes.putAnameValue(property_string, attr_s.getObject(), atype));
					attr.listProperties(ifcOWL.getHasDouble()).forEachRemaining(
							attr_s -> connected_attributes.putAnameValue(property_string, attr_s.getObject(), atype));
					attr.listProperties(ifcOWL.getHasBoolean()).forEachRemaining(
							attr_s -> connected_attributes.putAnameValue(property_string, attr_s.getObject(), atype));
				}

			}
		});
		connected_attributes.connect(bot_r, uncompressed_guid);
	}

	/**
	 * Creates URIs for the elements in the output graph. The IfcRoot elements (that
	 * have a GUID) are given URI that contais the guid in the standard uncompressed
	 * format.
	 * 
	 * The uncompressed GUID form is created using the implementation by Tulke & Co.
	 * (The OPEN IFC JAVA TOOLBOX)
	 * 
	 * @param r            A ifcOWL RDF node in a Apache Jena RDF store.
	 * @param m            The Apache Jena RDF Store for the output.
	 * @param product_type The LBD product type to be shown on the URI
	 * @return
	 */
	private Resource createformattedURI(Resource r, Model m, String product_type) {
		String guid = IfcOWLUtils.getGUID(r, this.ifcOWL);
		if (guid == null) {
			String localName = r.getLocalName();
			if (localName.startsWith("IfcPropertySingleValue")) {
				if (localName.lastIndexOf('_') > 0)
					localName = localName.substring(localName.lastIndexOf('_') + 1);
				Resource uri = m.createResource(this.uriBase + "propertySingleValue_" + localName);
				System.out.println("sameas 1: " + r);
				uri.addProperty(OWL.sameAs, r);
				return uri;
			}
			if (localName.toLowerCase().startsWith("ifc"))
				localName = localName.substring(3);
			Resource uri = m.createResource(this.uriBase + product_type.toLowerCase() + "_" + localName);
			System.out.println("sameas 2: " + r);
			uri.addProperty(OWL.sameAs, r);
			return uri;
		} else {
			Resource guid_uri = m.createResource(
					this.uriBase + product_type.toLowerCase() + "_" + GuidCompressor.uncompressGuidString(guid));
			System.out.println("sameas 3: " + r);
			guid_uri.addProperty(OWL.sameAs, r);
			return guid_uri;
		}
	}

	private Resource getformattedURI(Resource r, Model m, String product_type) {
		String guid = IfcOWLUtils.getGUID(r, this.ifcOWL);
		if (guid == null) {
			Resource uri = m.getResource(this.uriBase + product_type + "/" + r.getLocalName());
			return uri;
		} else {
			Resource guid_uri = m
					.getResource(this.uriBase + product_type + "/" + GuidCompressor.uncompressGuidString(guid));
			return guid_uri;
		}
	}

	/**
	 * Returns list of all RDF nodes that have an matching element type returned by
	 * getLBDProductType(String ifcType)
	 * 
	 * on the RDF graph.
	 * 
	 * @return the list of the matching nodes
	 */
	private List<Resource> listElements() {
		final List<Resource> ret = new ArrayList<>();
		ifcowl_model.listStatements().filterKeep(t1 -> t1.getPredicate().equals(RDF.type)).filterKeep(t2 -> {
			Optional<Resource> product_type = getLBDProductType(t2.getObject().asResource().getLocalName());
			return product_type.isPresent();
		}).mapWith(t1 -> t1.getSubject()).forEachRemaining(s -> ret.add(s));

		return ret;
	}

	/**
	 * This used the ifcowl_product_map map and returns one mapped class in a Linked
	 * Building Data ontology, if specified.
	 * 
	 * @param ifcType The IFC entity class
	 * @return The corresponding class Resource in a LBD ontology
	 */
	public Optional<Resource> getLBDProductType(String ifcType) {
		List<Resource> ret = ifcowl_product_map.get(ifcType);
		if (ret == null) {
			return Optional.empty();
		} else if (ret.size() > 1) {
			// System.out.println("many " + ifcType);
			return Optional.empty();
		} else if (ret.size() > 0)
			return Optional.of(ret.get(0));
		else
			return Optional.empty();
	}

	/**
	 * Fills in the ifcowl_product_map map using the seealso ontology statemets at
	 * the Apache Jena RDF ontology model on the memory.
	 * 
	 * Uses also RDFS.subClassOf so that subclasses are included.
	 */
	private void createIfcLBDProductMapping() {
		StmtIterator si = ontology_model.listStatements();
		while (si.hasNext()) {
			Statement product_BE_ontology_statement = si.next();
			if (product_BE_ontology_statement.getPredicate().toString().toLowerCase().contains("seealso")) {
				if (product_BE_ontology_statement.getObject().isLiteral())
					continue;
				if (!product_BE_ontology_statement.getObject().isResource())
					continue;
				Resource ifcowl_class = product_BE_ontology_statement.getObject().asResource();

				// This adds the seeAlso mapping directly: The base IRI is removed so that the
				// mapping is independent of various IFC versions
				List<Resource> resource_list = ifcowl_product_map.getOrDefault(ifcowl_class.getLocalName(),
						new ArrayList<Resource>());
				ifcowl_product_map.put(ifcowl_class.getLocalName(), resource_list);
				resource_list.add(product_BE_ontology_statement.getSubject());
				// System.out.println("added to resource_list : " +
				// product_BE_ontology_statement.getSubject());
			}
		}
		StmtIterator so = ontology_model.listStatements();
		while (so.hasNext()) {
			Statement product_BE_ontology_statement = so.next();
			if (product_BE_ontology_statement.getPredicate().toString().toLowerCase().contains("seealso")) {
				if (product_BE_ontology_statement.getObject().isLiteral())
					continue;
				if (!product_BE_ontology_statement.getObject().isResource())
					continue;
				Resource ifcowl_class = product_BE_ontology_statement.getObject().asResource();
				Resource mapped_ifcowl_class = ontology_model
						.getResource(this.ontURI.get() + ifcowl_class.getLocalName());
				StmtIterator subclass_statement_iterator = ontology_model
						.listStatements(new SimpleSelector(null, RDFS.subClassOf, mapped_ifcowl_class));
				while (subclass_statement_iterator.hasNext()) {
					Statement su = subclass_statement_iterator.next();
					Resource ifcowl_subclass = su.getSubject();
					if (ifcowl_product_map.get(ifcowl_subclass.getLocalName()) == null) {
						List<Resource> r_list = ifcowl_product_map.getOrDefault(ifcowl_subclass.getLocalName(),
								new ArrayList<Resource>());
						ifcowl_product_map.put(ifcowl_subclass.getLocalName(), r_list);
						// System.out.println(
						// ifcowl_subclass.getLocalName() + " ->> " +
						// product_BE_ontology_statement.getSubject());
						r_list.add(product_BE_ontology_statement.getSubject());
					}
				}

			}
		}

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

	/**
	 * 
	 * Reads in a Turtle - Terse RDF Triple Language (TTL) formatted ontology file:
	 * Turtle - Terse RDF Triple Language: https://www.w3.org/TeamSubmission/turtle/
	 * 
	 * The extra lines make sure that the files are found if run under Eclipse IDE
	 * or as a runnable Java Archive file (JAR).
	 * 
	 * Eclipse: https://www.eclipse.org/
	 * 
	 * @param model         An Apache Jena model: RDF store run on memory.
	 * @param ontology_file An Apache Jena ontokigy model: RDF store run on memory
	 *                      containing an ontology engine. If no user interface is
	 *                      present, adding messages to the channel does nothing.
	 * 
	 */
	public static void readInOntologyTTL(Model model, String ontology_file) {

		InputStream in = null;
		try {
			in = IFCtoLBDConverter_BIM4Ren.class.getResourceAsStream("/" + ontology_file);
			if (in == null) {
				try {
					in = IFCtoLBDConverter_BIM4Ren.class.getResourceAsStream("/resources/" + ontology_file);
					if (in == null)
						in = IFCtoLBDConverter_BIM4Ren.class.getResourceAsStream(ontology_file);
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}
			RDFDataMgr.read(model, in, Lang.TTL);
			in.close();

		} catch (Exception e) {
			System.out.println("missing file: " + ontology_file);
			e.printStackTrace();
		}

	}

	/**
	 * This internal method reads in all the associated ontologies so that ontology
	 * inference can ne used during the conversion.
	 * 
	 * @param ifc_file the absolute path (For example: c:\ifcfiles\ifc_file.ifc) for
	 *                 the IFC file
	 */
	private void readInOntologies(String ifc_file) {
		IfcOWLUtils.readIfcOWLOntology(ifc_file, ontology_model);
		IfcOWLUtils.readIfcOWLOntology(ifc_file, ifcowl_model);

		readInOntologyTTL(ontology_model, "prod.ttl");
		readInOntologyTTL(ontology_model, "beo_ontology.ttl");
		readInOntologyTTL(ontology_model, "prod_furnishing.ttl");
		readInOntologyTTL(ontology_model, "mep_ontology.ttl");

		readInOntologyTTL(ontology_model, "psetdef.ttl");
		List<String> files = FileUtils.getListofFiles("pset", ".ttl");
		for (String file : files) {
			//file = file.substring(file.indexOf("Pset"));
			file = file.replaceAll("\\\\", "/");
			readInOntologyTTL(ontology_model, file);
			System.out.println("read ontology file : " + file);
		}
	}

	/**
	 * 
	 * Adds Geolocation triples to the RDF model. Ontology: http://www.opengis.net
	 */
	private void addGeolocation2BOT() {

		IFC_Geolocation c = new IFC_Geolocation();
		String wkt_point = c.addGeolocation(ifcowl_model);

		IfcOWLUtils.listSites(ifcOWL, ifcowl_model).stream().map(rn -> rn.asResource()).forEach(site -> {
			// Create a resource and add to bot model (resource, model, string)
			Resource sio = createformattedURI(site, lbd_general_output_model, "Site");

			// Create a resource geosparql:Feature;
			Resource geof = lbd_general_output_model.createResource("http://www.opengis.net/ont/geosparql#Feature");
			// Add geosparl:Feature as a type to site;
			sio.addProperty(RDF.type, geof);
			// Create a resource geosparql:hasGeometry;
			Property geo_hasGeometry = lbd_general_output_model
					.createProperty("http://www.opengis.net/ont/geosparql#hasGeometry");

			// For the moment we will use a seperate graph for geometries, to "encourage"
			// people to not link to geometries
			// This could also be done using blanknodes, although, hard to maintain
			// provenance if required in future versions.

			String wktLiteralID = "urn:bot:geom:pt:";
			String guid_site = IfcOWLUtils.getGUID(site, this.ifcOWL);
			String uncompressed_guid_site = GuidCompressor.uncompressGuidString(guid_site);
			String uncompressed_wktLiteralID = wktLiteralID + uncompressed_guid_site;

			// Create a resource <urn:bot:geom:pt:guid>
			Resource rr = lbd_general_output_model.createResource(uncompressed_wktLiteralID);
			sio.addProperty(geo_hasGeometry, rr);

			// Create a property asWKT
			Property geo_asWKT = lbd_general_output_model.createProperty("http://www.opengis.net/ont/geosparql#asWKT");
			// add a data type
			RDFDatatype rtype = WktLiteral.wktLiteralType;
			TypeMapper.getInstance().registerDatatype(rtype);
			// add a typed wkt literal
			Literal l = lbd_general_output_model.createTypedLiteral(wkt_point, rtype);

			rr.addProperty(geo_asWKT, l);

		});

	}
	
	
	private Map<String, Resource> ifcowl_sot_map = new HashMap<>();


	public void convert_SOT() {
		long start_time=System.currentTimeMillis();
		IfcOWLUtils.listIfcRelConnectsPorts(ifcOWL, ifcowl_model).stream().map(rn -> rn.asResource()).forEach(cp ->
		{
			System.out.println("cp: "+cp);
			RDFStep[] connected_flowsegment_path1 = { new RDFStep(ifcOWL.getRelatingPort_IfcRelConnectsPorts()),new InvRDFStep(ifcOWL.getRelatingPort_IfcRelConnectsPortToElement()),new RDFStep(ifcOWL.getRelatedElement_IfcRelConnectsPortToElement()) };
			RDFStep[] connected_flowsegment_path2 = { new RDFStep(ifcOWL.getRelatedPort_IfcRelConnectsPorts()),new InvRDFStep(ifcOWL.getRelatingPort_IfcRelConnectsPortToElement()),new RDFStep(ifcOWL.getRelatedElement_IfcRelConnectsPortToElement()) };

			List<RDFNode> flowsegments1 = RDFUtils.pathQuery(cp, connected_flowsegment_path1);
			List<RDFNode> flowsegments2 = RDFUtils.pathQuery(cp, connected_flowsegment_path2);
			
			if(flowsegments1.size()>0 && flowsegments2.size()>0)
			{
				Resource ifc1=flowsegments1.get(0).asResource();
				Resource ifc2=flowsegments2.get(0).asResource();
				
				Resource s1=ifcowl_sot_map.get(ifc1.getURI());
				if(s1==null)
				{
					s1=lbd_general_output_model.createResource();
					ifcowl_sot_map.put(ifc1.getURI(),s1);
				}
				
				Resource s2=ifcowl_sot_map.get(ifc2.getURI());
				if(s2==null)
				{
					s2=lbd_general_output_model.createResource();
					ifcowl_sot_map.put(ifc2.getURI(),s2);
				}
				s1.addProperty(RDF.type,SOT.SystemComponent);
				s1.addProperty(OWL.sameAs, ifc1);
				
				s2.addProperty(RDF.type,SOT.SystemComponent);
				s2.addProperty(OWL.sameAs, ifc2);
				s1.addProperty(SOT.connectedWith, s2);
			}

		});
		long stop_time=System.currentTimeMillis();
		System.out.println("Time in milliseconds: "+(stop_time-start_time));
	}
	
	public Model analyse_SOT1(Model ifcowl_model, String ontology_URI) {

		InputStream SOT_inputStream = this.getClass().getResourceAsStream("/alignment/Core/SOT/SOT---IFC.ttl");
		String rule_txt = null;
		try {

			InputStream IFCtoSOT_inputStream = this.getClass().getResourceAsStream("/alignment/Core/SOT/IFCtoSOT.swrl");
			rule_txt = IOUtils.toString(IFCtoSOT_inputStream, StandardCharsets.UTF_8.name());
			Path tempFile = Files.createTempFile(null, null);
			rule_txt = setIFC_NS(rule_txt, ontology_URI);
			Files.write(tempFile, rule_txt.getBytes(StandardCharsets.UTF_8));

			List<Rule> rules = Rule.rulesFromURL(tempFile.toFile().getAbsolutePath());
			System.out.println("rules: " + rules.size());
			GenericRuleReasoner reasoner = new GenericRuleReasoner(rules);
			System.out.println("rules 1");
			
			Path ontology_tempFile = Files.createTempFile(null, null);
			//Model local_ontology_model=ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM_TRANS_INF); // Does not work
			Model local_ontology_model=ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM_RDFS_INF);  //RDFS_MEM_RDFS_INF
			
			@SuppressWarnings("unused")
			Resource sot_System= local_ontology_model.createResource("http://www.bim4ren.eu/sot#System");
			@SuppressWarnings("unused")
			Resource sot_SystemComponent= local_ontology_model.createResource("http://www.bim4ren.eu/sot#SystemComponent");
			@SuppressWarnings("unused")
			Resource sot_TransportElement= local_ontology_model.createResource("http://www.bim4ren.eu/sot#TransportElement");
			@SuppressWarnings("unused")
			Resource sot_TerminalElement= local_ontology_model.createResource("http://www.bim4ren.eu/sot#TerminalElement");
			@SuppressWarnings("unused")
			Property sot_hasElements = local_ontology_model.createProperty("http://www.bim4ren.eu/sot#hasElements");
			
							

			//local_ontology_model.add(")
			
			
			//Model local_ontology_model=ModelFactory.createRDFSModel(ifcowl_model);  
			
			String ontology_txt = IOUtils.toString(SOT_inputStream, StandardCharsets.UTF_8.name());
			ontology_txt = setIFC_NS(ontology_txt, ontology_URI);
			Files.write(ontology_tempFile, ontology_txt.getBytes(StandardCharsets.UTF_8));
			@SuppressWarnings("unused")
			InputStream SOT_tmpinputStream = new FileInputStream(ontology_tempFile.toFile());
			RDFDataMgr.read(local_ontology_model, SOT_inputStream, Lang.TTL);
			//reasoner.bindSchema(ontology);
			
			
			local_ontology_model.add(ifcowl_model);
			/*
			String query = 
					"PREFIX owl: <http://www.w3.org/2002/07/owl#> \r\n" + 
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \r\n" + 
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \r\n" + 
					"PREFIX sot: <http://www.bim4ren.eu/sot#> \r\n" + 
					"PREFIX list: <https://w3id.org/list#> \r\n" + 
					"PREFIX ifc: <http://standards.buildingsmart.org/IFC/DEV/IFC2x3/TC1/OWL#> \r\n" + 
					"SELECT ?elt1  \n" + 
					"       ?elt2 \n" + 
					"WHERE\r\n" + 
					"  {\r\n" + 
					"   ?c rdf:type ifc:IfcRelConnectsPorts .\n" +
					"   ?c ifc:relatingPort_IfcRelConnectsPorts ?p1 .\n" +
					"   ?c ifc:relatedPort_IfcRelConnectsPorts ?p2 .\n" + 
					"   ?elt1 rdf:type sot:SystemComponent .\n" +
					"   ?elt2 rdf:type sot:SystemComponent .\n" + 
					"	?n1 ifc:relatingObject_IfcRelNests ?elt1 .\n" +
					"   ?n1 ifc:relatedObjects_IfcRelNests ?list1 .\n" +
					"   ?p1 list:isIn ?list1 .\n" + 
					"	?n2 ifc:relatingObject_IfcRelNests ?elt2 .\n" +
					"   ?n2 ifc:relatedObjects_IfcRelNests ?list2 .\n" +
					"   ?p2 list:isIn ?list2 .\n" + 
					"	?rel ifc:relatingPort_IfcRelConnectsPorts ?p1 .\n" +
				    "   ?rel ifc:relatedPort_IfcRelConnectsPorts ?p2 .\n" +
					"  }";
			
*/
			String query = 
					"PREFIX owl: <http://www.w3.org/2002/07/owl#> \r\n" + 
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \r\n" + 
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \r\n" + 
					"PREFIX sot: <http://www.bim4ren.eu/sot#> \r\n" + 
					"PREFIX list: <https://w3id.org/list#> \r\n" + 
					"PREFIX ifc: <http://standards.buildingsmart.org/IFC/DEV/IFC2x3/TC1/OWL#> \r\n" + 
					"SELECT ?c ?p1 ?p2  \n" + 
					"WHERE\r\n" + 
					"  {\r\n" + 
					"   ?c rdf:type ifc:IfcRelConnectsPorts .\n" +
					"   ?c ifc:relatingPort_IfcRelConnectsPorts ?p1 .\n" +
					"   ?c ifc:relatedPort_IfcRelConnectsPorts ?p2 .\n" + 
					"   ?elt1 rdf:type ifc:IfcDistributionElement .\n" +
					"   ?elt2 rdf:type ifc:IfcDistributionElement .\n" +
					"	?n1 ifc:relatingObject_IfcRelNests ?elt1 .\n" +
					//"   ?n1 ifc:relatedObjects_IfcRelNests ?list1 .\n" +
					//"   ?p1 list:isIn ?list1 .\n" + 
					"	?n2 ifc:relatingObject_IfcRelNests ?elt2 .\n" +
					//"   ?n2 ifc:relatedObjects_IfcRelNests ?list2 .\n" +
					//"   ?p2 list:isIn ?list2 .\n" + 
					//"	?rel ifc:relatingPort_IfcRelConnectsPorts ?p1 .\n" +
				    //"   ?rel ifc:relatedPort_IfcRelConnectsPorts ?p2 .\n" +					
					"  }";

			System.out.println("rules 2");
			InfModel inference_model = ModelFactory.createInfModel(reasoner, local_ontology_model);
			
			 QueryExecution exec =  QueryExecutionFactory.create(QueryFactory.create(query), new
					 DatasetImpl(inference_model));
			 ResultSet results = exec.execSelect();
			 System.out.println("results: "+results.hasNext());
			 for ( ; results.hasNext() ; ) {
			      QuerySolution soln = results.nextSolution() ;
			      System.out.println("Solution: "+soln);
			    }
			 
			Model result=ModelFactory.createDefaultModel();
			result.add(inference_model);
			writeModel(result,  "c:\\temp\\reasoned_ifcowl_ont_model.ttl"); 
			
			/*final Model deductions = ModelFactory.createDefaultModel();
			System.out.println("rules 3");
			inf.listStatements().forEachRemaining(st -> {
				System.out.println("rules statements: " + st);
				if (!model.contains(st))
					deductions.add(st);
			});*/
			System.out.println("rules done");

		} catch (Exception e) {
			e.printStackTrace();
		}

		// the results are given with a fresh new model
		return ModelFactory.createDefaultModel();
	}

	public void analyse_SOT2(Model ifcowl_model, String ontology_URI) {
		long start_time=System.currentTimeMillis();
		InputStream SOT_inputStream = this.getClass().getResourceAsStream("/alignment/Core/SOT/SOT---IFC.ttl");
		String rule_txt = null;
		try {

			InputStream IFCtoSOT_inputStream = this.getClass().getResourceAsStream("/alignment/Core/SOT/IFCtoSOT.swrl");
			rule_txt = IOUtils.toString(IFCtoSOT_inputStream, StandardCharsets.UTF_8.name());
			Path tempFile = Files.createTempFile(null, null);
			rule_txt = setIFC_NS(rule_txt, ontology_URI);
			Files.write(tempFile, rule_txt.getBytes(StandardCharsets.UTF_8));

			List<Rule> rules = Rule.rulesFromURL(tempFile.toFile().getAbsolutePath());
			System.out.println("rules: " + rules.size());
			GenericRuleReasoner reasoner = new GenericRuleReasoner(rules);
			System.out.println("rules 1");
			
			Path ontology_tempFile = Files.createTempFile(null, null);
			Model local_ontology_model=ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM_RDFS_INF);  //RDFS_MEM_RDFS_INF   OWL_LITE_MEM_RDFS_INF
			String ontology_txt = IOUtils.toString(SOT_inputStream, StandardCharsets.UTF_8.name());
			ontology_txt = setIFC_NS(ontology_txt, ontology_URI);
			Files.write(ontology_tempFile, ontology_txt.getBytes(StandardCharsets.UTF_8));
			InputStream SOT_tmpinputStream = new FileInputStream(ontology_tempFile.toFile());
			RDFDataMgr.read(local_ontology_model, SOT_inputStream, Lang.TTL);
			//reasoner.bindSchema(ontology);
			local_ontology_model.add(ifcowl_model);
			System.out.println("rules 2");
			final Model m = ModelFactory.createDefaultModel();
			InfModel inference_model = ModelFactory.createInfModel(reasoner, local_ontology_model);
			inference_model.listStatements().forEachRemaining(x->{
				if(x.toString().contains("isIn"))
				   m.add(x);
			});
			//writeModel(inference_model,  "c:\\temp\\reasoned_ifcowl_ont_model.ttl"); 
			System.out.println("rules done");
			long stop_time=System.currentTimeMillis();
			System.out.println("Time in milliseconds: "+(stop_time-start_time));
			m.write(System.out, "TTL");
			System.exit(1);

		} catch (Exception e) {
			e.printStackTrace();
		}

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
