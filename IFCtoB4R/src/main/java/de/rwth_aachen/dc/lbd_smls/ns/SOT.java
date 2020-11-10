package de.rwth_aachen.dc.lbd_smls.ns;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class SOT extends abstract_NS{
	public static final String SOT_ns = "https://bim4ren.eu/alignment/SOT-IFC#";
	
	public static final Resource System =resource(SOT_ns,"System");
	public static final Resource SystemComponent =resource(SOT_ns,"SystemComponent");

	public static final Property hasElements=property(SOT_ns, "hasElements");
	public static final Property connectedWith=property(SOT_ns, "connectedWith");
	 
	public static void addNameSpace(Model model)
	{
		model.setNsPrefix("sot", SOT_ns);
	}
	
}