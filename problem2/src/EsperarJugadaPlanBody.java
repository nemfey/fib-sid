package p2;

import java.util.ArrayList;
import java.util.HashMap;

import bdi4jade.belief.*;
import bdi4jade.plan.planbody.BeliefGoalPlanBody;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class EsperarJugadaPlanBody extends BeliefGoalPlanBody{
    @Override
    protected void execute() {
        MessageTemplate mt = MessageTemplate.MatchOntology("play");
        ACLMessage  msg = myAgent.receive(mt);
        if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {

            String conversationId = msg.getConversationId();

            BeliefBase beliefBase = getCapability().getBeliefBase();

            @SuppressWarnings("unchecked")
            HashMap<String, ArrayList<String>> historico = (HashMap<String, ArrayList<String>>)this.getBeliefBase().getBelief("Historico").getValue();

            int penalizacion = ((Integer)this.getBeliefBase().getBelief("PenalizacionAcumulada").getValue());
            
            @SuppressWarnings("unchecked")
            ArrayList<Integer> C = (ArrayList<Integer>)beliefBase.getBelief("C").getValue();
            @SuppressWarnings("unchecked")
            ArrayList<Integer> D = (ArrayList<Integer>)beliefBase.getBelief("D").getValue();

            String content = msg.getContent();

            ACLMessage reply = msg.createReply();
            reply.setConversationId(conversationId);
            reply.setOntology("play");
            reply.setPerformative(ACLMessage.INFORM);

            String play = "";
            if (content != null) {
                ArrayList<String> aux = historico.get(conversationId);
                if (aux == null) {
                    aux = new ArrayList<>();
                    aux.add(content);
                    historico.put(conversationId, aux);
                }
                else {
                    historico.get(conversationId).add(content);
                }
                if (content.equals("C")) {
                    if (C.get(0) <= D.get(0)) {
                        play = "C";
                    }
                    else {
                        play = "D";
                    }
                }
                else if (content.equals("D")) {
                    if (C.get(1) <= D.get(1)) {
                        play = "C";
                    }
                    else {
                        play = "D";
                    }
                }
            }

            reply.setContent(play);
            myAgent.send(reply);

            //Actualizamos la penalizacion
            String myPlay = "";
            String rivalPlay = "";
            int historicSize = historico.get(conversationId).size();
            if (historicSize % 2 != 0) {
                myPlay = play;
                rivalPlay = historico.get(conversationId).get(historicSize-1);
            }
            else {
                myPlay = historico.get(conversationId).get(historicSize-1);
                rivalPlay = historico.get(conversationId).get(historicSize-2);
            }
            
            if(myPlay.equals("C") && rivalPlay.equals("C")) {
                penalizacion += C.get(0);
            }
            else if(myPlay.equals("C") && rivalPlay.equals("D")) penalizacion += C.get(1);
            else if(myPlay.equals("D") && rivalPlay.equals("C")) penalizacion += D.get(0);
            else if(myPlay.equals("D") && rivalPlay.equals("D")) penalizacion += D.get(1);

            historico.get(conversationId).add(play);
            
            this.getBeliefBase().updateBelief("Historico", historico);
            this.getBeliefBase().updateBelief("PenalizacionAcumulada", penalizacion);
        }
    }
    

}
