/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tutorial;

import bdi4jade.belief.BeliefBase;
import bdi4jade.belief.TransientBelief;
import bdi4jade.belief.TransientPredicate;
import bdi4jade.plan.planbody.BeliefGoalPlanBody;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;


/**
 *
 * @author jia.long.ji 
 */
public class AlmacenarPlanBody extends BeliefGoalPlanBody {
    
    private static final String P3_URI = "http://www.semanticweb.org/alumne/ontologies/2022/4/";
    String JENAPath = "./";
    String OntologyFile = "p3.owl";
    String NamingContext = "ontologia";
    
    public void releaseOntology(OntModel model) throws FileNotFoundException {
        System.out.println("· Releasing Ontology");
        if (!model.isClosed()) {
            model.write(new FileOutputStream(JENAPath + File.separator + "modified_" + OntologyFile, false));
            model.close();
            System.out.println("Model closed");
        }
    }
    
    @Override
    protected void execute() {
        
        // Coger nodo de belief
        BeliefBase beliefBase = getCapability().getBeliefBase();

        @SuppressWarnings("unchecked")
        OntModel modelo = (OntModel)beliefBase.getBelief("JenaModel").getValue();
        
        modelo = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF, modelo);
        
        Individual instance = modelo.getIndividual(P3_URI + "AgenteRecolector");
        
        if(instance.hasOntClass(P3_URI + "RecolectorDescarga")) {
            // Comprobamos si ya podemos descargar en el almacenador
            System.out.println("El recolector ya puede descargar");
            // Actualizar el belief de Almacenado
            this.getBeliefBase().addOrUpdateBelief(new TransientPredicate<>("Almacenado", true));
            // Actualizar el belief de JenaModel del modelo
            this.getBeliefBase().addOrUpdateBelief(new TransientBelief<>("JenaModel", modelo));
            // Actualizar y guardar la ontología
            try {
                releaseOntology(modelo);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(AlmacenarPlanBody.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else {
            // Movernos de Nodo5 a Nodo1
            System.out.println("El recolector se mueve al nodo 1");
            RDFNode posicionNueva = modelo.getIndividual(P3_URI + "Nodo1");
            // Actualizar el belief de posición
            this.getBeliefBase().addOrUpdateBelief(new TransientBelief<>("Posicion", posicionNueva));
            Property nameProperty = modelo.getProperty(P3_URI + "esta_en");
            instance.setPropertyValue(nameProperty, posicionNueva);
        }
    }
}
