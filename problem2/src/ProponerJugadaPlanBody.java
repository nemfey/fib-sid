package src;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import bdi4jade.belief.*;
import bdi4jade.plan.planbody.BeliefGoalPlanBody;
import jade.lang.acl.ACLMessage;
import jade.core.*;

public class ProponerJugadaPlanBody extends BeliefGoalPlanBody{

    @Override
    protected void execute() {
        BeliefBase beliefBase = getCapability().getBeliefBase();

        @SuppressWarnings("unchecked")
        Set<AID> agentesPendientes = (Set<AID>)beliefBase.getBelief("AgentesPendientes").getValue();

        @SuppressWarnings("unchecked")
        HashMap<String, ArrayList<String>> historico = (HashMap<String, ArrayList<String>>)this.getBeliefBase().getBelief("Historico").getValue();

        if (!agentesPendientes.isEmpty()) {
            AID[] array = agentesPendientes.toArray(new AID[agentesPendientes.size()]);
            int index = new Random().nextInt(agentesPendientes.size());
            AID rivalAID = array[index];

            String conversationId = myAgent.getAID().toString() + " vs. " + rivalAID.toString();

            agentesPendientes.remove(rivalAID);
            beliefBase.updateBelief("AgentesPendientes", agentesPendientes);

            @SuppressWarnings("unchecked")
            Set<AID> agentesJugando = (Set<AID>)beliefBase.getBelief("AgentesJugando").getValue();
            agentesJugando.add(rivalAID);
            beliefBase.updateBelief("AgentesJugando", agentesJugando);

            @SuppressWarnings("unchecked")
            ArrayList<Integer> C = (ArrayList<Integer>)beliefBase.getBelief("C").getValue();
            @SuppressWarnings("unchecked")
            ArrayList<Integer> D = (ArrayList<Integer>)beliefBase.getBelief("D").getValue();
            int minC = C.get(0);
            if (C.get(1) < minC) minC = C.get(1);
            int minD = D.get(0);
            if (D.get(1) < minD) minD = D.get(1);

            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            String play = "";
            if (minC <= minD) {
                play = "C";
            }
            else {
                play = "D";
            }
            msg.setContent(play);
            msg.setOntology("play");
            msg.addReceiver(rivalAID);
            msg.setConversationId(conversationId);
            myAgent.send(msg);

            ArrayList<String> plays = new ArrayList<>();
            plays.add(play);
            historico.put(conversationId, plays);
            this.getBeliefBase().updateBelief("Historico", historico);

            System.out.println(myAgent.getLocalName() + ": He propuesto una jugada " + play + " a " + rivalAID);
        }
    }
    

}
