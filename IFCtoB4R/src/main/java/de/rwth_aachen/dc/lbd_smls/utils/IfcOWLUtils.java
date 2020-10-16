package de.rwth_aachen.dc.lbd_smls.utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import be.ugent.IfcSpfReader;
import de.rwth_aachen.dc.lbd_smls.ns.IfcOWLNameSpace;
import de.rwth_aachen.dc.lbd_smls.utils.rdfpath.InvRDFStep;
import de.rwth_aachen.dc.lbd_smls.utils.rdfpath.RDFStep;


/*
 *  Copyright (c) 2017,2020 Jyrki Oraskari (Jyrki.Oraskari@gmail.f)
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

public class IfcOWLUtils {
	public static String getGUID(Resource r, IfcOWLNameSpace ifcOWL) {
		StmtIterator i = r.listProperties(ifcOWL.getGuid());
		if (i.hasNext()) {
			Statement s = i.next();
			String guid = s.getObject().asResource().getProperty(ifcOWL.getHasString()).getObject().asLiteral()
					.getLexicalForm();
			return guid;
		}
		
		return null;
	}
	
	public static String getGUID(Resource r, IfcOWLNameSpace ifcOWL, Model lbd_general_output_model4_errors) {
		StmtIterator i = r.listProperties(ifcOWL.getGuid());
		if (i.hasNext()) {
			Statement s = i.next();
			String guid = s.getObject().asResource().getProperty(ifcOWL.getHasString()).getObject().asLiteral()
					.getLexicalForm();
			return guid;
		}
		addError(lbd_general_output_model4_errors,r+" is missing: "+ifcOWL.getGuid()+" + "+ifcOWL.getHasString());	
		return null;
	}

	// Solution proposed by Simon Steyskal 2018
	private static RDFStep[] getNextLevelPath(IfcOWLNameSpace ifcOWL) {
		System.out.println("ifcoWL: "+ifcOWL.getIfcURI());
		if (ifcOWL.getIfcURI().toUpperCase().indexOf("IFC2X3") != -1) {  // fixed by JO 2020
			RDFStep[] path = { new InvRDFStep(ifcOWL.getRelatingObject_IfcRelDecomposes()),
					new RDFStep(ifcOWL.getRelatedObjects_IfcRelDecomposes()) };
			return path;
		} else {
			RDFStep[] path = { new InvRDFStep(ifcOWL.getProperty("relatingObject_IfcRelAggregates")),
					new RDFStep(ifcOWL.getProperty("relatedObjects_IfcRelAggregates")) };
			return path;
		}
	}

	public static List<RDFNode> listSites(IfcOWLNameSpace ifcOWL, Model ifcowl_model) {
		RDFStep[] path = { new InvRDFStep(RDF.type) };
		return RDFUtils.pathQuery(ifcowl_model.getResource(ifcOWL.getIfcSite()), path);
	}

	public static List<RDFNode> listSites(IfcOWLNameSpace ifcOWL, Model ifcowl_model,Model lbd_general_output_model4_errors ) {
		RDFStep[] path = { new InvRDFStep(RDF.type) };
		List<RDFNode> ret= RDFUtils.pathQuery(ifcowl_model.getResource(ifcOWL.getIfcSite()), path);
		if(ret==null)
		{
			addError(lbd_general_output_model4_errors,"model is missing an "+ifcOWL.getIfcSite());	
		}
		return ret;
	}


	
	public static List<RDFNode> listBuilding(IfcOWLNameSpace ifcOWL, Model ifcowl_model) {
		RDFStep[] path = { new InvRDFStep(RDF.type) };
		return RDFUtils.pathQuery(ifcowl_model.getResource(ifcOWL.getIfcBuilding()), path);
	}

	public static List<RDFNode> listBuilding(IfcOWLNameSpace ifcOWL, Model ifcowl_model,Model lbd_general_output_model4_errors ) {
		RDFStep[] path = { new InvRDFStep(RDF.type) };
		List<RDFNode> ret=RDFUtils.pathQuery(ifcowl_model.getResource(ifcOWL.getIfcBuilding()), path);
		
		if(ret==null)
		{
			addError(lbd_general_output_model4_errors,"model is missing an "+ifcOWL.getIfcBuilding());	
		}
		return ret;
	}
	
	/**
	 * 
	 * <font color="green"> <b>Site building (bot:hasBuilding)</b>
	 * (inst:IfcBuilding_xx)&lt;-[ifcowl:relatedObjects_IfcRelDecomposes]-(inst:IfcRelAggregates_xx)-[ifcowl:relatingObject_IfcRelDecomposes]-&gt;(inst:IfcSite_xx)
	 * </font>
	 * 
	 * @param site   Apache Jena Resource RDF node that refers to an #IfcSite ifcOWL
	 *               element
	 * @param ifcOWL The ifcOWL namespace element.
	 * @return The list of all #IfcBuilding ifcOWL elements under the site element
	 */
	public static List<RDFNode> listBuildings(Resource site, IfcOWLNameSpace ifcOWL) {
		System.out.println("Site: "+site.toString());
		List<RDFNode> buildings = RDFUtils.pathQuery(site, getNextLevelPath(ifcOWL));
		if (buildings == null || buildings.size() == 0)
			System.err.println("No Buildings! 1");
		return buildings;
	}

	public static List<RDFNode> listBuildings(Resource site, IfcOWLNameSpace ifcOWL,Model lbd_general_output_model4_errors) {
		// System.out.println("Site: "+site.toString());
		List<RDFNode> buildings = RDFUtils.pathQuery(site, getNextLevelPath(ifcOWL));
		if (buildings == null || buildings.size() == 0)
		{
			System.err.println("No Buildings! 2");
			addError(lbd_general_output_model4_errors,"Model has no buildings");	
		}
		return buildings;
	}
	/**
	 * 
	 * <font color="green"> <b>Building storeys (bot:hasStorey)</b>
	 * (inst:IfcBuildingStorey_xx)&lt;-[ifcowl:relatedObjects_IfcRelDecomposes]-(inst:IfcRelAggregates_xx)-[ifcowl:relatingObjects_IfcRelDecomposes]-&gt;(inst:IfcBuilding_xx)
	 * </font>
	 * 
	 * @param building Apache Jena Resource RDF node that refers to an #IfcBuilding
	 *                 ifcOWL element
	 * @param ifcOWL   The ifcOWL namespace element.
	 * @return The list of all #IfcBuildingStorey ifcOWL elements under the building
	 *         element
	 */

	public static List<RDFNode> listStoreys(Resource building, IfcOWLNameSpace ifcOWL) {
		return RDFUtils.pathQuery(building, getNextLevelPath(ifcOWL));
	}

	public static List<RDFNode> listStoreys(Resource building, IfcOWLNameSpace ifcOWL,Model lbd_general_output_model4_errors) {
		List<RDFNode> ret=RDFUtils.pathQuery(building, getNextLevelPath(ifcOWL));
		if(ret==null)
		{
			addError(lbd_general_output_model4_errors,"model is missing ifcStorey elements");	
		}
		return ret;
	}

	
	/**
	 * 
	 * <font color="green"> <b>Storeys spaces (bot:hasSpace)</b>
	 * (inst:IfcSpace_xx)&lt;-[ifcowl:relatedObjects_IfcRelDecomposes]-(inst:IfcRelAggregates_xx)-[ifcowl:relatingObjects_IfcRelDecomposes]-&gt;(inst:IfcBuildingStorey_xx)
	 * <P>
	 * <b>OR</b>
	 * <P>
	 * (inst:IfcSpace_xx)-[ifcowl:objectPlacement_IfcProduct]-&gt;(inst:IfcLocalPlacement_xx)-[ifcowl:placementRelTo_IfcLocalPlacement]-&gt;(inst:IfcLocalPlacement_yy)&lt;-[ifcowl:
	 * objectPlacement_IfcProduct]-(inst:IfcBuildingStorey_xx)
	 * 
	 * </font>
	 * 
	 * @param storey Apache Jena Resource RDF node that refers to an
	 *               #IfcBuildingStorey ifcOWL element
	 * @param ifcOWL The ifcOWL namespace element.
	 * @return The list of all corresponding space ifcOWL elements under the storey
	 *         element
	 */
	public static List<RDFNode> listStoreySpaces(Resource storey, IfcOWLNameSpace ifcOWL) {
		List<RDFNode> ret;

		ret = RDFUtils.pathQuery(storey, getNextLevelPath(ifcOWL));
		RDFStep[] path2 = { new RDFStep(ifcOWL.getProperty("objectPlacement_IfcProduct")),
				new InvRDFStep(ifcOWL.getProperty("placementRelTo_IfcLocalPlacement")),
				new InvRDFStep(ifcOWL.getProperty("objectPlacement_IfcProduct")) };
		ret.addAll(RDFUtils.pathQuery(storey, path2));

		return ret;
	}

	/**
	 * 
	 * <font color="green"> <b>Storeys elements (bot:containsElement)</b>
	 * (inst:IfcDoor_xx)&lt;-[ifcowl:relatedElements_IfcRelContainedInSpatialStructure]-(inst:IfcRelContainedInSpatialStructure_xx)-[ifcowl:relatingStructure_IfcRelContainedInSpatialStructure]-&gt;(inst:IfcBuildingStorey_xx)
	 *
	 * <P>
	 * <b>OR</b>
	 * <P>
	 * 
	 * (inst:IfcDoor_xx)-[ifcowl:objectPlacement_IfcProduct]-&gt;(inst:IfcLocalPlacement_xx)-[ifcowl:placementRelTo_IfcLocalPlacement]-&gt;(inst:IfcLocalPlacement_yy)&lt;-[ifcowl:
	 * objectPlacement_IfcProduct]-(inst:IfcBuildingStorey_xx)
	 * 
	 * </font>
	 * 
	 * @param storey Apache Jena Resource RDF node that refers to an
	 *               #IfcBuildingStorey ifcOWL element
	 * @param ifcOWL The ifcOWL namespace element.
	 * @return The list of all containded elements under the storey element
	 */

	public static List<RDFNode> listContained_StoreyElements(Resource storey, IfcOWLNameSpace ifcOWL) {
		List<RDFNode> ret;

		RDFStep[] path1 = { new InvRDFStep(ifcOWL.getProperty("relatingStructure_IfcRelContainedInSpatialStructure")),
				new RDFStep(ifcOWL.getProperty("relatedElements_IfcRelContainedInSpatialStructure")) };
		ret = RDFUtils.pathQuery(storey, path1);
		RDFStep[] path2 = { new RDFStep(ifcOWL.getProperty("objectPlacement_IfcProduct")),
				new InvRDFStep(ifcOWL.getProperty("placementRelTo_IfcLocalPlacement")),
				new InvRDFStep(ifcOWL.getProperty("objectPlacement_IfcProduct")) };
		ret.addAll(RDFUtils.pathQuery(storey, path2));
		return ret;
	}

	/**
	 * 
	 * <font color="green"> <b>Spaces elements (bot:containsElement)</b>
	 * (inst:IfcFurnishingElement_xx)&lt;-[ifcowl:relatedElements_IfcRelContainedInSpatialStructure]-(inst:IfcRelContainedInSpatialStructure_xx)-[ifcowl:relatingStructure_IfcRelContainedInSpatialStructure]-&gt;(inst:IfcSpace_xx)
	 * 
	 * </font>
	 * 
	 * @param space  Apache Jena Resource RDF node that refers to an ifcOWL space
	 *               element
	 * @param ifcOWL The ifcOWL namespace element.
	 * @return The list of all containded elements under the space
	 */

	public static List<RDFNode> listContained_SpaceElements(Resource space, IfcOWLNameSpace ifcOWL) {
		List<RDFNode> ret;

		RDFStep[] path1 = { new InvRDFStep(ifcOWL.getProperty("relatingStructure_IfcRelContainedInSpatialStructure")),
				new RDFStep(ifcOWL.getProperty("relatedElements_IfcRelContainedInSpatialStructure")) };
		ret = RDFUtils.pathQuery(space, path1);
		return ret;
	}

	/**
	 * 
	 * <font color="green"> <b>Spaces elements (bot:adjacentElement)</b>
	 * (inst:IfcDoor_xx)&lt;-[ifcowl:relatedBuildingElement_IfcRelSpaceBoundary]-(inst:IfcRelSpaceBoundary_xx)-[ifcowl:relatingSpace_IfcRelSpaceBoundary]-&gt;(inst:IfcSpace_xx)
	 * 
	 * </font>
	 * 
	 * @param space  Apache Jena Resource RDF node that refers to an ifcOWL space
	 * @param ifcOWL The ifcOWL namespace element.
	 * @return The list of all containded elements under the space
	 */
	public static List<RDFNode> listAdjacent_SpaceElements(Resource space, IfcOWLNameSpace ifcOWL) {
		List<RDFNode> ret;

		RDFStep[] path1 = { new InvRDFStep(ifcOWL.getProperty("relatingSpace_IfcRelSpaceBoundary")),
				new RDFStep(ifcOWL.getProperty("relatedBuildingElement_IfcRelSpaceBoundary")) };
		ret = RDFUtils.pathQuery(space, path1);
		return ret;
	}

	/**
	 * 
	 * <font color="green"> <b>Element element (bot:hostsElement)</b>
	 * (inst:IfcDoor_xx)&lt;-[ifcowl:relatedBuildingElement_IfcRelFillsElement]-(inst:IfcRelFillsElement_xx)-[ifcowl:relatingOpeningElement_IfcRelFillsElement]-&gt;(inst:IfcOpeningElement_xx)&lt;-[ifcowl:relatedOpeningElement_IfcRelVoidsElement]-(inst:IfcRelVoidsElement_xx)-[ifcowl:relatingBuildingElement_IfcRelVoidsElement]-&gt;(inst:IfcWallStandardCase_xx)
	 * 
	 * <P>
	 * <b>OR</b>
	 * <P>
	 * 
	 * (inst:IfcDoor_xx)-[ifcowl:objectPlacement_IfcProduct]-&gt;(inst:IfcLocalPlacement_xx)-[ifcowl:placementRelTo_IfcLocalPlacement]-&gt;(inst:IfcLocalPlacement_yy)&lt;-[ifcowl:objectPlacement_IfcProduct]-(inst:IfcOpeningElement_xx)&lt;-[ifcowl:relatedOpeningElement_IfcRelVoidsElement]-(inst:IfcRelVoidsElement_xx)-[ifcowl:relatingBuildingElement_IfcRelVoidsElement]-&gt;(inst:IfcWallStandardCase_xx)
	 * 
	 * </font>
	 * 
	 * @param element Apache Jena Resource RDF node that refers to an ifcOWL element
	 * @param ifcOWL  The ifcOWL namespace element.
	 * @return The list of all hosted elements under the element
	 */
	public static List<RDFNode> listHosted_Elements(Resource element, IfcOWLNameSpace ifcOWL) {
		List<RDFNode> ret;

		RDFStep[] path1 = { new InvRDFStep(ifcOWL.getProperty("relatingBuildingElement_IfcRelVoidsElement")),
				new RDFStep(ifcOWL.getProperty("relatedOpeningElement_IfcRelVoidsElement")),
				new InvRDFStep(ifcOWL.getProperty("relatingOpeningElement_IfcRelFillsElement")),
				new RDFStep(ifcOWL.getProperty("relatedBuildingElement_IfcRelFillsElement")) };
		ret = RDFUtils.pathQuery(element, path1);

		RDFStep[] path2 = { new InvRDFStep(ifcOWL.getProperty("relatingBuildingElement_IfcRelVoidsElement")),
				new RDFStep(ifcOWL.getProperty("relatedOpeningElement_IfcRelVoidsElement")),
				new RDFStep(ifcOWL.getProperty("objectPlacement_IfcProduct")),
				new InvRDFStep(ifcOWL.getProperty("placementRelTo_IfcLocalPlacement")),
				new InvRDFStep(ifcOWL.getProperty("objectPlacement_IfcProduct")) };
		ret.addAll(RDFUtils.pathQuery(element, path2));

		return ret;
	}

	/**
	 * Returns list of all RDF nodes that match the RDF graoh pattern:
	 * 
	 * INVERSE(relatingObject_IfcRelDecomposes)-> relatedObjects_IfcRelDecomposes
	 * 
	 * @param element the starting RDF node
	 * @return The list of the matching elements
	 */
	public static List<RDFNode> listAggregated_Elements(Resource element, IfcOWLNameSpace ifcOWL) {
		List<RDFNode> ret;

		RDFStep[] path1 = { new InvRDFStep(ifcOWL.getProperty("relatingObject_IfcRelDecomposes")),
				new RDFStep(ifcOWL.getProperty("relatedObjects_IfcRelDecomposes")) };
		ret = RDFUtils.pathQuery(element, path1);
		return ret;
	}

	/**
	 * Returns list of all RDF nodes that are of subclasses of IfcBuildingElement on
	 * the RDF graph.
	 * 
	 * @return list of the elements
	 */
	public static List<Resource> listBuildingElements(Model ifcowl_model) {
		final List<Resource> ret = new ArrayList<>();
		ifcowl_model.listStatements().filterKeep(t1 -> t1.getPredicate().equals(RDF.type)).filterKeep(t2 -> {
			if (!t2.getObject().isResource())
				return false;
			Resource rsuper = getSuperClass(t2.getObject().asResource());
			if (rsuper == null)
				return false;
			return rsuper.getLocalName().equals("IfcBuildingElement");
		}).mapWith(t1 -> t1.getSubject()).forEachRemaining(s -> ret.add(s));
		;
		return ret;
	}

	/**
	 * Returns a super class of the RDF resource, if any
	 * 
	 * @param r The Apache Jena node resource
	 * @return The RDF redource node of the super class, null if none found.
	 */
	public static Resource getSuperClass(Resource r) {
		StmtIterator subclass_statement_iterator = r.listProperties(RDFS.subClassOf);
		while (subclass_statement_iterator.hasNext()) {
			Statement su = subclass_statement_iterator.next();
			Resource ifcowl_superclass = su.getObject().asResource();
			if (!ifcowl_superclass.isAnon())
				return ifcowl_superclass;
		}
		return null;
	}

	// Solution proposed by Simon Steyskal 2018
	private static RDFStep[] getPropertySetPath(IfcOWLNameSpace ifcOWL) {
		if (ifcOWL.getIfcURI().toUpperCase().indexOf("IFC2X3") != -1) {  // fixed by JO 2020
			RDFStep[] path = { new InvRDFStep(ifcOWL.getRelatedObjects_IfcRelDefines()),
					new RDFStep(ifcOWL.getRelatingPropertyDefinition_IfcRelDefinesByProperties()) };
			return path;
		} else {
			RDFStep[] path = { new InvRDFStep(ifcOWL.getProperty("relatedObjects_IfcRelDefinesByProperties")),
					new RDFStep(ifcOWL.getProperty("relatingPropertyDefinition_IfcRelDefinesByProperties")) };
			return path;
		}
	}

	/**
	 * Returns list of all RDF nodes that match the RDF graoh pattern:
	 * 
	 *
	 * INVERSE (RelatedObjects_IfcRelDefines) ->
	 * RelatingPropertyDefinition_IfcRelDefinesByProperties
	 * 
	 * on the RDF graph.
	 * 
	 * @param resource the starting RDF node
	 * @return the list of the matching RDF nodes.
	 */
	public static List<RDFNode> listPropertysets(Resource resource, IfcOWLNameSpace ifcOWL) {
		return RDFUtils.pathQuery(resource, getPropertySetPath(ifcOWL));
	}

	
	public static List<RDFNode> getProjectSIUnits(IfcOWLNameSpace ifcOWL, Model ifcowl_model) {
		RDFStep[] path = { new InvRDFStep(RDF.type) };
		return RDFUtils.pathQuery(ifcowl_model.getResource(ifcOWL.getIfcSIUnit()), path);
	}

	/**
	 * Returns list of all RDF nodes that are of type ifcOWL Ontology base URI +
	 * IfcPropertySet on the RDF graph.
	 * 
	 * @return the list of the matching RDF nodes.
	 */
	public static List<RDFNode> listPropertysets(IfcOWLNameSpace ifcOWL, Model ifcowl_model) {
		RDFStep[] path = { new InvRDFStep(RDF.type) };
		return RDFUtils.pathQuery(ifcowl_model.getResource(ifcOWL.getIfcPropertySet()), path);
	}

	public static Optional<String> getPredefinedData(RDFNode rn) {
		if (!rn.isResource())
			return Optional.empty();

		final StringBuilder sb = new StringBuilder();
		rn.asResource().listProperties().toList().stream()
				.filter(t -> t.getPredicate().getLocalName().startsWith("predefinedType_"))
				.map(t -> t.getObject().asResource().getLocalName()).forEach(o -> sb.append(o));
		if (sb.length() == 0)
			return Optional.empty();
		return Optional.of(sb.toString());
	}

	/**
	 * Reads in a Turtle formatted ontology file into a Jena RDF store
	 * 
	 * @param ifc_file The absolute path of the Turtle formatted ontology file
	 * @param model    Am Apache Jene model where the ontology triples are added.
	 */
	public static void readIfcOWLOntology(String ifc_file, Model model) {
		String exp = IfcOWLUtils.getExpressSchema(ifc_file);
		InputStream in = null;
		try {
			in = IfcSpfReader.class.getResourceAsStream("/" + exp + ".ttl");

			if (in == null)
				in = IfcSpfReader.class.getResourceAsStream("/resources/" + exp + ".ttl");
			model.read(in, null, "TTL");
		} finally {
			try {
				in.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * Reads in a Turtle formatted ontology file into a Jena RDF store
	 * 
	 * @param exp The Express schema name
	 * @param model    Am Apache Jene model where the ontology triples are added.
	 */
	public static void readIfcOWLOntologyWhenSchemaKnown(String exp, Model model,String uri) {
		InputStream in = null;
		try {
			in = IfcSpfReader.class.getResourceAsStream("/" + exp + ".ttl");

			if (in == null)
				in = IfcSpfReader.class.getResourceAsStream("/resources/" + exp + ".ttl");
			System.out.println("in "+in+" exp: "+exp);
			model.read(in, uri, "TTL");
		} finally {
			try {
				in.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}
	


	/**
	 * 
	 * This is a direct copy from the IFCtoRDF https://github.com/pipauwel/IFCtoRDF
	 * 
	 * The idea is to make sure that we are using exactly the same ontology files
	 * that the IFCtoRDF is using for the associated Abox output.
	 * 
	 * @param ifc_file the absolute path (For example: c:\ifcfiles\ifc_file.ifc) for
	 *                 the IFC file
	 * @return the IFC Express chema of the IFC file.
	 */
	public static String getExpressSchema(String ifc_file) {
		try {
			FileInputStream fstream = new FileInputStream(ifc_file);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			try {
				String strLine;
				while ((strLine = br.readLine()) != null) {
					if (strLine.length() > 0) {
						if (strLine.startsWith("FILE_SCHEMA")) {
							if (strLine.indexOf("IFC2X3") != -1)
								return "IFC2X3_TC1";
							if (strLine.indexOf("IFC4") != -1)
								return "IFC4_ADD1";
							else
								return "";
						}
					}
				}
			} finally {
				br.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static void addError(Model lbd_general_output_model4_errors,String error_text)
	{
		Resource r=lbd_general_output_model4_errors.createResource();
		Property ep=lbd_general_output_model4_errors.createProperty("https://dc.rwth-aachen.de/LBD#model_error");
	    Literal  el=lbd_general_output_model4_errors.createLiteral(error_text);
		r.addLiteral(ep, el);
	}
}
