package org.lbd.ifc2lbd;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.lbd.ifc2lbd.ns.LBD_NS;
import org.lbd.ifc2lbd.ns.OPM;
import org.lbd.ifc2lbd.utils.StringOperations;

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
 * A class where IFC attributes are collected from an IFC element
 * 
 *
 */
public class AttributeSet_SMLS {
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

	private final Map<String, RDFNode> mapPnameValue = new HashMap<>();
	private final Map<String, RDFNode> mapPnameType = new HashMap<>();

	public AttributeSet_SMLS(String uriBase, Model lbd_model, Map<String, String> unitmap) {
		this.unitmap = unitmap;
		this.uriBase = uriBase;
		this.lbd_model = lbd_model;
	}

	public void putAnameValue(String attribute_name, RDFNode value, Optional<Resource> atype) {
		mapPnameValue.put(StringOperations.toCamelCase(attribute_name), value);
		if (atype.isPresent()) {
			mapPnameType.put(StringOperations.toCamelCase(attribute_name), atype.get());
		}
	}

	/**
	 * Adds property value property for an resource.
	 * 
	 * @param lbd_resource The Jena Resource in the model
	 * @param long_guid    The GUID of the elemet in the long form
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
							Resource bn_accuracy = lbd_resource.getModel().createResource();
							//Literal accuracy = lbd_resource.getModel().createTypedLiteral(1f);
							//bn.addProperty(LBD_NS.SMLS.accuracy, bn_accuracy);
							//bn_accuracy.addProperty(RDF.value, accuracy);

						} 
						else if (si_unit.equals("CUBIC_METRE")) {
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
		for (String k : this.mapPnameValue.keySet()) {
			Resource property_resource;
			property_resource = this.lbd_model.createResource(this.uriBase + k + "_" + long_guid);

			property_resource.addProperty(OPM.value, this.mapPnameValue.get(k));

			Property p;
			p = this.lbd_model.createProperty(LBD_NS.PROPS_NS.props_ns + StringOperations.toCamelCase(k));
			properties.add(new PsetProperty(p, property_resource));
		}
		return properties;
	}

}
