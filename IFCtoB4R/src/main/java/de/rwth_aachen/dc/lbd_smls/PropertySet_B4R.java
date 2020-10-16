package de.rwth_aachen.dc.lbd_smls;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import de.rwth_aachen.dc.lbd_smls.ns.PROPS;
import de.rwth_aachen.dc.lbd_smls.ns.SMLS;
import de.rwth_aachen.dc.lbd_smls.ns.UNIT;
import de.rwth_aachen.dc.lbd_smls.utils.StringOperations;

/*
 *  Copyright (c) 2017,2018,2019.2020 Jyrki Oraskari (Jyrki.Oraskari@gmail.f)
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
 * A class where IFC PropertySet is collected from the IFC file
 * 
 *
 */
public class PropertySet_B4R {
	private final Map<String, String> unitmap;

	private class PsetProperty {
		final Property p; // Jena RDF property
		final Resource r; // Jena RDF resource object

		public PsetProperty(Property p, Resource r) {
			super();
			this.p = p;
			this.r = r;
		}
	}

	private final String uriBase;
	private final Model lbd_model;
	private String propertyset_name;

	private final Map<String, RDFNode> mapPnameValue = new HashMap<>();
	private final Map<String, RDFNode> mapPnameType = new HashMap<>();
	private final Map<String, RDFNode> mapPnameUnit = new HashMap<>();
	private final Map<String, RDFNode> mapBSDD = new HashMap<>();

	private boolean is_bSDD_pset = false;
	private Resource psetDef = null;

	public PropertySet_B4R(String uriBase, Model lbd_model, Model ontology_model, String propertyset_name,
			Map<String, String> unitmap) {
		this.unitmap = unitmap;
		System.out.println("PSEt init");
		StmtIterator iter = ontology_model.listStatements(null, PROPS.namePset, propertyset_name);
		if (iter.hasNext()) {
			System.out.println("IS bsDDs");
			is_bSDD_pset = true;
			psetDef = iter.next().getSubject();
		}
		this.uriBase = uriBase;
		this.lbd_model = lbd_model;
		this.propertyset_name = propertyset_name;
	}
	

	public void putPnameValue(String property_name, RDFNode value) {
		property_name = org.apache.commons.lang3.StringUtils.stripAccents(property_name);
		mapPnameValue.put(StringOperations.toCamelCase(property_name), value);
	}

	public void putPnameType(String property_name, RDFNode type) {
		System.out.println("property name: "+property_name);
		property_name = org.apache.commons.lang3.StringUtils.stripAccents(property_name);
		mapPnameType.put(StringOperations.toCamelCase(property_name), type);
	}

	public void putPnameUnit(String property_name, RDFNode unit) {
		property_name = org.apache.commons.lang3.StringUtils.stripAccents(property_name);
		mapPnameUnit.put(StringOperations.toCamelCase(property_name), unit);
	}

	public void putPsetPropertyRef(RDFNode property) {
		String pname = property.asLiteral().getString();
		pname = org.apache.commons.lang3.StringUtils.stripAccents(pname);

		if (is_bSDD_pset) {
			StmtIterator iter = psetDef.listProperties(PROPS.propertyDef);
			while (iter.hasNext()) {
				Resource prop = iter.next().getResource();
				StmtIterator iterProp = prop.listProperties(PROPS.namePset);
				while (iterProp.hasNext()) {
					Literal psetPropName = iterProp.next().getLiteral();
					System.out.println("XXXXX "+psetPropName +" - "+ pname);
					if (psetPropName.getString().equals(pname))
						mapBSDD.put(StringOperations.toCamelCase(property.toString()), prop);
					else {
						String camel_name = StringOperations.toCamelCase(property.toString());
						if (psetPropName.getString().toUpperCase().equals(camel_name.toUpperCase()))
							mapBSDD.put(camel_name, prop);
					}
				}
			}
		}
	}

	/**
	 * Adds property value property for an resource.
	 * 
	 * @param lbd_resource   The Jena Resource in the model
	 * @param extracted_guid The GUID of the elemet in the long form
	 */
	Set<String> hashes = new HashSet<>();

	public void connect(Resource lbd_resource, String long_guid) {

		if (hashes.add(lbd_resource.getURI()))
			for (String pname : this.mapPnameValue.keySet()) {
				Property property = lbd_resource.getModel().createProperty(PROPS.props_ns + pname);

				RDFNode ifc_unit = this.mapPnameUnit.get(pname);
				if (ifc_unit != null) {
					Resource bn = lbd_resource.getModel().createResource();
					lbd_resource.addProperty(property, bn);

					if (mapBSDD.get(pname) != null)
						bn.addProperty(RDFS.seeAlso, mapBSDD.get(pname));
					
					bn.addProperty(RDF.value, this.mapPnameValue.get(pname));

					String si_unit = ifc_unit.asResource().getLocalName();
					if (si_unit != null) {
						if (si_unit.equals("METRE")) {
							bn.addProperty(SMLS.unit, UNIT.METER);
						} else if (si_unit.equals("SQUARE_METRE")) {
							bn.addProperty(SMLS.unit, UNIT.SQUARE_METRE);
						} else if (si_unit.equals("CUBIC_METRE")) {
							bn.addProperty(SMLS.unit, UNIT.CUBIC_METRE);
						} else if (si_unit.equals("RADIAN")) {
							bn.addProperty(SMLS.unit, UNIT.RADIAN);
						}
					}
				} else {

					RDFNode ifc_measurement_type = this.mapPnameType.get(pname);
					if (ifc_measurement_type != null) {
						String unit = ifc_measurement_type.asResource().getLocalName().toLowerCase();
						if (unit.startsWith("ifc"))
							unit = unit.substring(3);
						if (unit.startsWith("positive"))
							unit = unit.substring("positive".length());
						if (unit.endsWith("measure"))
							unit = unit.substring(0, unit.length() - "measure".length());
						String si_unit = this.unitmap.get(unit);
						Resource bn = lbd_resource.getModel().createResource();
						lbd_resource.addProperty(property, bn);

						if (mapBSDD.get(pname) != null)
							bn.addProperty(RDFS.seeAlso, mapBSDD.get(pname));

						bn.addProperty(RDF.value, this.mapPnameValue.get(pname));

						if (si_unit != null) {
							if (si_unit.equals("METRE")) {
								bn.addProperty(SMLS.unit, UNIT.METER);
							} else if (si_unit.equals("SQUARE_METRE")) {
								bn.addProperty(SMLS.unit, UNIT.SQUARE_METRE);
							} else if (si_unit.equals("CUBIC_METRE")) {
								bn.addProperty(SMLS.unit, UNIT.CUBIC_METRE);
							} else if (si_unit.equals("RADIAN")) {
								bn.addProperty(SMLS.unit, UNIT.RADIAN);
							}
						}
					}
				}
			}
	}
	
	 public Optional<Boolean> isExternal() {

	        RDFNode val = this.mapPnameValue.get("isExternal");

	        if (val == null)
	            return Optional.empty();
	        else {
	            if (!val.isLiteral())
	                return Optional.empty();
	            if (val.asLiteral().getValue().equals(true))
	                return Optional.of(true);
	            else
	                return Optional.of(false);
	        }
	    }
}
