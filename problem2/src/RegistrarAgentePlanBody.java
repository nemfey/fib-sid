package src;

import bdi4jade.belief.TransientPredicate;
import bdi4jade.plan.planbody.BeliefGoalPlanBody;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.DFService;
import jade.domain.FIPAException;

public class RegistrarAgentePlanBody extends BeliefGoalPlanBody {

    @Override
    protected void execute() {
        // Registro
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();   
        sd.setType("player"); 
        sd.setName(myAgent.getName());
        dfd.setName(myAgent.getAID());
        dfd.addServices(sd);
        try {
            DFService.register(myAgent,dfd);
            System.out.println("Agente " + myAgent.getLocalName() + " correctamente");
        } catch (FIPAException e) {
            System.out.println("[ERROR] El agente " + myAgent.getLocalName() + " no se ha podido registrar en el DF");
            myAgent.doDelete();
        }
        this.getBeliefBase().addOrUpdateBelief(new TransientPredicate<>("RegistrarAgente", true));
    }

    
}
