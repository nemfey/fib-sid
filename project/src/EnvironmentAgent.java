package practica;


import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.EntityType;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class EnvironmentAgent extends AbstractDedaleAgent{

private static final long serialVersionUID = -1784844593772918359L;
	
	private String EnvironmentServiceType = "EnvironmentAgentJTWW";
	
	private EntityType et = null;
	private Integer expertiseValue = 0;
	private Integer backpackFreeSpace = 0;
	
	private AID BDIAgentAID = null;

	private EntityType identifyEntityType() {
		if (this.getMyTreasureType() == Observation.NO_TREASURE) {
			return EntityType.AGENT_EXPLORER;
		}
		else {
			Set<Couple<Observation, Integer>> setObservations = this.getMyExpertise();
			for (Couple<Observation, Integer> expertise : setObservations) {
				if (expertise.getLeft() == Observation.LOCKPICKING) {
					if (expertise.getRight() > 0) return EntityType.AGENT_COLLECTOR;
					else return EntityType.AGENT_TANKER;
				}
			}
		}
		return null;
	}
	
	private void setCharacteristics() {
		et = identifyEntityType();
		
		// Check experitse value
		for(Couple<Observation,Integer> elem : getMyExpertise()) {
			if(elem.getLeft() == Observation.LOCKPICKING) {
				expertiseValue = elem.getRight();
			}
		}
		
		// Check initialFreeSpace
		for(Couple<Observation,Integer> elem : getBackPackFreeSpace()) {
			if(elem.getLeft() == getMyTreasureType()) {
				backpackFreeSpace = elem.getRight();
			}
		}
	}
	
	private void register() {
		DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();   
        dfd.setName(this.getAID());
        
        // ServiceDescription for communication with the BDI Agent
        sd.setType(EnvironmentServiceType); 
        sd.setName(this.getName());
        dfd.addServices(sd);
        // ServiceDescription for communication between agents of the same type
        sd = new ServiceDescription();
        sd.setName(this.getName());
        switch (et) {
        case AGENT_EXPLORER:
        	sd.setType("agentExplo");
        	break;
        case AGENT_COLLECTOR:
        	sd.setType("agentCollect");
        	break;
        case AGENT_TANKER:
        	sd.setType("agentTanker");
        	break;
        default:
        	break;
        }
        dfd.addServices(sd);
        
        switch (getMyTreasureType()) {
    	case GOLD:
    		sd = new ServiceDescription();
        	sd.setName(this.getName());
        	sd.setType("agentGold");
        	dfd.addServices(sd);
    		break;
    	case DIAMOND:
    		sd = new ServiceDescription();
        	sd.setName(this.getName());
        	sd.setType("agentDiamond");
        	dfd.addServices(sd);
    		break;
    	case ANY_TREASURE:
    		sd = new ServiceDescription();
        	sd.setName(this.getName());
        	sd.setType("agentGold");
        	dfd.addServices(sd);
        	dfd.addServices(sd);
        	
        	sd = new ServiceDescription();
        	sd.setName(this.getName());
        	sd.setType("agentDiamond");
        	dfd.addServices(sd);
    	default:
    		break;
    	} 
        dfd.addServices(sd);
        
        sd = new ServiceDescription();
        sd.setName(this.getName());
        sd.setType("environmentAgent");
        dfd.addServices(sd);
        try {
            DFService.register(this,dfd);
            System.out.println("Agent " + this.getLocalName() + " correctly registered");
        } catch (FIPAException e) {
            System.out.println("[ERROR] Agent " + this.getLocalName() + " could not be registered");
            this.doDelete();
        }
	}
	
	private void subscribeToDF() {
        /* Subscribing to DF */
        DFAgentDescription[] template = new DFAgentDescription[]{new DFAgentDescription(), new DFAgentDescription(), new DFAgentDescription(), new DFAgentDescription(), new DFAgentDescription()};
        ServiceDescription[] sd = new ServiceDescription[]{new ServiceDescription(), new ServiceDescription(), new ServiceDescription(), new ServiceDescription(), new ServiceDescription()};

        sd[0].setType("agentExplo");
        sd[1].setType("agentCollect");
        sd[2].setType("agentTanker");
        sd[3].setType("agentGold");
        sd[4].setType("agentDiamond");

        for (int i = 0; i < template.length; i++) {
            template[i].addServices(sd[i]);
            send(DFService.createSubscriptionMessage(this, getDefaultDF(), template[i], new SearchConstraints()));
        }

        System.out.println(getLocalName() + ": Subscribed to DF to get notifications about the registration of other teams agents");
    }
	
	protected void setup(){

		super.setup();
		
		Object[] args = super.getArguments();
		
		if (args.length == 3) {
			EnvironmentServiceType = (String)args[2];
		}

		setCharacteristics();
		
		register();
		
		subscribeToDF();
		
		List<Behaviour> lb=new ArrayList<Behaviour>();
		
		
		SequentialBehaviour sb = new SequentialBehaviour();
		sb.addSubBehaviour(new SynchroniseBehaviour());
		ParallelBehaviour pb = new ParallelBehaviour();
		pb.addSubBehaviour(new RedirectMessageBehaviour());
		switch (et) {
		case AGENT_EXPLORER:
			pb.addSubBehaviour(new ExplorerSensorActuatorBehaviour(this, 10));
			break;
		case AGENT_COLLECTOR:
			pb.addSubBehaviour(new CollectorSensorActuatorBehaviour(this, 10));
			break;
		case AGENT_TANKER:
			pb.addSubBehaviour(new TankerSensorActuatorBehaviour(this, 10));
			break;
		default:
			break;
		}
		
		sb.addSubBehaviour(pb);
		lb.add(sb);
		
		addBehaviour(new startMyBehaviours(this,lb));

	}
	
	@SuppressWarnings("serial")
	private class SynchroniseBehaviour extends SimpleBehaviour {
		private boolean finished = false;
		private int status = 0;
		private MessageTemplate mt = MessageTemplate.MatchOntology("SynchroniseJTWW");
		@Override
		public void action() {
			// En el status 0 el agente se guarda el AID del agente BDI
			if (status == 0) {
				ACLMessage msg = myAgent.receive(mt);
		        if (msg != null) {
	                BDIAgentAID = msg.getSender();
					status = 1;
		        }
			}
			// En el status 1 el agente responde lo que le vaya pidiendo el agente BDI
			else if (status == 1) {
				mt = MessageTemplate.MatchSender(BDIAgentAID);
				ACLMessage msg = myAgent.receive(mt);
		        if (msg != null) {
		        	ACLMessage reply = msg.createReply();
		        	reply.setPerformative(ACLMessage.INFORM);
		        	switch(msg.getOntology()) {
		        	
		        	// AQUI SE RESPONDEN LAS PETICIONES DEL AGENTE BDI
		        	case "EntityType":
		        		reply.setOntology("EntityType");
		        		reply.setContent(et.toString());
		        		myAgent.send(reply);
		        		break;
		        		
		        	case "TreasureType":
		        		reply.setOntology("TreasureType");
		        		reply.setContent(((AbstractDedaleAgent)myAgent).getMyTreasureType().getName());
		        		myAgent.send(reply);
		        		break;
		        		
		        	case "ExpertiseValue":
		        		reply.setOntology("ExpertiseValue");
		        		reply.setContent(Integer.toString(expertiseValue));
		        		myAgent.send(reply);
		        		break;
		        	
		        	case "BackpackMaxSpace":
		        		reply.setOntology("BackpackMaxSpace");
		        		// initial free space equals to max capacity of the backpack
		        		reply.setContent(Integer.toString(backpackFreeSpace));
		        		myAgent.send(reply);
		        		break;
		        	
		        	case "BackpackFreeSpace":
		        		reply.setOntology("BackpackFreeSpace");
		        		reply.setContent(Integer.toString(backpackFreeSpace));
		        		myAgent.send(reply);
		        		break;
		        		
		        	//////////////////////////////////////////////////////////////////////////////
		        		
		        	case "Synchronised":
		        		reply.setOntology("Synchronised");
		        		myAgent.send(reply);
		        		finished = true;
		        		System.out.println("Agent " + myAgent.getLocalName() + " has correctly syncronised with the BDI agent with AID " + BDIAgentAID);
		        		break;
		        	default:
		        		break;
		        	}
		        }
			}
		}

		@Override
		public boolean done() {
			return finished;
		}
	}
	
	@SuppressWarnings("serial")
	private class RedirectMessageBehaviour extends SimpleBehaviour {

		@Override
		public void action() {
			// The environment agent has no idea of what to do with other messages, so it sends them to the BDI agent
			MessageTemplate mt = MessageTemplate.not(MessageTemplate.MatchSender(BDIAgentAID));
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				ACLMessage msgWithMsg = new ACLMessage(ACLMessage.UNKNOWN);
				try {
					msgWithMsg.setContentObject((Serializable) msg);
				} catch (IOException e) {
					e.printStackTrace();
				}
				msgWithMsg.setOntology("Message");
				msgWithMsg.setSender(myAgent.getAID());
				msgWithMsg.addReceiver(BDIAgentAID);
				send(msgWithMsg);
			}
		}

		@Override
		public boolean done() {
			return false;
		}
	}
	
	@SuppressWarnings("serial")
	private class ExplorerSensorActuatorBehaviour extends TickerBehaviour {
		
		private String role = "Sensor";
		
		public ExplorerSensorActuatorBehaviour(Agent a, long period) {
			super(a, period);
		}

		

		@SuppressWarnings("unchecked")
		@Override
		protected void onTick() {
			String myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
            MessageTemplate mt;
            ACLMessage msg;
            
			switch (role) {
			case "Sensor":
				
				// Get the observations
				List<Couple<String,List<Couple<Observation,Integer>>>> lobs = null;
				if (myPosition != null) {
					lobs = ((AbstractDedaleAgent)this.myAgent).observe();
				}
				
				msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(BDIAgentAID);
				msg.setOntology("Observation");
				try {
					msg.setContentObject((Serializable) lobs);
				} catch (IOException e) {
					e.printStackTrace();
				}
				myAgent.send(msg);
				role = "Actuator";
	            break;
	            
			case "Actuator":
				
				mt = MessageTemplate.MatchSender(BDIAgentAID);
				msg = myAgent.blockingReceive(mt);
		        while (msg != null) {
		        	switch (msg.getOntology()) {
		        	case "MoveTo":
		        		Couple<String, String> movement;
						try {
							movement = (Couple<String, String>)msg.getContentObject();
							String currentNode = ((AbstractDedaleAgent)this.myAgent).observe().get(0).getLeft();
			        		if (currentNode.equals(movement.getLeft())&& movement.getRight() != null) {
			        		    //System.out.println(myAgent.getLocalName() + " will to move from " + currentNode + " to " + movement.getRight());
			        			((AbstractDedaleAgent)this.myAgent).moveTo(movement.getRight());
			        		}
			        		else {
			        			//System.out.println("ILLEGAL MOVEMENT AVOIDED");
			        		}
						} catch (UnreadableException e1) {
							e1.printStackTrace();
						}
		        		break;
		        	case "SendAgentPositions":
		        	case "SendResourcesInformation":
		        	case "SendMapTopology":
		        		try {
							ACLMessage containedMsg = (ACLMessage) msg.getContentObject();
							((AbstractDedaleAgent)myAgent).sendMessage(containedMsg);
						} catch (UnreadableException e) {
							e.printStackTrace();
						}
		        		break;
		        	default:
		        		break;
		        	}
		        	msg = myAgent.receive(mt);
		        }
		        
		        role = "Sensor";
		        break;
		        
			default:
				break;
			}
		}
	}
	
	@SuppressWarnings("serial")
	private class CollectorSensorActuatorBehaviour extends TickerBehaviour {
		
		public CollectorSensorActuatorBehaviour(Agent a, long period) {
			super(a, period);
		}
		
		private void updateBDIBackPackFreeSpace() {
			Integer backpackFreeSpace = 0;
			for(Couple<Observation,Integer> elem : getBackPackFreeSpace()) {
				if(elem.getLeft() == getMyTreasureType()) {
					backpackFreeSpace = elem.getRight();
				}
			}
			
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(BDIAgentAID);
			msg.setOntology("UpdateBackpackFreeSpace");
			msg.setContent(Integer.toString(backpackFreeSpace));
			myAgent.send(msg);
		}
		
		private String role = "Sensor";
		@SuppressWarnings("unchecked")
		@Override
		public void onTick() {
			String myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
            MessageTemplate mt;
            ACLMessage msg;
            
			switch (role) {
				case "Sensor":
					// Get the observations
					List<Couple<String,List<Couple<Observation,Integer>>>> lobs = null;
					if (myPosition != null) {
						lobs = ((AbstractDedaleAgent)this.myAgent).observe();
					}
					msg = new ACLMessage(ACLMessage.INFORM);
					msg.addReceiver(BDIAgentAID);
					msg.setOntology("Observation");
					try {
						msg.setContentObject((Serializable) lobs);
					} catch (IOException e) {
						e.printStackTrace();
					}
					myAgent.send(msg);
					role = "Actuator";
		            break;
		            
				case "Actuator":
					mt = MessageTemplate.MatchSender(BDIAgentAID);
					msg = myAgent.receive(mt);
					while (msg != null) {
						switch(msg.getOntology()) {
						case "MoveToNode":
							Couple<String, String> movement;
							try {
								movement = (Couple<String,String>)msg.getContentObject();
								String currentNode = ((AbstractDedaleAgent)this.myAgent).observe().get(0).getLeft();
				        		if (currentNode.equals(movement.getLeft()) && movement.getRight() != null) {
				        		    moveTo(movement.getRight());
				        		}
				        		else {
				        			//System.out.println("ILLEGAL MOVEMENT AVOIDED: "+currentNode+ "-> "+ movement.getRight());
				        		}
							} catch (UnreadableException e1) {
								e1.printStackTrace();
							}							
							break;
						
						case "MoveToTreasure":
							String nodeFrom = getCurrentPosition();
							String nodeTo = msg.getContent();
							
							MapRepresentation ma = new MapRepresentation();
							List<String> path = ma.getShortestPath(nodeFrom,nodeTo);
							moveTo(path.get(0));
							break;		
							
						case "SendAgentPositions":
			        	case "SendResourcesInformation":
			        	case "SendMapTopology":
			        		try {
								ACLMessage containedMsg = (ACLMessage) msg.getContentObject();
								((AbstractDedaleAgent)myAgent).sendMessage(containedMsg);
							} catch (UnreadableException e) {
								e.printStackTrace();
							}
			        		break;
							
						case "PickUp":		
							((AbstractDedaleAgent)this.myAgent).pick();
							
							updateBDIBackPackFreeSpace();
							break;
							
						case "OpenLock":
							String nodeTreasureType = msg.getContent();
							if(nodeTreasureType.equals("Gold")) openLock(Observation.GOLD);
							else if(nodeTreasureType.equals("Diamond")) openLock(Observation.DIAMOND);
							break;
							
						case "DepositIn":
							//System.out.println("VOY A DEPOSITAR");
							String silo = msg.getContent();
							emptyMyBackPack(silo);
							
							updateBDIBackPackFreeSpace();
							break;
							
						default:
							break;
						}
						msg = myAgent.receive(mt);
					}
					role = "Sensor";
					break;
				
				default:
					break;
			}
		}
	}
	
	@SuppressWarnings("serial")
	class TankerSensorActuatorBehaviour extends TickerBehaviour {
		
		private String role = "Sensor";
		
		public TankerSensorActuatorBehaviour(Agent a, long period) {
			super(a, period);
		}

		
		@SuppressWarnings("unchecked")
		@Override
		public void onTick() {
			
			String myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
            MessageTemplate mt;
            ACLMessage msg;
            
			switch (role) {
			case "Sensor":
				// Get the observations
				List<Couple<String,List<Couple<Observation,Integer>>>> lobs = null;
				if (myPosition != null) {
					lobs = ((AbstractDedaleAgent)this.myAgent).observe();
				}
				
				msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(BDIAgentAID);
				msg.setOntology("Observation");
				try {
					msg.setContentObject((Serializable) lobs);
				} catch (IOException e) {
					e.printStackTrace();
				}
				myAgent.send(msg);
				role = "Actuator";
				
	            break;
			
			case "Actuator":
				mt = MessageTemplate.MatchSender(BDIAgentAID);
				msg = myAgent.receive(mt);
				while (msg != null) {
					switch(msg.getOntology()) {
						case "MoveTo":
							Couple<String, String> movement;
							try {
								movement = (Couple<String, String>)msg.getContentObject();
								String currentNode = ((AbstractDedaleAgent)this.myAgent).observe().get(0).getLeft();
				        		if (currentNode.equals(movement.getLeft()) && movement.getRight() != null) {
				        		    //System.out.println(myAgent.getLocalName() + " will to move from " + currentNode + " to " + movement.getRight());
				        			((AbstractDedaleAgent)this.myAgent).moveTo(movement.getRight());
				        		}
				        		else {
				        			//System.out.println("ILLEGAL MOVEMENT AVOIDED");
				        		}
							} catch (UnreadableException e1) {
								e1.printStackTrace();
							}
							break;
						case "SendAgentPositions":
			        	case "SendResourcesInformation":
			        	case "SendMapTopology":
			        	case "SendTankingPositionInform":
			        		try {
								ACLMessage containedMsg = (ACLMessage) msg.getContentObject();
								((AbstractDedaleAgent)myAgent).sendMessage(containedMsg);
							} catch (UnreadableException e) {
								e.printStackTrace();
							}
			        		break;		
			        	default:
			        		break;
							
					}
					msg = myAgent.receive(mt);
				}
		        
		        role = "Sensor";
		        break;
			default:
				break;
			}
		}
	}
}
