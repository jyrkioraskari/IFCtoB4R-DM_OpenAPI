from requests import get
from urllib.parse import quote

IFC = "http://standards.buildingsmart.org/IFC/DEV/IFC4_1/OWL#"

PREFIXES = ["prefix owl: <http://www.w3.org/2002/07/owl#>",
  "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>",
  "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
  "prefix ifc: <"+IFC+">"]

PATTERN = "[ifcToLbd[XXX]-[YYY]:\
    (?z rdf:type ifc:Ifc[XXX]) (?z ifc:predefinedType_[XXX] ifc:[YYY])\
    -> (?z rdf:type goal:[XXX]-[YYY])."

IFCOWL_ENDPOINT = 'http://localhost:3030/ifcOwl/query'
SPARQL_QUERY = """select ?distribType (group_concat(DISTINCT ?value; separator=' & ') as ?values) where {\
  ?distribType rdfs:subClassOf* ifc:[PARENT_CLASS].\
  ?class rdfs:subClassOf <https://w3id.org/express#ENUMERATION>.\
  ?predefinedType rdfs:domain ?distribType;\
  				  rdfs:range ?class.\
  ?value a ?class.\
  FILTER( regex(str(?predefinedType), "predefinedType_.+"))
} group by ?distribType"""

DISTRIBUTION = 'IfcDistributionElement'
BUILDING = 'IfcBuildingElement'

ONTOLOGIES = {DISTRIBUTION: 'https://pi.pauwel.be/voc/distributionelement#',
    BUILDING: 'https://pi.pauwel.be/voc/buildingelement#'}

PARENT_CLASS = [DISTRIBUTION, BUILDING]

def remove_prefix(text, prefix):
    '''Simple process of the URIs, to remove the prefix.'''
    if text.startswith(prefix):
        return text[len(prefix):]
    return text

def preprocess(result):
    '''Preprocess results from the SPARQL query.
    The query must specify that the class name is separated from the enum names
    with a comma, and that enum names are separated with a commercial and.'''
    k, v = result.split(',')
    return remove_prefix(k, IFC+'Ifc'), [remove_prefix(val, IFC) for val in v.split(' & ')]

def extract_enums(parent_class):
    '''Operation to obtain all the enum names for each class in the IFC schema'''
    # execute query on endpoint
    query = SPARQL_QUERY.replace('[PARENT_CLASS]', parent_class)
    r = get(
        IFCOWL_ENDPOINT + "?query=" + quote('\n'.join(PREFIXES)+'\n'+query),
        headers={"Accept": "text/csv"})
    r.encoding = 'utf-8'
    res = {}
    if r.status_code == 200:
        # create dictionary enum values per class
        lines = r.text.encode("utf-8").splitlines()
        lines.pop(0)
        for line in lines:
            k, v = preprocess(line.decode('utf-8'))
            res[k] = v
    return res

def create_rule(classname, enumname):
    return PATTERN.replace('[XXX]', classname).replace('[YYY]', enumname)

def create_rules(dico):
    str = '\n'.join(['@'+prefix+'.' for prefix in PREFIXES]) + '\n'
    for classname, enums in dico.items():
        str += '\n'
        for enum in enums:
            str += create_rule(classname, enum) + '\n'
    return str

if __name__ == "__main__":
    for parent_class in PARENT_CLASS:
        rules = "prefix goal:<"+ONTOLOGIES[parent_class]+">.\n"
        rules += create_rules(extract_enums(parent_class))
        with open("IFCto"+parent_class+".swrl","w+") as f:
            f.write(rules)
