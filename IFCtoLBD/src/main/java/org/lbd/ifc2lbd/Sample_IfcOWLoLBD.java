package org.lbd.ifc2lbd;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;

public class Sample_IfcOWLoLBD {
	public static void main(String[] args) {
		String ifcOwlFileName = "c:\\test\\bim4ren\\n1\\BIM4REN_DUNAN_20200716_IfcSite_added.ttl";
		//String ifcOwlFileName = "c:\\ifc2\\Barcelona_Pavilion.ttl";
		File ifcOwlFile = new File(ifcOwlFileName);
		try {
			byte[] fileContent = Files.readAllBytes(ifcOwlFile.toPath());

			File tempFile;
			tempFile = File.createTempFile("model-", ".ttl");
			tempFile.deleteOnExit();
			FileUtils.writeByteArrayToFile(tempFile, fileContent);
			System.out.println("Temp ifcOWL TTL file:" + tempFile.getAbsolutePath());
			String outputFile = tempFile.getAbsolutePath().substring(0, tempFile.getAbsolutePath().length() - 4)
					+ ".ttl";
			System.out.println("outputfile: "+outputFile);
			IfcOWLtoLBDConverter converter=new IfcOWLtoLBDConverter(false, 0);
			
			Model m=converter.convert(ifcOwlFile.getAbsolutePath());
			m.write(System.out, "TTL");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
