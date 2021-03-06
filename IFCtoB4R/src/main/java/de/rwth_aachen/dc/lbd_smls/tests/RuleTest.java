package de.rwth_aachen.dc.lbd_smls.tests;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;

/**
 * Describe class <code>RuleTest</code> here.
 */
public class RuleTest {
    public static void main(String[] args) throws Exception {
        /* create model */
        Model model = ModelFactory.createDefaultModel();

        /* output model */
        System.out.println("original model : " + model);
        System.out.println("-----");

        /* collect rules */
        List<Rule> rules = new ArrayList<Rule>();
        Rule rule = Rule.parseRule("[ (subject predicate object) -> (object predicate subject) ].");
        rules.add(rule);
        System.out.println("rules: "+rules.size());
        /* create rule reasoner */
        GenericRuleReasoner reasoner = new GenericRuleReasoner(rules);

        /* attach reasoner to model */
        InfModel infModel = ModelFactory.createInfModel(reasoner, model);

        /* output model */
        //-----------------------------------------------//
        // commenting the following line in/out changes  //
        // the output of (*) below in Jena 2.5.5 ?!?!?!  //
        //-----------------------------------------------//
        //System.out.println("inference model: " + infModel);        
        System.out.println("=====");

        /* add facts to original model */
        Resource s = model.createResource("subject");
        Property p = model.createProperty("predicate");
        RDFNode  o = model.createResource("object");
        Statement stmt = model.createStatement(s, p, o);
        model.add(stmt);

        /* output models */
        System.out.println("original model : " + model);
        System.out.println("-----");
        System.out.println("inference model: " + infModel); // (*)
    }
}