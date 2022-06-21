package practica;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Map;

import bdi4jade.belief.BeliefBase;
import bdi4jade.plan.planbody.BeliefGoalPlanBody;
import dataStructures.serializableGraph.SerializableNode;
import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.util.leap.Iterator;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;

@SuppressWarnings("serial")
public class ReasonPlanBody extends BeliefGoalPlanBody {
    
    
    private String nextNodeInPath(HashMap<String, String> paths, String currentPos, String finalPos) {
        String previousNode = paths.get(finalPos);
        if (previousNode == null) {
        	// This should not happen, but I treat it just in case
            //System.out.println("ERROR EN PREVIOUSNODE:");
            //System.out.println(paths);
            //System.out.println("I want to go from " + currentPos + " to " + finalPos);
            return currentPos;
        }
        if (previousNode.equals(currentPos)) return finalPos;
        return nextNodeInPath(paths, currentPos, previousNode);
     }
    
    // Gets the shortest path using BFS
    @SuppressWarnings("unchecked")
	private boolean getShortestPath(HashMap<String, Set<String>> map, String currentPos, String finalPos, HashMap<String, String> paths) {
        if (finalPos == null) return false;
        BeliefBase beliefBase = getCapability().getBeliefBase();
        HashMap<AID, String> occupiedPositions = (HashMap<AID, String>) beliefBase.getBelief("OccupiedPositions").getValue();
        Set<String> golemPositions = (Set<String>) beliefBase.getBelief("GolemPositions").getValue();
        //System.out.println("There are agents in " + occupiedPositions.values());
        Queue<String> pending = new LinkedList<>();
        
        pending.add(currentPos);
        paths.put(currentPos, currentPos);
         
        while (!pending.isEmpty()) {
            String position = pending.poll();
            Set<String> adjacencies = map.get(position);
            if (adjacencies == null) {
            	// This should not happen, but I treat it just in case
                //System.out.println(myAgent.getLocalName() + " ERROR WAS PROVOKED IN POSITION " + position);
                //System.out.println("CURRENTPOS="+currentPos+", FINALPOS="+finalPos);
                //System.out.println(map);
            	return false;
            }
            for (String adjacency : adjacencies) {
                if (!occupiedPositions.containsValue(adjacency) && !golemPositions.contains(adjacency) && !paths.containsKey(adjacency)) {
                    pending.add(adjacency);
                    paths.put(adjacency, position);
                    if (adjacency.equals(finalPos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    @SuppressWarnings("unchecked")
	private String getShortestPathToTanker(HashMap<String, Set<String>> map, String currentPos, String finalPos, HashMap<String, String> paths) {
        if (finalPos == null) return null;
        BeliefBase beliefBase = getCapability().getBeliefBase();
        HashMap<AID, String> occupiedPositions = (HashMap<AID, String>) beliefBase.getBelief("OccupiedPositions").getValue();
        Set<String> golemPositions = (Set<String>) beliefBase.getBelief("GolemPositions").getValue();
        //System.out.println("There are agents in " + occupiedPositions.values());
        Queue<String> pending = new LinkedList<>();
        
        pending.add(currentPos);
        paths.put(currentPos, currentPos);
         
        while (!pending.isEmpty()) {
            String position = pending.poll();
            Set<String> adjacencies = map.get(position);
            if (adjacencies == null) {
                //System.out.println(myAgent.getLocalName() + " ERROR WAS PROVOKED IN POSITION " + position);
                //System.out.println("CURRENTPOS="+currentPos+", FINALPOS="+finalPos);
                //System.out.println(map);
                return currentPos;
            }
            else {
                for (String adjacency : adjacencies) {
                    if (adjacency.equals(finalPos)) {
                        return position;
                    }
                    if (!occupiedPositions.containsValue(adjacency) && !golemPositions.contains(adjacency) && !paths.containsKey(adjacency)) {
                        pending.add(adjacency);
                        paths.put(adjacency, position);
                    }
                }
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private String doDFS(List<Couple<String,List<Couple<Observation,Integer>>>> observation) {
        BeliefBase beliefBase = getCapability().getBeliefBase();
        HashMap<String, Set<String>> map = (HashMap<String, Set<String>>) beliefBase.getBelief("Map").getValue();
        
        Set<String> closedNodes = (Set<String>)beliefBase.getBelief("ClosedNodes").getValue();
        List<String> openNodes = (List<String>)beliefBase.getBelief("OpenNodes").getValue();
        MapRepresentation mapRepresentation = (MapRepresentation)beliefBase.getBelief("MapRepresentation").getValue();
        
        if (mapRepresentation == null) {
            beliefBase.updateBelief("MapRepresentation",new MapRepresentation());
            mapRepresentation = (MapRepresentation)beliefBase.getBelief("MapRepresentation").getValue();
        }
        
        String currentPosition = observation.get(0).getLeft();
        closedNodes.add(currentPosition);
        openNodes.remove(currentPosition);
        
        // Initialize new node if not in the map
        if (!map.keySet().contains(currentPosition)) {
            map.put(currentPosition, new HashSet<String>());
        }
        
        mapRepresentation.addNode(currentPosition,MapAttribute.closed);
        
        String nextNode = null;
        
        boolean inWind = false;
        for (Couple<Observation, Integer> obs : observation.get(0).getRight()) {
            if (obs.getLeft() == Observation.WIND) {
                inWind = true;
                break;
            }
        }
        
        for (int i = 1; i < observation.size(); ++i) {
        	boolean safe = true;
        	// Looking for safety in each adjacency
            for (Couple<Observation, Integer> obs : observation.get(i).getRight()) {
                if (obs.getLeft() == Observation.WIND && inWind) {
                    safe = false;
                    break;
                }
            }
            // If the node is safe, add it
            if (safe) {
            	String adjacency = observation.get(i).getLeft();
                if (!closedNodes.contains(adjacency)){
                    if (!openNodes.contains(adjacency)){
                        openNodes.add(adjacency);
                        mapRepresentation.addNode(adjacency, MapAttribute.open);
                        mapRepresentation.addEdge(currentPosition, adjacency);    
                        map.put(adjacency, new HashSet<>());
                        map.get(adjacency).add(currentPosition);
                        map.get(currentPosition).add(adjacency);
                    } else {
                        //the node exist, but not necessarily the edge
                        mapRepresentation.addEdge(currentPosition, adjacency);
                        map.get(adjacency).add(currentPosition);
                        map.get(currentPosition).add(adjacency);
                    }
                    if (nextNode == null) nextNode = adjacency;
                }
                // This is for the case an explorer agent spawns in wind position: even though after sharing a map a certain node is closed, does not mean that adjacencies have been added.
                else if (map.get(adjacency).contains(currentPosition) || !map.get(currentPosition).contains(adjacency)) {
                    mapRepresentation.addEdge(currentPosition, adjacency);
                    map.get(adjacency).add(currentPosition);
                    map.get(currentPosition).add(adjacency);
                }
            }
        }
        
        if (openNodes.isEmpty()){
            return null;
        }
        else {
            HashMap<String, String> paths = new HashMap<>();
            boolean found = getShortestPath(map, currentPosition, nextNode, paths);
            
            int index = 0;
            while (index < openNodes.size() && !found) {
                nextNode = openNodes.get(index);
                found = getShortestPath(map, currentPosition, nextNode, paths);
                if (!found) {
                    paths = new HashMap<>();
                    ++index;
                }
            }
            if (found) {
                // Next node should be selected next time a DFS is done, so I I put it in the head of the list
                if (index > 0) {
                    openNodes.add(0, openNodes.remove(index));
                    //System.out.println("Prioritising now " + openNodes.get(0) + ". Open nodes are " + openNodes);
                }
                return nextNodeInPath(paths, currentPosition, nextNode);
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
	private void treatDFNotification(ACLMessage containedMessage) {
        BeliefBase beliefBase = getCapability().getBeliefBase();
        try {
            DFAgentDescription[] dfds = DFService.decodeNotification(containedMessage.getContent());
            if (dfds.length > 0) {
                for (DFAgentDescription dfd : dfds) {
                    if (!dfd.getName().equals(containedMessage.getSender())) {
                        for (Iterator iter = dfd.getAllServices(); iter.hasNext(); ) {
                            ServiceDescription sd = (ServiceDescription) iter.next();
                            if (sd.getType().equals("agentExplo") || sd.getType().equals("agentCollect") || sd.getType().equals("agentTanker") || sd.getType().equals("agentGold") || sd.getType().equals("agentDiamond")) {
                                HashMap<String, Set<AID>> foreignAgents = (HashMap<String, Set<AID>>) beliefBase.getBelief("ForeignAgents").getValue();

                                if (!foreignAgents.containsKey(sd.getType())) {
                                    Set<AID> set = new HashSet<AID>();
                                    set.add(dfd.getName());
                                    foreignAgents.put(sd.getType(), set);
                                } else {
                                    foreignAgents.get(sd.getType()).add(dfd.getName());
                                }
                                ACLMessage broadcast = (ACLMessage) beliefBase.getBelief("BroadcastMessage").getValue();
                                boolean newAID = true;
                                Iterator it = broadcast.getAllReceiver();
                                while (it.hasNext()) {
                                    AID ag = (AID) it.next();
                                    if (ag.equals(dfd.getName())) {
                                        newAID = false;
                                    }
                                }
                                if(newAID) broadcast.addReceiver(dfd.getName());
                                
                                System.out.println("  --  Agent " + dfd.getName().getLocalName() + " added to list of " + sd.getType() + " by " + myAgent.getLocalName());
                            }
                        }
                    }
                }
            }
            HashMap<String, Set<AID>> foreignAgents = (HashMap<String, Set<AID>>) beliefBase.getBelief("ForeignAgents").getValue();
            Set<AID> collectors = foreignAgents.get("agentCollect");
            Set<AID> tankers = foreignAgents.get("agentTanker");
            if (collectors == null) collectors = new HashSet<>();
            if (tankers == null) tankers = new HashSet<>();
            if (!foreignAgents.containsKey("agentTankerDiamond")) foreignAgents.put("agentTankerDiamond", new HashSet<>());
            if (!foreignAgents.containsKey("agentTankerGold")) foreignAgents.put("agentTankerGold", new HashSet<>());
            if (!foreignAgents.containsKey("agentCollectDiamond")) foreignAgents.put("agentCollectDiamond", new HashSet<>());
            if (!foreignAgents.containsKey("agentCollectGold")) foreignAgents.put("agentCollectGold", new HashSet<>());
            Set<AID> diamond = foreignAgents.get("agentDiamond");
            if (diamond != null) {
                for (AID ag : diamond) {
                    if (collectors.contains(ag)) foreignAgents.get("agentCollectDiamond").add(ag);
                    else if (tankers.contains(ag)) foreignAgents.get("agentTankerDiamond").add(ag);
                }
            }
            Set<AID> gold = foreignAgents.get("agentGold");
            if (gold != null) {
                for (AID ag : gold) {
                    if (collectors.contains(ag)) foreignAgents.get("agentCollectGold").add(ag);
                    else if (tankers.contains(ag)) foreignAgents.get("agentTankerGold").add(ag);
                }
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
	private void treatAgentPositions(ACLMessage containedMessage) {
        BeliefBase beliefBase = getCapability().getBeliefBase();
        HashMap<AID, Couple<Long, String>> agentPositions = (HashMap<AID, Couple<Long, String>>) beliefBase.getBelief("AgentPositions").getValue();
        HashMap<AID, String> occupiedPositions = (HashMap<AID, String>) beliefBase.getBelief("OccupiedPositions").getValue();
        HashMap<AID, Couple<Long, String>> receivedPositions = null;
        try {
            receivedPositions = (HashMap<AID, Couple<Long, String>>) containedMessage.getContentObject();
            for (AID key : receivedPositions.keySet()) {
                if (!agentPositions.containsKey(key)) {
                    agentPositions.put(key, receivedPositions.get(key));
                    occupiedPositions.put(key, receivedPositions.get(key).getRight());
                }
                else {
                    Long receivedTime = receivedPositions.get(key).getLeft();
                    Long savedTime = agentPositions.get(key).getLeft();
                    if (receivedTime > savedTime) {
                        agentPositions.put(key, receivedPositions.get(key));
                    }
                }
                // Not 100% reliable but better than putting all positions
            }
            AID sender = containedMessage.getSender();
            if (agentPositions.containsKey(sender)) {
            	occupiedPositions.put(containedMessage.getSender(), agentPositions.get(containedMessage.getSender()).getRight());
            }
        } catch (UnreadableException e1) {
            e1.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
	private void sendAgentPositions() {
        BeliefBase beliefBase = getCapability().getBeliefBase();
        AID environmentAgentAID = (AID)beliefBase.getBelief("EnvironmentAgentAID").getValue();
        HashMap<AID, Couple<Long, String>> agentPositions = (HashMap<AID, Couple<Long, String>>) beliefBase.getBelief("AgentPositions").getValue();
        ACLMessage broadcast = ((ACLMessage) beliefBase.getBelief("BroadcastMessage").getValue()).shallowClone();
        broadcast.removeReceiver(environmentAgentAID);
        // DECISION
        broadcast.setOntology("agentPositions");
        try {
            ACLMessage msgWithMsg;
            broadcast.setContentObject((Serializable) agentPositions);
            broadcast.setSender(environmentAgentAID);
            msgWithMsg = new ACLMessage(ACLMessage.INFORM);
            msgWithMsg.setOntology("SendAgentPositions");
            msgWithMsg.addReceiver(environmentAgentAID);
            try {
                msgWithMsg.setContentObject((Serializable)broadcast);
            } catch (IOException e) {
                e.printStackTrace();
            }
            myAgent.send(msgWithMsg);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
	private void treatResourcesInformation(ACLMessage containedMessage) {
        BeliefBase beliefBase = getCapability().getBeliefBase();
        HashMap<String, Couple<Long, HashMap<Observation, Integer>>> receivedInformation = null;
        try {
            receivedInformation = (HashMap<String, Couple<Long, HashMap<Observation, Integer>>>) containedMessage.getContentObject();
            HashMap<String, Couple<Long, HashMap<Observation, Integer>>> resourcesInformation = (HashMap<String, Couple<Long, HashMap<Observation, Integer>>>)beliefBase.getBelief("ResourcesInformation").getValue();
            if (receivedInformation != null) {
            	for (String pos : receivedInformation.keySet()) {
                    if (!resourcesInformation.containsKey(pos)) {
                        resourcesInformation.put(pos, receivedInformation.get(pos));
                    }
                    else {
                        Long receivedTime = receivedInformation.get(pos).getLeft();
                        Long savedTime = resourcesInformation.get(pos).getLeft();
                        if (receivedTime > savedTime) {
                            resourcesInformation.put(pos, receivedInformation.get(pos));
                        }
                    }
                }
            }
        } catch (UnreadableException e2) {
            e2.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
	private void sendResourcesInformation() {
        BeliefBase beliefBase = getCapability().getBeliefBase();
        AID environmentAgentAID = (AID)beliefBase.getBelief("EnvironmentAgentAID").getValue();
        ACLMessage broadcast = ((ACLMessage) beliefBase.getBelief("BroadcastMessage").getValue()).shallowClone();
        broadcast.setOntology("resourceInformation");
        HashMap<String, Couple<Long, HashMap<Observation, Integer>>> resourcesInformation = (HashMap<String, Couple<Long, HashMap<Observation, Integer>>>)beliefBase.getBelief("ResourcesInformation").getValue();
        try {
            broadcast.setContentObject((Serializable) resourcesInformation);
            broadcast.setSender(environmentAgentAID);
            ACLMessage msgWithMsg;
            msgWithMsg = new ACLMessage(ACLMessage.INFORM);
            msgWithMsg.setOntology("SendResourcesInformation");
            msgWithMsg.addReceiver(environmentAgentAID);
            try {
                msgWithMsg.setContentObject((Serializable)broadcast);
            } catch (IOException e) {
                e.printStackTrace();
            }
            myAgent.send(msgWithMsg);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
	private void treatMapTopology(ACLMessage containedMessage) {
        BeliefBase beliefBase = getCapability().getBeliefBase();
        MapRepresentation mapRepresentation = (MapRepresentation)beliefBase.getBelief("MapRepresentation").getValue();
        HashMap<String, Set<String>> map = (HashMap<String, Set<String>>)beliefBase.getBelief("Map").getValue();
        Set<String> closedNodes = (Set<String>)beliefBase.getBelief("ClosedNodes").getValue();
        List<String> openNodes = (List<String>)beliefBase.getBelief("OpenNodes").getValue();
        try {
            SerializableSimpleGraph<String,MapAttribute> receivedMap = (SerializableSimpleGraph<String,MapAttribute>)containedMessage.getContentObject();
            if (receivedMap != null) {
            	mapRepresentation.mergeMap(receivedMap);
                Set<SerializableNode<String, MapAttribute>> nodes = receivedMap.getAllNodes();
                for (SerializableNode<String,MapAttribute> node : nodes) {
                    String id = node.getNodeId();
                    if (!closedNodes.contains(id)) {
                        // Only closed nodes are reliable. Also: if a node is closed, all its adjacencies are added
                        Set<String> adjacencies = receivedMap.getEdges(id);
                        map.put(id, adjacencies);
                        for (String adj : adjacencies) {
                            if (map.containsKey(adj)) {
                                map.get(adj).add(id);
                            }
                        }
                        MapAttribute atr = node.getNodeContent();
                        if (atr.equals(MapAttribute.closed)) {
                            openNodes.remove(id);
                            closedNodes.add(id);
                            HashMap<String, Integer> timesVisited = ((HashMap<String, Integer>) beliefBase.getBelief("TimesVisited").getValue());
                            if (!timesVisited.containsKey(id)) {
                                timesVisited.put(id, 0); 
                            }
                        }
                        else {
                            if (!openNodes.contains(id)) {
                                openNodes.add(id);
                            }
                        }
                    }
                }
            }
        } catch (UnreadableException e) {
            e.printStackTrace();
        }
    }
    
    private void sendMapTopology() {
        BeliefBase beliefBase = getCapability().getBeliefBase();
        AID environmentAgentAID = (AID)beliefBase.getBelief("EnvironmentAgentAID").getValue();
        ACLMessage broadcast = ((ACLMessage) beliefBase.getBelief("BroadcastMessage").getValue()).shallowClone();
        broadcast.setOntology("mapTopology");
        MapRepresentation mapRepresentation = (MapRepresentation)beliefBase.getBelief("MapRepresentation").getValue();
        try {
            broadcast.setContentObject(mapRepresentation.getSerializableGraph());
            broadcast.setSender(environmentAgentAID);
            ACLMessage msgWithMsg = new ACLMessage(ACLMessage.INFORM);
            msgWithMsg.setOntology("SendMapTopology");
            msgWithMsg.addReceiver(environmentAgentAID);
            try {
                msgWithMsg.setContentObject((Serializable)broadcast);
            } catch (IOException e) {
                e.printStackTrace();
            }
            myAgent.send(msgWithMsg);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
	private void treatObservationExplorer(List<Couple<String, List<Couple<Observation, Integer>>>> observation) {
        BeliefBase beliefBase = getCapability().getBeliefBase();
        
        AID environmentAgentAID = (AID)beliefBase.getBelief("EnvironmentAgentAID").getValue();
        String currentPosition = observation.get(0).getLeft();
        
        // Initialize mapRepresentation
        MapRepresentation mapRepresentation = (MapRepresentation)beliefBase.getBelief("MapRepresentation").getValue();
        if (mapRepresentation == null) {
            beliefBase.updateBelief("MapRepresentation",new MapRepresentation());
            mapRepresentation = (MapRepresentation)beliefBase.getBelief("MapRepresentation").getValue();
        }
        
        // This will help to remove inaccurate information of positions occupied so an agent can avoid getting stuck
        HashMap<AID, String> occupiedPositions = ((HashMap<AID, String>) beliefBase.getBelief("OccupiedPositions").getValue());
        Set<String> golemPositions = (Set<String>) beliefBase.getBelief("GolemPositions").getValue();
        int ticks = (int)beliefBase.getBelief("Ticks").getValue();
        if (ticks == 50) {
            ticks = 0;
            beliefBase.updateBelief("Ticks", 0);
            occupiedPositions.clear();
            golemPositions.clear();
        }
        else beliefBase.updateBelief("Ticks", ticks+1);
        
        List<String> openNodes = (List<String>)beliefBase.getBelief("OpenNodes").getValue();
        Set<String> closedNodes = (Set<String>)beliefBase.getBelief("ClosedNodes").getValue();
        
        // Wind nodes will always be treated as occupied positions, in case someone sends a well position as an open node
        // UNCOMMENT THIS IF I END UP NOT TRUSTING OTHER CLASSMATES
        Set<String> windPositions = (Set<String>)beliefBase.getBelief("WindPositions").getValue();
        /*
        for (int i = 1; i < observation.size(); ++i) {
        	String node = observation.get(i).getLeft();
        	for (Couple<Observation, Integer> obs : observation.get(i).getRight()) {
        		if (obs.getLeft() == Observation.WIND && !windPositions.contains(node)) {
        			windPositions.add(node);
        			mapRepresentation.addNode(node, MapAttribute.closed);
        			mapRepresentation.addEdge(node, currentPosition);
        			if (!closedNodes.contains(node)) {
        				openNodes.remove(node);
        				closedNodes.add(node);
        				HashMap<String, Set<String>> map = (HashMap<String, Set<String>>) beliefBase.getBelief("Map").getValue();
        				if (!map.containsKey(node)) {
        					map.put(node, new HashSet<>());
        				}
        				map.get(node).add(currentPosition);
        				if (!map.containsKey(currentPosition)) {
        					map.put(currentPosition, new HashSet<>());
        				}
        				map.get(currentPosition).add(node);
        			}
        		}
        	}
        }
        */
        
        
        
        HashMap<String, Integer> timesVisited = ((HashMap<String, Integer>) beliefBase.getBelief("TimesVisited").getValue());
        Boolean exploring = (Boolean)beliefBase.getBelief("Exploring").getValue();
        Boolean firstTick = (Boolean)beliefBase.getBelief("FirstTick").getValue();
        // PLAN A: TRY TO VISIT ALL OPEN NODES
        if (exploring && (firstTick || openNodes.size() > 0)) {
        	beliefBase.updateBelief("FirstTick", false);
            // Increment the current node visits: this will help for plan B, which goal is to reach the less visited nodes
            if (!timesVisited.containsKey(currentPosition)) {
                timesVisited.put(currentPosition, 1); 
            }
            else {
                timesVisited.put(currentPosition, timesVisited.get(currentPosition)+1);
            }
            
            String nextNode = doDFS(observation);
            if (nextNode != null) {
                ACLMessage nodeMsg = new ACLMessage(ACLMessage.PROPOSE);
                nodeMsg.setOntology("MoveTo");
                try {
                    nodeMsg.setContentObject((Serializable) new Couple<String, String>(currentPosition, nextNode));
                    nodeMsg.addReceiver(environmentAgentAID);
                    myAgent.send(nodeMsg);
                    
                    String selectedNode = (String)beliefBase.getBelief("SelectedNode").getValue();
                    if (selectedNode == null) {
                        beliefBase.updateBelief("SelectedNode", nextNode);
                    }
                    // WANTING TO ATTEMPT TO GO TO THE SAME NODE AS BEFORE
                    else if (selectedNode.equals(nextNode)) {
                        int attempts = (int)beliefBase.getBelief("Attempts").getValue();
                        beliefBase.updateBelief("Attempts", attempts + 1);
                    }
                    else {
                        beliefBase.updateBelief("SelectedNode", nextNode);
                        beliefBase.updateBelief("Attempts", 0);
                    }
                    
                    // DETECT GOLEMS WHEN GETTING STUCK
                    if ((boolean)beliefBase.getBelief("Attempts").getValue().equals(10)) {
                        golemPositions.add(nextNode);
                    }
                    else if ((boolean)beliefBase.getBelief("Attempts").getValue().equals(100)) {
                        System.out.println(myAgent.getLocalName() + " too many attempts, changing to plan B");
                        beliefBase.updateBelief("Exploring", false);
                        beliefBase.updateBelief("Attempts", 0);
                    }
                    
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                // If the agent does not move, it will also count as an attempt to move
                int attempts = (int)beliefBase.getBelief("Attempts").getValue();
                beliefBase.updateBelief("Attempts", attempts + 1);
                if ((boolean)beliefBase.getBelief("Attempts").getValue().equals(100)) {
                    System.out.println(myAgent.getLocalName() + " too many attempts, changing to plan B");
                    beliefBase.updateBelief("Exploring", false);
                    beliefBase.updateBelief("Attempts", 0);
                }
            }
        }
        // PLAN B: ALL NODES ARE CLOSED OR GOT STUCK WITH PLAN A
        else {
        	HashMap<String, Set<String>> map = (HashMap<String, Set<String>>)beliefBase.getBelief("Map").getValue();
        	// Since in plan B we can still visit an open node, we need to treat it
        	String nextNode = null;
        	if (openNodes.contains(currentPosition)) {
        		nextNode = doDFS(observation);
        	}
        	
        	// Let's check if there is an adjacent open node first, if so, try to go there
        	HashMap<String,String> paths = new HashMap<>();
        	if (nextNode == null) {
        		for (int i = 1; i < observation.size(); ++i) {
                	String node = observation.get(i).getLeft();
                	if (openNodes.contains(node) && getShortestPath(map,currentPosition,node,paths)) {
                		nextNode = node;
                		break;
                	}
                	paths = new HashMap<>();
                }
        	}
        	
        	// Otherwise, just go to the first available less visited node
        	if (nextNode == null) {
        		LinkedHashMap<String, Integer> sortedMap = new LinkedHashMap<>();
                timesVisited.entrySet().stream().sorted(Map.Entry.comparingByValue())
                    .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
                
                paths = new HashMap<>();
                Set<String> nodes = sortedMap.keySet();
                for (String node : nodes) {
                    if (!windPositions.contains(node) && !node.equals(currentPosition) && getShortestPath(map,currentPosition,node,paths)) {
                        nextNode = node;
                        break;
                    }
                    paths = new HashMap<>();
                }
        	}
                
            if (nextNode != null) {
            	if (!timesVisited.containsKey(nextNode)) {
        			timesVisited.put(nextNode, 0);
        		}
                
                String previousNode = (String)beliefBase.getBelief("PreviousLessVisitedNode").getValue();
                if (previousNode == null) {
                    beliefBase.updateBelief("PreviousLessVisitedNode", nextNode);
                }
                // Whenever there is a change in the previous less visited node, if changes are being very frequent
                // it will mean that the agent is loop-selecting two nodes, so incrementing those nodes in every change even though it has
                // not been reached yet, will avoid selecting them again so a different less visited node will be selected.
                else if (!nextNode.equals(previousNode)) {
                    timesVisited.put(previousNode, timesVisited.get(previousNode)+5);
                    beliefBase.updateBelief("PreviousLessVisitedNode", nextNode);
                }
                // Sometimes the nextNode is already calculated in doDFS(), so no nextNodeInPath() invocation is needed
                if (paths.size() > 0) {
                	nextNode = nextNodeInPath(paths,currentPosition,nextNode);
                }
                if (nextNode != null) {
                    ACLMessage nodeMsg = new ACLMessage(ACLMessage.PROPOSE);
                    if (!timesVisited.containsKey(currentPosition)) {
            			timesVisited.put(currentPosition, 0);
            		}
                    
                    timesVisited.put(currentPosition, timesVisited.get(currentPosition)+1);
                    nodeMsg.setOntology("MoveTo");
                    try {
                        nodeMsg.setContentObject((Serializable) new Couple<String, String>(currentPosition, nextNode));
                        nodeMsg.addReceiver(environmentAgentAID);
                        myAgent.send(nodeMsg);
                        
                        String selectedNode = (String)beliefBase.getBelief("SelectedNode").getValue();
                        int attempts = (int)beliefBase.getBelief("Attempts").getValue();
                        if (selectedNode == null) {
                            beliefBase.updateBelief("SelectedNode", nextNode);
                        }
                        // WANTING TO ATTEMPT TO GO TO THE SAME NODE AS BEFORE
                        else if (selectedNode.equals(nextNode)) {
                            beliefBase.updateBelief("Attempts", attempts + 1);
                        }
                        else {
                            beliefBase.updateBelief("SelectedNode", nextNode);
                            beliefBase.updateBelief("Attempts", 0);
                        }
                        if (attempts >= 10) {
                            golemPositions.add(nextNode);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            // In plan B there is no need for treating no movement ticks: the agent always has nodes to visit
        }
        
        // Update agentPositions with the position of the environment agent
        HashMap<AID, Couple<Long, String>> agentPositions = (HashMap<AID, Couple<Long, String>>) beliefBase.getBelief("AgentPositions").getValue();
        agentPositions.put(environmentAgentAID, new Couple<Long, String>((Long)System.nanoTime(), currentPosition));
        
        // Update resources information with the current observation
        HashMap<String, Couple<Long, HashMap<Observation, Integer>>> resourcesInformation = (HashMap<String, Couple<Long, HashMap<Observation, Integer>>>)beliefBase.getBelief("ResourcesInformation").getValue();
        List<Couple<Observation, Integer>> currentContents = observation.get(0).getRight();
        if (currentContents.size() > 0) {
            HashMap<Observation, Integer> info = new HashMap<>();
            for (int i = 0; i < currentContents.size(); ++i) {
                info.put(currentContents.get(i).getLeft(), currentContents.get(i).getRight());
            }
            resourcesInformation.put(currentPosition, new Couple<Long, HashMap<Observation, Integer>>(System.nanoTime(), info));
        }
        
        // Topology does not need updates since it is already done in DFS
        
        sendAgentPositions(); sendResourcesInformation(); sendMapTopology();
        
    }
    
    
    @SuppressWarnings("unchecked")
	private void treatObservationCollector(List<Couple<String, List<Couple<Observation, Integer>>>> observation) {
        BeliefBase beliefBase = getCapability().getBeliefBase();
        
        MapRepresentation mapRepresentation = (MapRepresentation)beliefBase.getBelief("MapRepresentation").getValue();
        
        if (mapRepresentation == null) {
            beliefBase.updateBelief("MapRepresentation",new MapRepresentation());
            
        }
        
        // This will help to remove inaccurate information of positions occupied so an agent can avoid getting stuck
        HashMap<AID, String> occupiedPositions = ((HashMap<AID, String>) beliefBase.getBelief("OccupiedPositions").getValue());;
        
        int ticks = (int)beliefBase.getBelief("Ticks").getValue();
        if (ticks == 5) {
            ticks = 0;
            beliefBase.updateBelief("Ticks", 0);
            occupiedPositions.clear();
        }
        else beliefBase.updateBelief("Ticks", ticks+1);
        
        String treasureType = (String)beliefBase.getBelief("TreasureType").getValue();
        Integer expertiseValue = (Integer)beliefBase.getBelief("ExpertiseValue").getValue();
        Integer backpackFreeSpace = (Integer)beliefBase.getBelief("BackpackFreeSpace").getValue();
        HashMap<String, Set<AID>> foreignAgents = (HashMap<String,Set<AID>>)beliefBase.getBelief("ForeignAgents").getValue();    
        AID environmentAgentAID = (AID)beliefBase.getBelief("EnvironmentAgentAID").getValue();
        String currentPosition = observation.get(0).getLeft();
        
        HashMap<AID, Couple<Long, String>> agentPositions = (HashMap<AID, Couple<Long, String>>) beliefBase.getBelief("AgentPositions").getValue();
        agentPositions.put(environmentAgentAID, new Couple<Long, String>((Long)System.nanoTime(), currentPosition));
        
        HashMap<String,Set<String>> map = (HashMap<String,Set<String>>)beliefBase.getBelief("Map").getValue();
        
        HashMap<String, Couple<Long, HashMap<Observation, Integer>>> resourcesInformation = (HashMap<String, Couple<Long, HashMap<Observation, Integer>>>)beliefBase.getBelief("ResourcesInformation").getValue();
        
        List<Couple<Observation, Integer>> currentPosContents = observation.get(0).getRight();
        
        if (currentPosContents.size() > 1) {
            HashMap<Observation, Integer> info = new HashMap<>();
            for (int i = 0; i < currentPosContents.size(); ++i) {
                info.put(currentPosContents.get(i).getLeft(), currentPosContents.get(i).getRight());
            }
            resourcesInformation.put(currentPosition, new Couple<Long, HashMap<Observation, Integer>>(System.nanoTime(), info));
        }
        
        Boolean IsMyTreasureType = false;
        Boolean lockIsOpen = false;
        Boolean canLockPicking = false;
        String nodeTreasureType = "None";
        
        // check if treasures in my position
        for( Couple<Observation, Integer> elem : currentPosContents) {
            if(elem.getLeft().getName().equals(treasureType)) {
                if(elem.getRight() > 0)    IsMyTreasureType = true;
                nodeTreasureType = elem.getLeft().getName();
            }
            if(elem.getLeft().getName() == "LockIsOpen" )  {
                if(elem.getRight() == 1) lockIsOpen = true;
            }
            if(elem.getLeft().getName() == "LockPicking" ) {
                if(elem.getRight() <= expertiseValue) canLockPicking = true;
            }
        }
        
        // What do i do?
        if(IsMyTreasureType && lockIsOpen && backpackFreeSpace!=0) {
            ACLMessage nodeMsg = new ACLMessage(ACLMessage.PROPOSE);
            nodeMsg.setOntology("PickUp");
            nodeMsg.addReceiver(environmentAgentAID);
            myAgent.send(nodeMsg);
        }
        //else if(!lockIsOpen && canLockPicking) {
        else if(IsMyTreasureType && canLockPicking && backpackFreeSpace!=0) {
            ACLMessage nodeMsg = new ACLMessage(ACLMessage.PROPOSE);
            nodeMsg.setOntology("OpenLock");
            nodeMsg.setContent(nodeTreasureType);
            nodeMsg.addReceiver(environmentAgentAID);
            myAgent.send(nodeMsg);
        }
        else {
            String nodeTo = null;
            if(backpackFreeSpace == 0) {
                Set<AID> myTypeTankers = Collections.emptySet();
                switch(treasureType) {
                case "Gold":
                    myTypeTankers = foreignAgents.get("agentTankerGold");
                    break;
                case "Diamond":
                    myTypeTankers = foreignAgents.get("agentTankerDiamond");
                default:
                    break;
                }
                
                // Iterate through the tankers of our type we know about
                AID agentNodeObjective = null;
                String nodeObjective = null;
                String adjOfTanker = null;
                for(AID a : myTypeTankers) {
                    if(occupiedPositions.size() > 0) {
                        //System.out.println("Occup map:");
                        //occupiedPositions.forEach((key, value) -> System.out.println(key.getName() + "<-key | value->" + value));

                    }
                    if(occupiedPositions.containsKey(a)) {    // sabemos que estan ahi o hace nada lo estaban
                        nodeObjective = occupiedPositions.get(a);
                        HashMap<String,String> paths = new HashMap<>();
                        adjOfTanker = getShortestPathToTanker(map,currentPosition,nodeObjective,paths);
                        if(adjOfTanker != null) {
                            //System.out.println("HE ENCONTRADO UN TANKER");
                            nodeTo = nextNodeInPath(paths,currentPosition,adjOfTanker);
                            agentNodeObjective = a;
                            break;
                        }
                    }
                    else if(agentPositions.containsKey(a)) {    // la ultima vez estaban ahi
                        nodeObjective = agentPositions.get(a).getRight();
                        HashMap<String,String> paths = new HashMap<>();
                        adjOfTanker = getShortestPathToTanker(map,currentPosition,nodeObjective,paths);
                        if(adjOfTanker != null) {
                            //System.out.println("HE ENCONTRADO UN TANKER");
                            nodeTo = nextNodeInPath(paths,currentPosition,adjOfTanker);
                            agentNodeObjective = a;
                            break;
                        }
                    }
                }
                
                if(nodeTo == null) {
                    nodeTo = doDFS(observation);
                    if(nodeTo!=null && nodeTo.contentEquals(currentPosition)) nodeTo = null;    // avoid being stuck when all map explored
                }
                //System.out.println("MI ESPACIO VACIO AHORA MISMO ES :"+backpackFreeSpace);
                if(nodeTo != null) {
                    //System.out.println("adjOFTANKER: " +adjOfTanker+",nodeTo: "+nodeTo+", currentPosition: "+currentPosition);
                    if(adjOfTanker!=null && currentPosition.contentEquals(adjOfTanker)) {
                        //System.out.println("HE ENCONTRADO A UN TANKER AL LADO DE MI");
                        //System.out.println("Posicion actual " +currentPosition);
                        //System.out.println("posicion " +adjOfTanker);
                        ACLMessage depositMsg = new ACLMessage(ACLMessage.PROPOSE);
                        depositMsg.setOntology("DepositIn");
                        depositMsg.setContent(agentNodeObjective.getLocalName());
                        depositMsg.addReceiver(environmentAgentAID);
                        myAgent.send(depositMsg);
                        
                    }
                    else {
                        ACLMessage nodeMsg = new ACLMessage(ACLMessage.PROPOSE);
                        nodeMsg.setOntology("MoveToNode");
                        try {
                            nodeMsg.setContentObject((Serializable) new Couple<String, String>(currentPosition, nodeTo));
                            nodeMsg.addReceiver(environmentAgentAID);
                            myAgent.send(nodeMsg);
                            
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    ACLMessage nodeMsg = new ACLMessage(ACLMessage.PROPOSE);
                    nodeMsg.setOntology("MoveToRandomNode");
                    nodeMsg.addReceiver(environmentAgentAID);
                    myAgent.send(nodeMsg);
                }
            }
            else {
                for(String s : resourcesInformation.keySet()) {
                    if(treasureType.contentEquals("Gold") && resourcesInformation.get(s).getRight().containsKey(Observation.GOLD)) {
                        HashMap<String,String> paths = new HashMap<>();
                        if(getShortestPath(map,currentPosition,s,paths)) {
                            //System.out.println("GOLD I WANT: "+ s);
                            //System.out.println("positions: "+occupiedPositions);
                            nodeTo = nextNodeInPath(paths,currentPosition,s);
                            //System.out.println("RESOURCES: "+resourcesInformation);
                        }
                    }
                    else if(treasureType.contentEquals("Diamond") && resourcesInformation.get(s).getRight().containsKey(Observation.DIAMOND)) {
                        HashMap<String,String> paths = new HashMap<>();
                        if(getShortestPath(map,currentPosition,s,paths)) {
                            nodeTo = nextNodeInPath(paths,currentPosition,s);
                        }
                    }
                }
                
                if(nodeTo == null) {
                    nodeTo = doDFS(observation);
                    if(nodeTo!=null && nodeTo.equals(currentPosition)) nodeTo = null;
                }
                
                if(nodeTo != null) {
                    ACLMessage nodeMsg = new ACLMessage(ACLMessage.PROPOSE);
                    nodeMsg.setOntology("MoveToNode");
                    try {
                        nodeMsg.setContentObject((Serializable) new Couple<String, String>(currentPosition, nodeTo));
                        nodeMsg.addReceiver(environmentAgentAID);
                        myAgent.send(nodeMsg);
                        
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    ACLMessage nodeMsg = new ACLMessage(ACLMessage.PROPOSE);
                    nodeMsg.setOntology("MoveToRandomNode");
                    nodeMsg.addReceiver(environmentAgentAID);
                    myAgent.send(nodeMsg);
                }
            }
        }
        
        // Update agentPositions with the position of the environment agent
        agentPositions = (HashMap<AID, Couple<Long, String>>) beliefBase.getBelief("AgentPositions").getValue();
        agentPositions.put(environmentAgentAID, new Couple<Long, String>((Long)System.nanoTime(), currentPosition));
        
        // Update resources information with the current observation
        resourcesInformation = (HashMap<String, Couple<Long, HashMap<Observation, Integer>>>)beliefBase.getBelief("ResourcesInformation").getValue();
        List<Couple<Observation, Integer>> currentContents = observation.get(0).getRight();
        if (currentContents.size() > 0) {
            HashMap<Observation, Integer> info = new HashMap<>();
            for (int i = 0; i < currentContents.size(); ++i) {
                info.put(currentContents.get(i).getLeft(), currentContents.get(i).getRight());
            }
            resourcesInformation.put(currentPosition, new Couple<Long, HashMap<Observation, Integer>>(System.nanoTime(), info));
        }
        
        // Topology does not need updates since it is already done in DFS
        
        sendAgentPositions(); sendResourcesInformation(); sendMapTopology();
    }
    
    
    @SuppressWarnings("unchecked")
	private void treatObservationTanker(List<Couple<String, List<Couple<Observation, Integer>>>> observation) {
        
        BeliefBase beliefBase = getCapability().getBeliefBase();
        
        String currentPosition = observation.get(0).getLeft();
        
        MapRepresentation mapRepresentation = (MapRepresentation)beliefBase.getBelief("MapRepresentation").getValue();
        
        if (mapRepresentation == null) {
            beliefBase.updateBelief("MapRepresentation",new MapRepresentation());
            mapRepresentation = (MapRepresentation)beliefBase.getBelief("MapRepresentation").getValue();
            mapRepresentation.addNode(currentPosition, MapAttribute.closed);
        }
        else mapRepresentation.addNode(currentPosition, MapAttribute.closed);
        
        int ticks = (int)beliefBase.getBelief("Ticks").getValue();
        
        if (ticks == 5) {
            ticks = 0;
            beliefBase.updateBelief("Ticks", 0);
            HashMap<String, String> occupiedPositions = ((HashMap<String, String>) beliefBase.getBelief("OccupiedPositions").getValue());
            occupiedPositions.clear();
        }
        
        else beliefBase.updateBelief("Ticks", ticks+1);
        
        AID environmentAgentAID = (AID)beliefBase.getBelief("EnvironmentAgentAID").getValue();
        
        HashMap<AID, Couple<Long, String>> agentPositions = (HashMap<AID, Couple<Long, String>>) beliefBase.getBelief("AgentPositions").getValue();
        agentPositions.put(environmentAgentAID, new Couple<Long, String>((Long)System.nanoTime(), currentPosition));
        
        //a_salvo -> indica si el nodo es un nodo vacío y que tiene solo una adyacencia
        boolean a_salvo = true;
        
        List<Couple<Observation, Integer>> currentPosContents = observation.get(0).getRight();
        
        if (currentPosContents.isEmpty() && observation.size() == 2) { 
            a_salvo = true;
        }
        else a_salvo = false;

        String nextNode = null;
        
        Set<String> visitedNodes = (Set<String>)beliefBase.getBelief("VisitedNodes").getValue();
        
        HashMap<String,Set<String>> map = (HashMap<String,Set<String>>)beliefBase.getBelief("Map").getValue();
        
        HashMap<String, Couple<Long, HashMap<Observation, Integer>>> resourcesInformation = (HashMap<String, Couple<Long, HashMap<Observation, Integer>>>)beliefBase.getBelief("ResourcesInformation").getValue();
        
        HashMap<String, Set<AID>> foreignAgents = (HashMap<String, Set<AID>>) beliefBase.getBelief("ForeignAgents").getValue();
        
        boolean recolectores_cerca = false;
        
        HashMap<AID, HashSet<String>> caminos = new HashMap<>();
        
        HashSet<String> camino_mas_corto = new HashSet<>();
        
        for (HashMap.Entry<AID, Couple<Long, String>> entry : agentPositions.entrySet()) {
            if (foreignAgents.containsKey("agentCollect") && foreignAgents.get("agentCollect").contains(entry.getKey())) {
                HashMap<String,String> paths = new HashMap<>();
                camino_mas_corto.clear();
                if (getShortestPath(map,currentPosition,entry.getValue().getRight(),paths)) {
                    String node;
                    node = nextNodeInPath(paths,currentPosition,entry.getValue().getRight());
                    
                    camino_mas_corto.add(node);
                    
                    while (node != entry.getValue().getRight()) {
                        String aux = node;
                        node = nextNodeInPath(paths,aux,entry.getValue().getRight());
                        
                        camino_mas_corto.add(node);
                    }
                    if (camino_mas_corto.size() < 4) {
                        camino_mas_corto.clear();
                        for(String s : resourcesInformation.keySet()) {
                            if (getShortestPath(map,currentPosition,s,paths)) {
                                String node2;
                                node2 = nextNodeInPath(paths,currentPosition,s);
                                
                                camino_mas_corto.add(node2);
                                
                                while (node2 != s) {
                                    String aux2 = node2;
                                    node2 = nextNodeInPath(paths,aux2,s);
                                    
                                    camino_mas_corto.add(node2);
                                }
                                if (camino_mas_corto.size() < 4) {
                                    //System.out.println("COLLECTOR AND RESOURCES NEARBY");
                                    caminos.put(entry.getKey(), camino_mas_corto);
                                    recolectores_cerca = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (a_salvo == true) {
            nextNode = currentPosition;
        }

        else {
            //si hay algun recolector cerca y recursos cerca
            if (recolectores_cerca) {
                if (caminos.size() == 1) {
                     Map.Entry<AID,HashSet<String>> entry = caminos.entrySet().iterator().next();
                     HashSet<String> values = entry.getValue();
                     if (!values.contains(currentPosition)) {
                         nextNode = currentPosition;
                     }
                     else {
                        for (int i = 1; i < observation.size(); ++i) {
                            List<Couple<Observation, Integer>> aux = observation.get(i).getRight();
                            String pos = observation.get(i).getLeft();
                            for (int j = 0; j < aux.size(); ++j) {
                                Observation aux2 = aux.get(j).getLeft();
                                if (!aux2.equals(Observation.WIND) && !values.contains(pos)) {
                                    nextNode = pos;
                                }
                            }
                        }
                     }
                }
                else {
                    HashSet<String> all = new HashSet<>();
                    for (HashMap.Entry<AID, HashSet<String>> entry : caminos.entrySet()) {
                        all.addAll(entry.getValue());
                    }
                    if (!all.contains(currentPosition)) {
                         nextNode = currentPosition;
                    }
                    else {
                        for (int i = 1; i < observation.size(); ++i) {
                            List<Couple<Observation, Integer>> aux = observation.get(i).getRight();
                            String pos = observation.get(i).getLeft();
                            for (int j = 0; j < aux.size(); ++j) {
                                Observation aux2 = aux.get(j).getLeft();
                                if (!aux2.equals(Observation.WIND) && !all.contains(pos)) {
                                    nextNode = pos;
                                }
                            }
                        }
                     }
                }
            }
            else {
                for (HashMap.Entry<String, Set<String>> entry : map.entrySet()) {
                    if (!visitedNodes.contains(entry.getKey()) && entry.getValue().size() == 1 && !resourcesInformation.containsKey(entry.getKey())) {
                        nextNode = entry.getKey();
                        break;
                    }
                }
                if (nextNode == null) {
                    nextNode = doDFS(observation);
                }
                else {
                    HashMap<String,String> paths = new HashMap<>();
                    if(getShortestPath(map,currentPosition,nextNode,paths)) {
                        nextNode = nextNodeInPath(paths,currentPosition,nextNode);
                    }
                    else {
                        nextNode = doDFS(observation);
                    }
                }
            }
        }
        
        if (nextNode == null) {
            nextNode = currentPosition;
        }
        
        if (nextNode.contentEquals(currentPosition)) {
        	Couple<String, Long> cpl = new Couple<String,Long>(currentPosition, (Long)System.nanoTime());
            
            ACLMessage broadcast = ((ACLMessage) beliefBase.getBelief("BroadcastMessage").getValue()).shallowClone();
            broadcast.removeReceiver(environmentAgentAID);
            broadcast.setOntology("TankingPositionInform");
            try {
                ACLMessage msgWithMsg;
                broadcast.setContentObject((Serializable) cpl);
                broadcast.setSender(environmentAgentAID);
                msgWithMsg = new ACLMessage(ACLMessage.INFORM);
                msgWithMsg.setOntology("SendTankingPositionInform");
                msgWithMsg.addReceiver(environmentAgentAID);
                try {
                    msgWithMsg.setContentObject((Serializable)broadcast);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                myAgent.send(msgWithMsg);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        else {
	        visitedNodes.add(nextNode);
	        ACLMessage nodeMsg = new ACLMessage(ACLMessage.PROPOSE);
	        nodeMsg.setOntology("MoveTo");
	        try {
	            nodeMsg.setContentObject((Serializable) new Couple<String, String> (currentPosition, nextNode));
	            nodeMsg.addReceiver(environmentAgentAID);
	            myAgent.send(nodeMsg);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
        }
        
     // Update agentPositions with the position of the environment agent
        
        agentPositions = (HashMap<AID, Couple<Long, String>>) beliefBase.getBelief("AgentPositions").getValue();
        agentPositions.put(environmentAgentAID, new Couple<Long, String>((Long)System.nanoTime(), currentPosition));
        
        // Update resources information with the current observation
        resourcesInformation = (HashMap<String, Couple<Long, HashMap<Observation, Integer>>>)beliefBase.getBelief("ResourcesInformation").getValue();
        List<Couple<Observation, Integer>> currentContents = observation.get(0).getRight();
        if (currentContents.size() > 0) {
            HashMap<Observation, Integer> info = new HashMap<>();
            for (int i = 0; i < currentContents.size(); ++i) {
                info.put(currentContents.get(i).getLeft(), currentContents.get(i).getRight());
            }
            resourcesInformation.put(currentPosition, new Couple<Long, HashMap<Observation, Integer>>(System.nanoTime(), info));
        }
        
        // Topology does not need updates since it is already done in DFS
        
        sendAgentPositions(); sendResourcesInformation(); sendMapTopology();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void execute() {
        BeliefBase beliefBase = getCapability().getBeliefBase();
        String entityType = (String)beliefBase.getBelief("EntityType").getValue();
        AID environmentAgentAID = (AID)beliefBase.getBelief("EnvironmentAgentAID").getValue();
        MessageTemplate mt = MessageTemplate.MatchSender(environmentAgentAID);
        ACLMessage msg = myAgent.receive(mt);
        if (msg != null) {
            switch (msg.getOntology()) {
            case "Message":
                // Treatment of different messages
                ACLMessage containedMessage = null;
                try {
                    containedMessage = (ACLMessage) msg.getContentObject();
                    if (containedMessage.getOntology() != null) {
                        switch (containedMessage.getOntology()) {
                        case "FIPA-Agent-Management": // Foreign agents
                            treatDFNotification(containedMessage);
                            break;
                        case "agentPositions":
                            treatAgentPositions(containedMessage);
                            break;
                        case "resourceInformation":
                            treatResourcesInformation(containedMessage);
                            break;
                        case "mapTopology":
                            treatMapTopology(containedMessage);
                            break;
                        case "TankingPositionInform":
                            if (entityType.equals("AgentCollect")) {
                                Couple<String, Long> msgContent = null;
                                try {
                                    msgContent = (Couple<String,Long>)containedMessage.getContentObject();
                                } catch (UnreadableException e1) {
                                    e1.printStackTrace();
                                }
                                HashMap<AID, Couple<Long, String>> agentPositions = (HashMap<AID, Couple<Long, String>>) beliefBase.getBelief("AgentPositions").getValue();
                                Couple<Long,String> ap = new Couple<Long,String>(msgContent.getRight(), msgContent.getLeft());
                                agentPositions.put(containedMessage.getSender(),ap);
                            }
                            break;
                        default:
                            break;
                        }
                    }
                } catch (UnreadableException e1) {
                    e1.printStackTrace();
                }
                break;
            case "Observation":
                List<Couple<String, List<Couple<Observation, Integer>>>> observation;
                try {
                    observation = (List<Couple<String,List<Couple<Observation,Integer>>>>) msg.getContentObject();
                    switch (entityType) {
                    case "AgentExplo":
                        treatObservationExplorer(observation);
                        break;
                    case "AgentCollect":
                        treatObservationCollector(observation);
                        break;
                    case "AgentTanker":
                        treatObservationTanker(observation);
                        break;
                    default:
                        break;
                    }
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
                break;
            case "UpdateBackpackFreeSpace":
                beliefBase.updateBelief("BackpackFreeSpace", Integer.valueOf(msg.getContent()));
                break;
            default:
                break;
            }
        }
    }
}