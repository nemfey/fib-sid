package practica;

import bdi4jade.belief.BeliefBase;
import bdi4jade.belief.TransientPredicate;
import bdi4jade.plan.planbody.BeliefGoalPlanBody;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;

@SuppressWarnings("serial")
public class SynchronisePlanBody extends BeliefGoalPlanBody {

    @Override
    protected void execute() {
    	BeliefBase beliefBase = getCapability().getBeliefBase();
    	int status = (int)beliefBase.getBelief("SynchroniseStatus").getValue();
    	if (status == 0) {
    		DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();   
            String environmentServiceType = (String)beliefBase.getBelief("EnvironmentServiceType").getValue();
            sd.setType(environmentServiceType);
            dfd.addServices(sd);
    		SearchConstraints sc = new SearchConstraints();
    		sc.setMaxResults((long) 1);
            try {
            	DFAgentDescription[] environmentAgent = DFService.search(myAgent, dfd, sc);
            	if (environmentAgent.length > 0) {
            		beliefBase.updateBelief("EnvironmentAgentAID", environmentAgent[0].getName());
            		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(environmentAgent[0].getName());
                    msg.setOntology("SynchroniseJTWW");
                    myAgent.send(msg);
                    
                    // AQUI SE ENVIAN LAS PETICIONES PARA IDENTIFICAR ATRIBUTOS DEL AGENTE SITUADO
                    msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(environmentAgent[0].getName());
                    msg.setOntology("EntityType");
                    myAgent.send(msg);
                    
                    msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(environmentAgent[0].getName());
                    msg.setOntology("TreasureType");
                    myAgent.send(msg);
                    
                    msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(environmentAgent[0].getName());
                    msg.setOntology("ExpertiseValue");
                    myAgent.send(msg);
                    
                    msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(environmentAgent[0].getName());
                    msg.setOntology("BackpackMaxSpace");
                    myAgent.send(msg);
                    
                    msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(environmentAgent[0].getName());
                    msg.setOntology("BackpackFreeSpace");
                    myAgent.send(msg);
                    ////////////////////////////////////////////////////////////////////////////////
                    
                    beliefBase.updateBelief("SynchroniseStatus", 1);
                }
            } catch (FIPAException e) {
                System.out.println("[ERROR] EnvironmentAgent not found");
            }
    	}
    	else if (status == 1) {
    		AID environmentAgentAID = (AID)beliefBase.getBelief("EnvironmentAgentAID").getValue();
    		MessageTemplate mt = MessageTemplate.MatchSender(environmentAgentAID);
    		ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
            	
            	// AQUI SE TRATAN LAS DISTINTAS RESPUESTAS DE ACUERDO A LAS PETICIONES ANTERIORES
            	switch (msg.getOntology()) {
            	case "EntityType":
            		System.out.println("\tEntityType set to " + msg.getContent());
            		beliefBase.updateBelief("EntityType", msg.getContent());
            		break;
            		
            	case "TreasureType":
            		System.out.println("\tTreasureType set to " + msg.getContent());
            		beliefBase.updateBelief("TreasureType", msg.getContent());
            		break;
            		
            	case "ExpertiseValue":
            		System.out.println("\tExpertiseValue set to " + msg.getContent());
            		beliefBase.updateBelief("ExpertiseValue", Integer.valueOf(msg.getContent()));
            		break;
            	
            	case "BackpackMaxSpace":
            		System.out.println("\tBackpackMaxSpace set to " + msg.getContent());
            		beliefBase.updateBelief("BackpackMaxSpace", Integer.valueOf(msg.getContent()));
            		break;
            		
            	case "BackpackFreeSpace":
            		System.out.println("\tBackpackFreeSpace set to " + msg.getContent());
            		beliefBase.updateBelief("BackpackFreeSpace", Integer.valueOf(msg.getContent()));
            		break;
            	/////////////////////////////////////////////////////////////////////////////////
            		
            	case "Synchronised":
            		beliefBase.addOrUpdateBelief(new TransientPredicate<>("Synchronised", true));
            		System.out.println("\t" + myAgent.getLocalName() + " has been synchronised with " + environmentAgentAID);
            		break;
            	default:
            		break;
            	}
            }
            
            if (beliefBase.getBelief("EntityType") != null &&
    			beliefBase.getBelief("TreasureType") != null && 
    			beliefBase.getBelief("ExpertiseValue") != null &&
    			beliefBase.getBelief("BackpackMaxSpace") != null &&
    			beliefBase.getBelief("BackpackFreeSpace") != null) {
    			msg = new ACLMessage(ACLMessage.INFORM);
    			msg.addReceiver(environmentAgentAID);
    			msg.setOntology("Synchronised");
    			myAgent.send(msg);
        	}
    	}
    }
}
