package p2;

import java.util.Set;

import bdi4jade.belief.*;
import bdi4jade.plan.planbody.BeliefGoalPlanBody;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.core.*;
import jade.domain.DFService;
import jade.domain.FIPAException;

public class BuscarAgentesPlanBody extends BeliefGoalPlanBody{

    @Override
    protected void execute() {
        SearchConstraints sc = new SearchConstraints();
		sc.setMaxResults((long) 100);
        DFAgentDescription[] players = new DFAgentDescription[0];
        try {
            DFAgentDescription dfdPlayer = new DFAgentDescription();
            ServiceDescription sdPlayer = new ServiceDescription();
            sdPlayer.setType("player");
            dfdPlayer.addServices(sdPlayer);
            players = DFService.search(myAgent, dfdPlayer, sc);
        } catch (FIPAException e) {
            System.out.println("[ERROR] Error en la busqueda de agentes Termometro");
        }

        BeliefBase beliefBase = getCapability().getBeliefBase();

        @SuppressWarnings("unchecked")
        Set<AID> agentesPendientes = (Set<AID>)beliefBase.getBelief("AgentesPendientes").getValue();
        @SuppressWarnings("unchecked")
        Set<AID> agentesJugando = (Set<AID>)beliefBase.getBelief("AgentesJugando").getValue();

        for (DFAgentDescription player : players) {
            AID playerAID = player.getName();
            if (!playerAID.equals(myAgent.getAID()) && !agentesJugando.contains(playerAID)) {
                agentesPendientes.add(player.getName());
            }
        }
        beliefBase.updateBelief("AgentesPendientes", agentesPendientes);
    }

}
