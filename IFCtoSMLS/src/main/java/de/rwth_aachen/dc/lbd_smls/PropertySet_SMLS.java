package de.rwth_aachen.dc.lbd_smls;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.function.library.leviathan.rnd;
import org.apache.jena.vocabulary.RDF;

import de.rwth_aachen.dc.lbd_smls.ns.LBD_NS;
import de.rwth_aachen.dc.lbd_smls.ns.OPM;
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
public class PropertySet_SMLS {
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
	private final Map<String, RDFNode> mapBSDD = new HashMap<>();

	private boolean is_bSDD_pset = false;
	private Resource psetDef = null;

	public PropertySet_SMLS(String uriBase, Model lbd_model, Model ontology_model, String propertyset_name,
			Map<String, String> unitmap) {
		this.unitmap = unitmap;
		StmtIterator iter = ontology_model.listStatements(null, LBD_NS.PROPS_NS.namePset, propertyset_name);
		if (iter.hasNext()) {

			is_bSDD_pset = true;
			psetDef = iter.next().getSubject();
		}
		this.uriBase = uriBase;
		this.lbd_model = lbd_model;
		this.propertyset_name = propertyset_name;
	}

	public void putPnameValue(String property_name, RDFNode value) {
		mapPnameValue.put(StringOperations.toCamelCase(property_name), value);
	}

	public void putPnameType(String property_name, RDFNode type) {
		mapPnameType.put(StringOperations.toCamelCase(property_name), type);
	}

	public void putPsetPropertyRef(RDFNode property) {
		String pname = property.asLiteral().getString();
		if (is_bSDD_pset) {
			StmtIterator iter = psetDef.listProperties(LBD_NS.PROPS_NS.propertyDef);
			while (iter.hasNext()) {
				Resource prop = iter.next().getResource();
				StmtIterator iterProp = prop.listProperties(LBD_NS.PROPS_NS.namePset);
				while (iterProp.hasNext()) {
					Literal psetPropName = iterProp.next().getLiteral();
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
				Property property = lbd_resource.getModel().createProperty(LBD_NS.PROPS_NS.props_ns + pname);

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
					if (si_unit != null) {
						Resource bn = lbd_resource.getModel().createResource();
						lbd_resource.addProperty(property, bn);

						bn.addProperty(RDF.value, this.mapPnameValue.get(pname));

						if (si_unit.equals("METRE")) {
							bn.addProperty(LBD_NS.SMLS.unit, LBD_NS.UNIT.METER);
							//Resource bn_accuracy = lbd_resource.getModel().createResource();
							//Literal accuracy = lbd_resource.getModel().createTypedLiteral(1f);
							//bn.addProperty(LBD_NS.SMLS.accuracy, bn_accuracy);
							//bn_accuracy.addProperty(RDF.value, accuracy);

						} else if (si_unit.equals("SQUARE_METRE")) {
							bn.addProperty(LBD_NS.SMLS.unit, LBD_NS.UNIT.SQUARE_METRE);
							//Resource bn_accuracy = lbd_resource.getModel().createResource();
							//Literal accuracy = lbd_resource.getModel().createTypedLiteral(1f);
							//bn.addProperty(LBD_NS.SMLS.accuracy, bn_accuracy);
							//bn_accuracy.addProperty(RDF.value, accuracy);

						} else if (si_unit.equals("CUBIC_METRE")) {
							bn.addProperty(LBD_NS.SMLS.unit, LBD_NS.UNIT.CUBIC_METRE);
							//Resource bn_accuracy = lbd_resource.getModel().createResource();
							//Literal accuracy = lbd_resource.getModel().createTypedLiteral(1f);
							//bn.addProperty(LBD_NS.SMLS.accuracy, bn_accuracy);
							//bn_accuracy.addProperty(RDF.value, accuracy);

						} 
						else if (si_unit.equals("RADIAN")) {
							bn.addProperty(LBD_NS.SMLS.unit, LBD_NS.UNIT.RADIAN);
							//Resource bn_accuracy = lbd_resource.getModel().createResource();
							//Literal accuracy = lbd_resource.getModel().createTypedLiteral(1f);
							//bn.addProperty(LBD_NS.SMLS.accuracy, bn_accuracy);
							//bn_accuracy.addProperty(RDF.value, accuracy);

						} 
						/*else {
							bn.addProperty(LBD_NS.BEXT.si_unit, si_unit);
							bn.addProperty(LBD_NS.BEXT.unitType, unit);
						}*/
					} else {
						lbd_resource.addProperty(property, this.mapPnameValue.get(pname));
					}
				}
			}
	}

	private List<PsetProperty> writeOPM_Set(String long_guid) {
		List<PsetProperty> properties = new ArrayList<>();
		for (String key : this.mapPnameValue.keySet()) {
			Resource property_resource;
			property_resource = this.lbd_model.createResource(this.uriBase + key + "_" + long_guid);

			if (mapBSDD.get(key) != null)
				property_resource.addProperty(LBD_NS.PROPS_NS.isBSDDProp, mapBSDD.get(key));

			property_resource.addProperty(OPM.value, this.mapPnameValue.get(key));

			Property p;
			p = this.lbd_model.createProperty(LBD_NS.PROPS_NS.props_ns + StringOperations.toCamelCase(key));
			properties.add(new PsetProperty(p, property_resource));
		}
		return properties;
	}

}
