package de.rwth_aachen.dc.lbd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.vecmath.Point3d;

import org.bimserver.geometry.Matrix;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.renderengine.IndexFormat;
import org.bimserver.plugins.renderengine.Precision;
import org.bimserver.plugins.renderengine.RenderEngineException;
import org.bimserver.plugins.renderengine.RenderEngineGeometry;
import org.bimserver.plugins.renderengine.RenderEngineInstance;
import org.bimserver.plugins.renderengine.RenderEngineModel;
import org.bimserver.plugins.renderengine.RenderEngineSettings;
import org.ifcopenshell.IfcOpenShellEngine;
import org.ifcopenshell.IfcOpenShellModel;

import de.rwth_aachen.dc.OperatingSystemCopyOf_IfcGeomServer;
import nl.tue.ddss.bcf.BoundingBox;


public class IFCBoundingBoxes {

	private final IfcOpenShellModel renderEngineModel;

	public IFCBoundingBoxes(File ifcFile)
			throws DeserializeException, IOException, RenderEngineException {
		this.renderEngineModel = getRenderEngineModel(ifcFile);

	}

	public BoundingBox getBoundingBox(String guid) {
		BoundingBox boundingBox = null;
		RenderEngineInstance renderEngineInstance;
		try {
			renderEngineInstance = renderEngineModel.getInstanceFromGuid(guid); 
																											
			if (renderEngineInstance == null) {
				return null;
			}
			RenderEngineGeometry geometry = renderEngineInstance.generateGeometry();
			if (geometry != null && geometry.getNrIndices() > 0) {
				boundingBox = new BoundingBox();
				double[] tranformationMatrix = new double[16];
				if (renderEngineInstance.getTransformationMatrix() != null) {
					tranformationMatrix = renderEngineInstance.getTransformationMatrix();
					tranformationMatrix = Matrix.changeOrientation(tranformationMatrix);
				} else {
					Matrix.setIdentityM(tranformationMatrix, 0);
				}

				for (int i = 0; i < geometry.getNrVertices(); i += 3) {
					processExtends(boundingBox, tranformationMatrix, geometry.getVertex(i), geometry.getVertex(i + 1),
							geometry.getVertex(i + 2));
				}
			}
		} catch (RenderEngineException e) {
			e.printStackTrace();
		}
		return boundingBox;
	}
	private IfcOpenShellModel getRenderEngineModel(File ifcFile) throws RenderEngineException, IOException {
		String ifcGeomServerLocation = OperatingSystemCopyOf_IfcGeomServer.getIfcGeomServer();
		Path ifcGeomServerLocationPath = Paths.get(ifcGeomServerLocation);
		IfcOpenShellEngine ifcOpenShellEngine = new IfcOpenShellEngine(ifcGeomServerLocationPath, false, false);
		ifcOpenShellEngine.init();
		FileInputStream ifcFileInputStream = new FileInputStream(ifcFile);

		IfcOpenShellModel model = ifcOpenShellEngine.openModel(ifcFileInputStream);
		System.out.println("IfcOpenShell opens ifc: " + ifcFile.getAbsolutePath());
		RenderEngineSettings settings = new RenderEngineSettings();
		settings.setPrecision(Precision.SINGLE);
		settings.setIndexFormat(IndexFormat.AUTO_DETECT);
		settings.setGenerateNormals(false);
		settings.setGenerateTriangles(true);
		settings.setGenerateWireFrame(false);
		model.setSettings(settings);
		model.generateGeneralGeometry();
		return model;
	}

	

	
	private void processExtends(BoundingBox boundingBox, double[] transformationMatrix, double x, double y, double z) {
		double[] result = new double[4];
		Matrix.multiplyMV(result, 0, transformationMatrix, 0, new double[] { x, y, z, 1 }, 0);
		x = result[0];
		y = result[1];
		z = result[2];
		Point3d point = new Point3d(x / 1000, y / 1000, z / 1000);
		boundingBox.add(point);
	}

}