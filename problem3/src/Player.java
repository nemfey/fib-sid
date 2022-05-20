/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tutorial;

/**
 *
 * @author alumne
 */

import bdi4jade.core.*;
import bdi4jade.plan.*;
import bdi4jade.goal.*;
import bdi4jade.belief.*;
import java.util.*;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;

public class Player extends SingleCapabilityAgent {
    
    private static final String P3_URI = "http://www.semanticweb.org/alumne/ontologies/2022/4/";
    
    public Player() {
        GoalTemplate almacenarGoal = GoalTemplateFactory.hasBeliefValueOfType("Almacenado", Boolean.class);
	Plan almacenarPlan = new DefaultPlan(almacenarGoal, AlmacenarPlanBody.class);
        this.addGoal(new PredicateGoal("Almacenado", true));
        getCapability().getPlanLibrary().addPlan(almacenarPlan);
    }

    @Override
    public void takeDown() {
	System.out.println("Agente " + getLocalName() + " eliminado");
    }

    OntModel loadModel() {
        OntModel model;
        String JENAPath = "./";
        String OntologyFile = "p3.owl";
        String NamingContext = "ontologia";
        
        OntDocumentManager dm;
        model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_TRANS_INF);
        dm = model.getDocumentManager();
        dm.addAltEntry(NamingContext, "file:" + JENAPath + OntologyFile);
        model.read(NamingContext, "TURTLE");
            
        return model;
    }    
    
    public RDFNode getAgentPosition(OntModel model, String role) {
        
        OntClass p3Class = model.getOntClass(P3_URI + role);
        Iterator i = model.listIndividuals(p3Class).toList().iterator();
        Individual instance = (Individual) i.next();
        
        Property nameProperty = model.getProperty(P3_URI + "esta_en");
        RDFNode nameValue = instance.getPropertyValue(nameProperty);
        
        return nameValue;
    }

        
    @Override
    public void init() {
        System.out.println("Player created");
        
        Capability cap = this.getCapability();
        BeliefBase beliefBase = cap.getBeliefBase();

        // Acceso al mapa desde el belif del modelo

        OntModel modelo = loadModel();
        Belief<String, OntModel> beliefJenaModel = new TransientBelief<>("JenaModel", modelo);
        
        String rol = "Recolector";  // el rol del agente es recolector
        Belief<String, String> beliefRol = new TransientBelief<>("Rol", rol);

        RDFNode posicion = getAgentPosition(modelo, rol); // empieza en el nodo 5
        Belief<String, RDFNode> beliefPosicion = new TransientBelief<>("Posicion", posicion);
        
        beliefBase.addBelief(beliefRol);
        beliefBase.addBelief(beliefPosicion);
        beliefBase.addBelief(beliefJenaModel);
    }
}
