package de.rwth_aachen.dc.lbd_smls.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.apache.jena.rdf.model.Model;
import org.lbd.ifc2lbd.application_messaging.events.IFCtoLBD_SystemStatusEvent;

import com.google.common.eventbus.EventBus;

import de.rwth_aachen.dc.lbd_smls.IFCtoLBDConverter_BIM4Ren;

public class Sample_IFCtoSMLS {

	static private void extractLBD_SMLS(File ifcFile, StringBuilder result_string) {
		IFCtoLBDConverter_BIM4Ren lbdconverter = new IFCtoLBDConverter_BIM4Ren();
		Model m = lbdconverter.convert(ifcFile.getAbsolutePath(), "https://dot.dc.rwth-aachen.de/IFCtoLBDset#");

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
		// m.write(System.out, "TTL");
		m.write(ttl_output, "TTL");
		writeModel(m, ifcFile+"_LBD.ttl"); 
		// RDFDataMgr.write(ttl_output, m, RDFFormat.JSONLD_COMPACT_PRETTY);
		result_string.append(ttl_output.toString());
	}

	public static void writeModel(Model m, String target_file) {
		OutputStreamWriter fo = null;
		try {
			fo = new OutputStreamWriter(new FileOutputStream(new File(target_file)),
					Charset.forName("UTF-8").newEncoder());

			m.write(fo, "TTL");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (fo != null)
				try {
					fo.close();
				} catch (IOException e) {
				}
		}
	}

	public static void main(String[] args) {
		// String ifcFileName = "c:\\test\\bim4ren\\BIM4Ren_DUNANT_cleaned_IFC2x3.ifc";
		//String ifcFileName = "C:\\test\\bim4ren\\BIM4Ren_DUNANT_cleaned_IFC2x3.ifc";
		String ifcFileName = "c:\\ifc3\\ifc\\SGD_Blueberry_Eng-HVAC-Plumbing-1.ifc";
		File ifcFile = new File(ifcFileName);
		StringBuilder result_string = new StringBuilder();
		extractLBD_SMLS(ifcFile, result_string);
		System.out.println(result_string.toString());
	}
}
