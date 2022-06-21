package practica;

import bdi4jade.core.*;
import bdi4jade.plan.*;
import dataStructures.tuple.Couple;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import bdi4jade.goal.*;
import bdi4jade.belief.*;

import java.util.*;

import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;

@SuppressWarnings("serial")
public class BDIAgent extends SingleCapabilityAgent {
	
	public BDIAgent() {
    	GoalTemplate synchronise = GoalTemplateFactory.hasBeliefValueOfType("Synchronised", Boolean.class);
		Plan synchronisePlan = new DefaultPlan(synchronise, SynchronisePlanBody.class);
		getCapability().getPlanLibrary().addPlan(synchronisePlan);
		
		GoalTemplate reason = GoalTemplateFactory.hasBeliefValueOfType("Finished", Boolean.class);
		Plan reasonPlan = new DefaultPlan(reason, ReasonPlanBody.class);
		getCapability().getPlanLibrary().addPlan(reasonPlan);
		
		Goal[] sequentialGoals = new Goal[2];
		sequentialGoals[0] = new PredicateGoal<>("Synchronised", true);
		sequentialGoals[1] = new PredicateGoal<>("Finished", true);
		this.addGoal(new SequentialGoal(sequentialGoals));
    }

    @Override
    public void takeDown() {
    	System.out.println("Agent " + getLocalName() + " killed");
    }
        
    @Override
    public void init() {
    	
    	String EnvironmentServiceType = "EnvironmentAgentJTWW";
    	
    	Object[] args = this.getArguments();
    	if (args.length == 1) {
    		EnvironmentServiceType = (String) args[0];
    	}
    	
    	Capability cap = this.getCapability();
		BeliefBase beliefBase = cap.getBeliefBase();
		
		// Synchronisation process
		Belief<String, Integer> synchroniseStatus = new TransientBelief<>("SynchroniseStatus", 0);
		beliefBase.addBelief(synchroniseStatus);
		Belief<String, AID> environmentAgentAID = new TransientBelief<>("EnvironmentAgentAID", null);
		beliefBase.addBelief(environmentAgentAID);
		Belief<String, String> environmentServiceType = new TransientBelief<>("EnvironmentServiceType", EnvironmentServiceType);
		beliefBase.addBelief(environmentServiceType);
		
		// Environment agent characteristics
		Belief<String, String> entityType = new TransientBelief<>("EntityType", null);
		beliefBase.addBelief(entityType);
		Belief<String, String> treasureType = new TransientBelief<>("TreasureType", null);
		beliefBase.addBelief(treasureType);
		Belief<String, Integer> backpackMaxSpace = new TransientBelief<>("BackpackMaxSpace", null);
		beliefBase.addBelief(backpackMaxSpace);
		Belief<String, Integer> expertiseValue = new TransientBelief<>("ExpertiseValue", null);
		beliefBase.addBelief(expertiseValue);
		Belief<String, Integer> backpackFreeSpace = new TransientBelief<>("BackpackFreeSpace", null);
		beliefBase.addBelief(backpackFreeSpace);
		
		// Common knowledge
		Belief<String, HashMap<String, Set<AID>>> foreignAgents = new TransientBelief<>("ForeignAgents", new HashMap<>());
		beliefBase.addBelief(foreignAgents);
		Belief<String, HashMap<AID, Couple<Long, String>>> agentPositions = new TransientBelief<>("AgentPositions", new HashMap<>());
		beliefBase.addBelief(agentPositions);
		Belief<String, HashMap<AID, String>> occupiedPositions = new TransientBelief<>("OccupiedPositions", new HashMap<>());
		beliefBase.addBelief(occupiedPositions);
		Belief<String, HashMap<String, Couple<Long, HashMap<Observation, Integer>>>> resourcesInformation = new TransientBelief<>("ResourcesInformation", new HashMap<>());
		beliefBase.addBelief(resourcesInformation);
		Belief<String, ACLMessage> broadcastMessage = new TransientBelief<>("BroadcastMessage", new ACLMessage(ACLMessage.INFORM));
		beliefBase.addBelief(broadcastMessage);
		Belief<String, HashMap<String, Set<String>>> map = new TransientBelief<>("Map", new HashMap<>());
		beliefBase.addBelief(map);
		Belief<String, MapRepresentation> mapRepresentation = new TransientBelief<>("MapRepresentation", null);
		beliefBase.addBelief(mapRepresentation);
		Belief<String, ArrayList<String>> openNodes = new TransientBelief<>("OpenNodes", new ArrayList<>());
		beliefBase.addBelief(openNodes);
		Belief<String, HashSet<String>> closedNodes = new TransientBelief<>("ClosedNodes", new HashSet<>());
		beliefBase.addBelief(closedNodes);
		
		// EXPLORER beliefs
		Belief<String, Boolean> firstTick = new TransientBelief<>("FirstTick", true);
		beliefBase.addBelief(firstTick);
		Belief<String, Boolean> exploring = new TransientBelief<>("Exploring", true);
		beliefBase.addBelief(exploring);
		Belief<String, Integer> ticks = new TransientBelief<>("Ticks", 0);
		beliefBase.addBelief(ticks);
		Belief<String, HashMap<String, Integer>> timesVisited = new TransientBelief<>("TimesVisited", new HashMap<>());
		beliefBase.addBelief(timesVisited);
		Belief<String, Integer> attempts = new TransientBelief<>("Attempts", 0);
		beliefBase.addBelief(attempts);
		Belief<String, String> selectedNode = new TransientBelief<>("SelectedNode", null);
		beliefBase.addBelief(selectedNode);
		Belief<String, String> previousLessVisited = new TransientBelief<>("PreviousLessVisitedNode", null);
		beliefBase.addBelief(previousLessVisited);
		Belief<String, Set<String>> golemPositions = new TransientBelief<>("GolemPositions", new HashSet<>());
		beliefBase.addBelief(golemPositions);
		Belief<String, Set<String>> windPositions = new TransientBelief<>("WindPositions", new HashSet<>());
		beliefBase.addBelief(windPositions);
		
		// TANKER beliefs
		Belief<String, HashSet<String>> visitedNodes = new TransientBelief<>("VisitedNodes", new HashSet<>());
		beliefBase.addBelief(visitedNodes);
    }
    
} 