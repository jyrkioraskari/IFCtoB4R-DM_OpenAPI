/*
 * Copyright 2016  Pieter Pauwels, Ghent University;Lewis John McGibbney, Apache
 * 2016, 2020, Jyrki Oraskari Aalto University, RWTH-Aachen 
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

package de.rwth_aachen.dc.lbd_smls.ns;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;

public class IfcOWLNameSpace extends abstract_NS {
	public static final String EXPRESS = "https://w3id.org/express#";
	public static final String SIMPLEBIM = "http://ifcowl.openbimstandards.org/SimpleBIM";

	private final Property relatingObject_IfcRelDecomposes;
	private final Property relatedObjects_IfcRelDecomposes;
	private final Property relatingStructure_IfcRelContainedInSpatialStructure;
	private final Property relatedElements_IfcRelContainedInSpatialStructure;
	
	private final Property relatedObjects_IfcRelDefines;
	private final Property relatingPropertyDefinition_IfcRelDefinesByProperties;
	private final Property name_IfcRoot;
	private final Property name_IfcProperty;
	private final Property hasProperties_IfcPropertySet;
	private final Property nominalValue_IfcPropertySingleValue;
    private final Property unit_IfcPropertySingleValue;
	
	private final Property description;
	private final Property name;
	private final Property longName;
	private final Property guid;

	private final Property units_IfcUnitAssignment;
	private final Property unitType_IfcNamedUnit;
	private final Property prefix_IfcSIUnit;
	private final Property name_IfcSIUnit;

    private final Property unitsInContext_IfcProject;

	
	private final String IfcProject;
	private final String IfcSite;
	private final String IfcBuilding;
	private final String IfcSpace;
	private final String IfcProduct;
	private final String IfcPropertySet;
	private final String IfcUnitAssignment;
	private final String IfcSIUnit;
	private final String LENGTHUNIT;
	private final String AREAUNIT;
	private final String VOLUMEUNIT;
	private final String PLANEANGLEUNIT;
	
	private final String METRE;
	private final String SQUARE_METRE;
	private final String CUBIC_METRE;
	private final String RADIAN;
	
	private final String ifcURI;
	
	
	// HVAC
	private final String IfcFlowSegment;
	private final String IfcRelConnectsPorts;
	private final String IfcRelConnectsPortToElement;
	private final String IfcRelAssignsToGroup;

	
	private final Property relatedElement_IfcRelConnectsPortToElement;
	private final Property relatingPort_IfcRelConnectsPortToElement;
	
	private final Property relatedObjects_IfcRelAssigns;
	private final Property relatingGroup_IfcRelAssignsToGroup;

	private final Property relatedPort_IfcRelConnectsPorts;
	private final Property relatingPort_IfcRelConnectsPorts;

	
	public IfcOWLNameSpace(String ifcURI)
	{
		// There should be no vocabulary that does not end in # or /.  This fixes possible errors
		if(!ifcURI.endsWith("#")&&!ifcURI.endsWith("/"))
			ifcURI=ifcURI+"#";
		this.ifcURI=ifcURI;
		relatingObject_IfcRelDecomposes = property(ifcURI, "relatingObject_IfcRelDecomposes");
		relatedObjects_IfcRelDecomposes = property(ifcURI, "relatedObjects_IfcRelDecomposes");
		relatingStructure_IfcRelContainedInSpatialStructure = property(ifcURI,
				"relatingStructure_IfcRelContainedInSpatialStructure");
		relatedElements_IfcRelContainedInSpatialStructure = property(ifcURI,
				"relatedElements_IfcRelContainedInSpatialStructure");
		
		relatedObjects_IfcRelDefines = property(ifcURI, "relatedObjects_IfcRelDefines");
		relatingPropertyDefinition_IfcRelDefinesByProperties =property(ifcURI, "relatingPropertyDefinition_IfcRelDefinesByProperties");
		
		name_IfcRoot  =property(ifcURI, "name_IfcRoot");
		name_IfcProperty  =property(ifcURI, "name_IfcProperty");
		hasProperties_IfcPropertySet =property(ifcURI, "hasProperties_IfcPropertySet");
		nominalValue_IfcPropertySingleValue =property(ifcURI, "nominalValue_IfcPropertySingleValue");
        unit_IfcPropertySingleValue = property(ifcURI, "unit_IfcPropertySingleValue");
		
		description = property(ifcURI, "description_IfcRoot");
		name = property(ifcURI, "name_IfcRoot");
		longName = property(ifcURI, "longName_IfcSpatialStructureElement");
		units_IfcUnitAssignment=property(ifcURI, "units_IfcUnitAssignment");
		
		unitsInContext_IfcProject = property(ifcURI, "unitsInContext_IfcProject");
		
		
		guid = property(ifcURI, "globalId_IfcRoot");

		IfcProject = ifcURI + "IfcProject";
		IfcSite = ifcURI + "IfcSite";
		IfcBuilding = ifcURI + "IfcBuilding";
		IfcSpace = ifcURI + "IfcSpace";
		IfcProduct = ifcURI + "IfcProduct";
		IfcPropertySet = ifcURI + "IfcPropertySet";
		IfcUnitAssignment = ifcURI + "IfcUnitAssignment";
		IfcSIUnit = ifcURI + "IfcSIUnit";
		
		unitType_IfcNamedUnit = property(ifcURI, "unitType_IfcNamedUnit");
		prefix_IfcSIUnit = property(ifcURI, "prefix_IfcSIUnit");
		name_IfcSIUnit = property(ifcURI, "name_IfcSIUnit");
		LENGTHUNIT= ifcURI + "LENGTHUNIT";
		AREAUNIT= ifcURI + "AREAUNIT";
		VOLUMEUNIT= ifcURI + "VOLUMEUNIT";
		PLANEANGLEUNIT= ifcURI + "PLANEANGLEUNIT";
		
		METRE= ifcURI + "METRE";
		SQUARE_METRE= ifcURI + "SQUARE_METRE";
		CUBIC_METRE= ifcURI + "CUBIC_METRE";
		RADIAN= ifcURI + "RADIAN";
		
		//HVAC
		IfcRelConnectsPorts = ifcURI +"IfcRelConnectsPorts";
		IfcRelConnectsPortToElement=ifcURI +"IfcRelConnectsPortToElement";
		IfcFlowSegment=ifcURI + "IfcFlowSegment";
		IfcRelAssignsToGroup=ifcURI + "IfcRelAssignsToGroup";
		
		relatedElement_IfcRelConnectsPortToElement=property(ifcURI, "relatedElement_IfcRelConnectsPortToElement");
		relatingPort_IfcRelConnectsPortToElement=property(ifcURI, "relatingPort_IfcRelConnectsPortToElement");
		
		relatedObjects_IfcRelAssigns=property(ifcURI, "relatedObjects_IfcRelAssigns");
		relatingGroup_IfcRelAssignsToGroup=property(ifcURI, "relatingGroup_IfcRelAssignsToGroup");

		relatingPort_IfcRelConnectsPorts = property(ifcURI, "relatingPort_IfcRelConnectsPorts");
		relatedPort_IfcRelConnectsPorts = property(ifcURI, "relatedPort_IfcRelConnectsPorts");
	}
	
	
	
	public String getIfcURI() {
		return ifcURI;
	}



	public Property getProperty(String name)
	{
		return property(ifcURI, name);
	}

	public Property getRelatingObject_IfcRelDecomposes() {
		return relatingObject_IfcRelDecomposes;
	}
	public Property getRelatedObjects_IfcRelDecomposes() {
		return relatedObjects_IfcRelDecomposes;
	}
	public Property getRelatingStructure_IfcRelContainedInSpatialStructure() {
		return relatingStructure_IfcRelContainedInSpatialStructure;
	}
	public Property getRelatedElements_IfcRelContainedInSpatialStructure() {
		return relatedElements_IfcRelContainedInSpatialStructure;
	}
	public Property getRelatedObjects_IfcRelDefines() {
		return relatedObjects_IfcRelDefines;
	}
	public Property getRelatingPropertyDefinition_IfcRelDefinesByProperties() {
		return relatingPropertyDefinition_IfcRelDefinesByProperties;
	}
	public Property getName_IfcRoot() {
		return name_IfcRoot;
	}
	public Property getName_IfcProperty() {
		return name_IfcProperty;
	}
	public Property getNominalValue_IfcPropertySingleValue() {
		return nominalValue_IfcPropertySingleValue;
	}
    public Property getUnit_IfcPropertySingleValue() {
        return unit_IfcPropertySingleValue;
    }
	public Property getDescription() {
		return description;
	}
	public Property getName() {
		return name;
	}
	public Property getLongName() {
		return longName;
	}
	public Property getGuid() {
		return guid;
	}

	public Property getUnits_IfcUnitAssignment() {
		return units_IfcUnitAssignment;
	}
	public Property getUnitType_IfcNamedUnit() {
		return unitType_IfcNamedUnit;
	}

	public Property getName_IfcSIUnit() {
		return name_IfcSIUnit;
	}
	
	public Property getPrefix_IfcSIUnit() {
		return prefix_IfcSIUnit;
	}



	public Property getUnitsInContext_IfcProject() {
		return unitsInContext_IfcProject;
	}

	
	public String getIfcProject() {
		return IfcProject;
	}



	public String getIfcSite() {
		return IfcSite;
	}
	public String getIfcBuilding() {
		return IfcBuilding;
	}
	public String getIfcSpace() {
		return IfcSpace;
	}
	public String getIfcProduct() {
		return IfcProduct;
	}

	public String getIfcUnitAssignment() {
		return IfcUnitAssignment;
	}
	public String getIfcSIUnit() {
		return IfcSIUnit;
	}
	



	public String getLENGTHUNIT() {
		return LENGTHUNIT;
	}

	public String getAREAUNIT() {
		return AREAUNIT;
	}

	public String getVOLUMEUNIT() {
		return VOLUMEUNIT;
	}

	public String getPLANEANGLEUNIT() {
		return PLANEANGLEUNIT;
	}


	public String getMETRE() {
		return METRE;
	}
	
	public String getSQUARE_METRE() {
		return SQUARE_METRE;
	}
	
	public String getCUBIC_METRE() {
		return CUBIC_METRE;
	}

	public String getRADIAN() {
		return RADIAN;
	}


	public String getIfcPropertySet() {
		return IfcPropertySet;
	}
	
	
	//HVAC
	
	public String getIfcFlowSegment() {
		return IfcFlowSegment;
	}

	public String getIfcRelConnectsPortToElement() {
		return IfcRelConnectsPortToElement;
	}



	public String getIfcRelConnectsPorts() {
		return IfcRelConnectsPorts;
	}



	public String getIfcRelAssignsToGroup() {
		return IfcRelAssignsToGroup;
	}


    public Property getRelatedElement_IfcRelConnectsPortToElement() {
		return relatedElement_IfcRelConnectsPortToElement;
	}



	public Property getRelatingPort_IfcRelConnectsPortToElement() {
		return relatingPort_IfcRelConnectsPortToElement;
	}



	public Property getRelatedObjects_IfcRelAssigns() {
		return relatedObjects_IfcRelAssigns;
	}



	public Property getRelatingGroup_IfcRelAssignsToGroup() {
		return relatingGroup_IfcRelAssignsToGroup;
	}



	public Property getRelatingPort_IfcRelConnectsPorts() {
		return relatingPort_IfcRelConnectsPorts;
	}



	public Property getRelatedPort_IfcRelConnectsPorts() {
		return relatedPort_IfcRelConnectsPorts;
	}



	// Properties
	static public Property getHasString() {
		return hasString;
	}
	
	public Property getHasDouble() {
		return hasDouble;
	}
	public Property getHasInteger() {
		return hasInteger;
	}
	public Property getHasBoolean() {
		return hasBoolean;
	}
	public Property getHasLogical() {
		return hasLogical;
	}
	public Property getHasProperties_IfcPropertySet() {
		return hasProperties_IfcPropertySet;
	}

	public static final Property hasString = property(EXPRESS, "hasString");
	public static final Property hasDouble = property(EXPRESS, "hasDouble");
	public static final Property hasInteger = property(EXPRESS, "hasInteger");
	public static final Property hasBoolean = property(EXPRESS, "hasBoolean");
	public static final Property hasLogical = property(EXPRESS, "hasLogical");
}
