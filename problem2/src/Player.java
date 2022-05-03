package src;

import bdi4jade.core.*;
import bdi4jade.plan.*;
import bdi4jade.goal.*;
import jade.core.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import bdi4jade.belief.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.*;

public class Player extends SingleCapabilityAgent {

	private int TFT;

    public Player() {
		Set<Goal> parallelGoals = new HashSet<>();

		parallelGoals.add(new PredicateGoal<>("BuscarAgentes", true));
		parallelGoals.add(new PredicateGoal<>("ProponerJugada", true));
		parallelGoals.add(new PredicateGoal<>("EsperarJugada", true));

		Goal[] sequentialGoals = new Goal[2];

		sequentialGoals[0] = new PredicateGoal<>("RegistrarAgente", true);
		sequentialGoals[1] = new ParallelGoal(parallelGoals);

		this.addGoal(new SequentialGoal(sequentialGoals));

		GoalTemplate buscarAgentesGoal = GoalTemplateFactory.hasBeliefValueOfType("BuscarAgentes", Boolean.class);
		Plan buscarAgentesPlan = new DefaultPlan(buscarAgentesGoal, BuscarAgentesPlanBody.class); 
		getCapability().getPlanLibrary().addPlan(buscarAgentesPlan);

		GoalTemplate registrarAgenteGoal = GoalTemplateFactory.hasBeliefValueOfType("RegistrarAgente", Boolean.class);
		Plan registrarAgentePlan = new DefaultPlan(registrarAgenteGoal, RegistrarAgentePlanBody.class);
		getCapability().getPlanLibrary().addPlan(registrarAgentePlan);

    }

	public void takeDown() {
		System.out.println("Agente " + getLocalName() + " eliminado");

		try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            System.out.println("[ERROR] El agente " + getLocalName() + " no se ha podido desregistrar en el DF");
        }
	}

    public void init() {
    	Object[] args = getArguments();
		if (args.length != 5) {
			System.out.println("Usage: Player(CC, CD, DC, DD, TFT)\n\t- CC, CD, DC y DD son numeros enteros\n\t- TFT indica si se quiere utilizar (1) o no (0) la estrategia Tit for Tat");
			doDelete();
		}
		else {

			Capability cap = this.getCapability();
			BeliefBase beliefBase = cap.getBeliefBase();
			ArrayList<Integer> CCCD = new ArrayList<>();
			CCCD.add(Integer.parseInt((String)args[0]));
			CCCD.add(Integer.parseInt((String)args[1]));
			Belief<String, ArrayList<Integer>> C = new TransientBelief<>("C", CCCD);

			ArrayList<Integer> DDDC = new ArrayList<>();
			DDDC.add(Integer.parseInt((String)args[2]));
			DDDC.add(Integer.parseInt((String)args[3]));
			Belief<String, ArrayList<Integer>> D = new TransientBelief<>("D", DDDC);

			TFT = Integer.parseInt((String)args[4]);
			if (TFT != 0 && TFT != 1) {
				System.out.println("Usage: Player(CC, CD, DC, DD, TFT)\n\t- CC, CD, DC y DD son numeros enteros\n\t- TFT indica si se quiere utilizar (1) o no (0) la estrategia Tit for Tat");
				doDelete();
				return;
			}

			GoalTemplate proponerJugadaGoal = GoalTemplateFactory.hasBeliefValueOfType("ProponerJugada", Boolean.class);
			GoalTemplate esperarJugadaGoal = GoalTemplateFactory.hasBeliefValueOfType("EsperarJugada", Boolean.class);
			Plan proponerJugadaPlan;
			Plan esperarJugadaPlan;

			if (TFT == 1) {
				proponerJugadaPlan = new DefaultPlan(proponerJugadaGoal, ProponerJugadaTFTPlanBody.class);
				esperarJugadaPlan = new DefaultPlan(esperarJugadaGoal, EsperarJugadaTFTPlanBody.class);
			}
			else {
				proponerJugadaPlan = new DefaultPlan(proponerJugadaGoal, ProponerJugadaPlanBody.class);
				esperarJugadaPlan = new DefaultPlan(esperarJugadaGoal, EsperarJugadaPlanBody.class);
			}

			getCapability().getPlanLibrary().addPlan(proponerJugadaPlan);
			getCapability().getPlanLibrary().addPlan(esperarJugadaPlan);

			Belief<String, HashMap<String, ArrayList<String>>> historico = new TransientBelief<>("Historico", new HashMap<>());

			Belief<String, Integer> penalizacion = new TransientBelief<>("PenalizacionAcumulada", 0);

			Belief<String, Set<AID>> agentesPendientes = new TransientBelief<>("AgentesPendientes", new HashSet<>());

			Belief<String, Set<AID>> agentesJugando = new TransientBelief<>("AgentesJugando", new HashSet<>());

			beliefBase.addBelief(C);
			beliefBase.addBelief(D);
			beliefBase.addBelief(penalizacion);
			beliefBase.addBelief(historico);
			beliefBase.addBelief(agentesPendientes);
			beliefBase.addBelief(agentesJugando);
		}
    }
}
