package de.rwth_aachen.dc.lbd_smls.tests;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.jena.rdf.model.Model;

import de.rwth_aachen.dc.lbd_smls.IfcOWLtoLBDConverter_BIM4Ren;

public class  Sample_IfcOWLtoSMLS{
	
	static private void extractIfcOWLtoBD_SMLS(File ifcOwlFile, StringBuilder result_string) {
		IfcOWLtoLBDConverter_BIM4Ren lbdconverter = new IfcOWLtoLBDConverter_BIM4Ren();
		Model m = lbdconverter.convert(ifcOwlFile.getAbsolutePath());

		OutputStream ttl_output = new OutputStream() {
			private StringBuilder string = new StringBuilder();

			@Override
			public void write(int b) throws IOException {
				this.string.append((char) b);
			}

			public String toString() {
				return this.string.toString();
			}
		};
		//m.write(System.out, "TTL");
		m.write(ttl_output, "TTL");
		//RDFDataMgr.write(ttl_output, m, RDFFormat.JSONLD_COMPACT_PRETTY);
		result_string.append(ttl_output.toString());
	}
	
	
	public static void main(String[] args) {
		String ifcOwlFileName = "c:\\test\\bim4ren\\n1\\LibertyLoft_BOT.ttl";
		//String ifcFileName = "c:\\ifc2\\Barcelona_Pavilion.ttl";
		File ifcOwlFile = new File(ifcOwlFileName);
		StringBuilder result_string=new StringBuilder();
		extractIfcOWLtoBD_SMLS(ifcOwlFile, result_string);
		System.out.println(result_string.toString());
	}
}
		
