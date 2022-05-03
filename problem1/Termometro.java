package p1;

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.DFService;
import jade.domain.FIPAException;


import java.util.Random;

public class Termometro extends Agent {
    /**
     * m: valor medio del rango de temperatura
     * r: valores limites del rango de temperatura
     * p: probabilidad en la que la temperatura pasa a tener un valor fuera del rango
     * s: tiempo en el que se actualiza la temperatura
     */
    private int m, r, p, s;
    private float temperatura;
    private boolean ignorado;
    
    MessageTemplate tp1 = MessageTemplate.MatchOntology("P1"); //Hay que poner la ontologia del mensaje que envia el termostato com "P1"
        
    public class ActualizarTemperatura extends TickerBehaviour {
        public ActualizarTemperatura(Agent a, long period) {
            super(a, period);
        }

        public void onTick() {
            Random random = new Random();
            int randomProbability = random.nextInt(101);
            if (randomProbability <= p) { // Caso valor fuera del rango, [m-3*r, m+3*r]
                float min = m - 3 * r;
                float max = m + 3 * r;
                temperatura = min + random.nextFloat() * (max - min);
            }
            else {
                float min = m - r;
                float max = m + r;
                temperatura = min + random.nextFloat() * (max - min);
            }
            System.out.println("Temperatura actualizada: " + temperatura);
        }
    }
    
    private class EsperarMensajeTermostato extends CyclicBehaviour {

		public EsperarMensajeTermostato(Agent a) {
			super(a);
		}

		public void action() {
			ACLMessage  msg = myAgent.receive(tp1);
			if(msg != null){
				if (msg.getPerformative() == ACLMessage.REQUEST){
					String content = msg.getContent();
                    System.out.println("Mensaje recibido");
					if ((content != null) && (content.equals("TEMPERATURA"))){
                        if (ignorado) {
                            ignorado = false;
                        }
						else {
                            ACLMessage reply = msg.createReply();
                            reply.setOntology("P1");
						    reply.setPerformative(ACLMessage.INFORM);
						    reply.setContent(Float.toString(temperatura));
                            send(reply);
                            System.out.println("Temperatura enviada");
                        }
					}
                    else if ((content != null) && (content.equals("IGNORAR"))) {
                        ignorado = true;
                    }
				}
			}
			else {
				block();
			}
		}
	}

    protected void setup() {
        Object[] args = getArguments();
        if (args.length != 4) {
            System.out.println("[ERROR] El agente no ha sido creado. Usage: 'A:p1.Termometro(m, r, p, s)'");
            doDelete();
        }
        else {
            m = Integer.parseInt((String)args[0]);
            r = Integer.parseInt((String)args[1]);
            p = Integer.parseInt((String)args[2]);
            s = Integer.parseInt((String)args[3]);
            ignorado = false;
            System.out.println(m + " " + r + " " + p + " " + s);

            ActualizarTemperatura b = new ActualizarTemperatura(this, s * 1000);
            
            ParallelBehaviour term = new ParallelBehaviour();
            term.addSubBehaviour(b);
            // Registration with the DF 
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();   
            sd.setType("Termometro"); 
            sd.setName(getName());
            sd.setOwnership("TILAB");
            dfd.setName(getAID());
            dfd.addServices(sd);
		
            try {
                DFService.register(this,dfd);
                EsperarMensajeTermostato Temperatura = new  EsperarMensajeTermostato(this);
                term.addSubBehaviour(Temperatura);
            } catch (FIPAException e) {
                System.out.println("El agente " +getLocalName()+ " no se ha podido registrar en el DF");
                doDelete();
            }
            
            this.addBehaviour(term);
            
        }
    }
}
