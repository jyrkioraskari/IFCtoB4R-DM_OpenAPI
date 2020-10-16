package de.rwth_aachen.dc.lbd_smls.ns;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class Product extends abstract_NS{
	public static final String beo_ns = "https://pi.pauwel.be/voc/buildingelement#"; 
	public static final String furnishing_ns = "https://pi.pauwel.be/voc/furniture#";
	public static final String mep_ns = "https://pi.pauwel.be/voc/distributionelement#";  

	public static void addNameSpace(Model model)
	{
		model.setNsPrefix("beo", beo_ns);
	}
	
	public static Resource getProductType(Resource ifOwlClass)
	{
		String uri=ifOwlClass.getLocalName().substring(3);
		return resource(beo_ns, uri);
	}
	
	public static Property getProperty(String name) {
		String[] splitted=name.split("_");
		return property(beo_ns,splitted[0]);
	}
}


