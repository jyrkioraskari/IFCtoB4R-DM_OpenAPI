
package de.rwth_aachen.dc.lbd_smls.ns;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class LBD_NS extends abstract_NS{
 
	public static class PROPS_NS {
		public static final String props_ns = "https://w3id.org/props#";
		public static final String bsddprops_ns = "https://buildingsmart.org/bsddld#";
		public static final String psd_ns = "http://www.buildingsmart-tech.org/ifcOWL/IFC4-PSD#";

		public static void addNameSpace(Model model)
		{
			model.setNsPrefix("props", props_ns);
			//model.setNsPrefix("bsddld", bsddprops_ns);
			//model.setNsPrefix("IFC4-PSD", psd_ns);
		}
		
		//public static final Resource props=resource(props_ns,"Pset");
		//public static final Property partofPset=property(props_ns, "partOfPset");	
		
		public static final Property isBSDDProp=property(bsddprops_ns, "isBSDDProperty");	
		public static final Property namePset=property(psd_ns, "name");
		public static final Property ifdGuidProperty=property(psd_ns,"ifdguid");
		public static final Property propertyDef=property(psd_ns,"propertyDef");

		public static final Resource attribute_group=resource(props_ns,"AttributesGroup");
		public static final Property partofAG=property(props_ns, "partOfAttributesGroup");
		
		public static final Resource propertyset=resource(props_ns,"PropertySet");
		public static final Resource property=resource(props_ns,"Property");
		
		public static final Property hasPropertySet =property(props_ns,"hasPropertySet");
		
		public static final Property hasProperty =property(props_ns,"hasProperty");
		public static final Property hasValue =property(props_ns,"hasValue");
		public static final Property hasName =property(props_ns,"hasName");
		
	}

}
