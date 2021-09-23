# IFCtoB4R-DM_OpenAPI
Version 

An Restful OpenAPI implementation that converts an IFC Step file into Linked Building Data RDF defined in the B4R-DM ontology.
The output is compatible with Building Topology Ontology defined in https://w3c-lbd-cg.github.io/bot/. 


OpenAPI interface of the IFC-B4R DM converter accepts an IFC STEP formatted file as an input.
HTTP POST protocol is used where two content-types for input data are accepted.
They are plain text-based application/ifc that was defined in BIM4Ren WP2 D2.2.,
and MULTIPART_FORM_DATA Multimedia Internet Message Extensions (MIME).

The proposed IFC-B4R DM converter can handle IFC files from all actively used IFC schemas,
from IFC2x3 TC1 till IFC4x3. The converter first uses the existing IFC-to-RDF converter
internally to transform the original IFC file to a temporary ifcOWL Abox graph
according to the ifcOWL ontology.



## Attribution

This work uses bSDD buildingSMART Data Dictionary data that is licensed under Attribution-ShareAlike 4.0 International.

https://github.com/buildingSMART/bSDD

## A test installation:
http://lbd.arch.rwth-aachen.de/IFCtoB4R_OpenAPI/apidocs


## Swagger.json description for the services

http://lbd.arch.rwth-aachen.de/IFCtoB4R_OpenAPI/apidocs/ui/swagger.json

## Changes
- namespace fixes
- IFCtoLBD was added. BOT+SMLS has a separate REST interface 
- BIMserver integration is functional. Some beta testing is still needed.

### Docker for the Open API interface

Install Docker Desktop:  https://www.docker.com/get-started

https://hub.docker.com/repository/docker/jyrkioraskari/ifc2b4r

Command-line commands needed to start the server at your computer;
```
docker pull jyrkioraskari/ifc2b4r:latest

docker container run -it --publish 8081:8080 jyrkioraskari/ifc2b4r


```
Then the software can be accessed from the local web address:
http://localhost:8081/IFCtoB4R_OpenAPI


## How to cite
```
@software{jyrki_oraskari_2020_4046603,
  author       = {Jyrki Oraskari},
  title        = {{jyrkioraskari/IFCtoB4R-DM\_OpenAPI: Restful API for 
                   IFC - B4R-DM version 1.12}},
  month        = sep,
  year         = 2020,
  publisher    = {Zenodo},
  version      = {1.14},
  doi          = {10.5281/zenodo.4046603},
  url          = {https://doi.org/10.5281/zenodo.4046603}
}
```

## Acknowledgements
This work has been written in RWTH-Aachen. The research was funded by the EU through 
the H2020 project BIM4REN.

https://dc.rwth-aachen.de/de/forschung/bim4ren

