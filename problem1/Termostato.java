//package org.upc.edu.Behaviours;
package p1;

import java.util.*;

import jade.core.*;
import jade.core.behaviours.*;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.util.Logger;


public class Termostato extends Agent
{

  private DFAgentDescription[] termometrosAID; //Termometros AIDs
  private Map<AID,Float> temperaturas = new HashMap<AID,Float>();
  private int mensajes_esperados = 0;
  private int n_termometros = 0;
  private int n_ignorados = 0;
  private int a, b;
  private boolean first_tick = false;
  private float ultima_temperatura = -999999;
  MessageTemplate tp1 = MessageTemplate.MatchOntology("P1");

  public class SearchAndAskTermometrosBehaviour extends TickerBehaviour
  {
    SearchAndAskTermometrosBehaviour(Agent a, long period) {
      super(a,period);
    }

    public void onTick()
    {
		System.out.println("tick!");
      //ENCONTRAR TERMOMETROS
      DFAgentDescription template = new DFAgentDescription();
      ServiceDescription templateSd = new ServiceDescription();

      templateSd.setType("Termometro");
      template.addServices(templateSd);

      SearchConstraints sc = new SearchConstraints();

      // We want to receive 10 results at most
      sc.setMaxResults(new Long(20));
      try
      {
        termometrosAID = DFService.search(myAgent, template, sc);
        n_termometros = termometrosAID.length;
		System.out.println("Termometros encontrados: " + n_termometros);
      }
      catch(Exception e)
      {

      }

      //ENVIA SOLICITUD A TODOS LOS TERMOMETROS
      for(int i = 0; i < n_termometros; ++i) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(termometrosAID[i].getName());
        msg.setContent("TEMPERATURA");
		msg.setOntology("P1");
		System.out.println("Mensaje enviado");
        send(msg);
      }
      mensajes_esperados = n_termometros - n_ignorados;
      n_ignorados = 0;
      first_tick = true;
    }
  }

	private class WaitAndTreatmentBehaviour extends CyclicBehaviour {

        public WaitAndTreatmentBehaviour(Agent a) {
            super(a);
        }

        public void action() {
          System.out.println("Cyclic");
        	//WAIT TEMPERATURAS (guardar temperatura y quien la ha enviado)
        	if(temperaturas.size() < mensajes_esperados) {
            System.out.println("Llego aqui1");
        		ACLMessage  msg = myAgent.receive(tp1);
            	if(msg != null) {
            		ACLMessage reply = msg.createReply();
              		if(msg.getPerformative() == ACLMessage.INFORM){
                		String content = msg.getContent();
                		if (content != null){
                    	float temp = Float.valueOf(content);
                    	temperaturas.put(msg.getSender(), temp);
                		}
              		}
              		else block();
            	}
        	}
          	//TREATMENT
          	else {
            	float suma =  Float.valueOf(0);
              System.out.println("Llego aqui2");
            	for (Map.Entry<AID, Float> entry : temperaturas.entrySet()) {
                System.out.println("for" + temperaturas.size());
      					//Si la temperatura es mayor que 2*b o menor que 2*a quitamos del vector temperaturas
      					if (entry.getValue() > b+2*Math.abs(b) || entry.getValue() < a-2*Math.abs(a)) {
      						ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
      						msg.addReceiver(entry.getKey());
      						msg.setContent("IGNORAR");
      						send(msg);
      						++n_ignorados;
      					}
      					else {
      						suma += entry.getValue();
      						if (entry.getValue() <= a || entry.getValue() >= b) {
      							DFAgentDescription template = new DFAgentDescription();
      							ServiceDescription templateSd = new ServiceDescription();
      							templateSd.setType("alarm-management");
      							template.addServices(templateSd);
      							SearchConstraints sc = new SearchConstraints();
      							sc.setMaxResults(new Long(10));
      							try {
      								DFAgentDescription[] results = DFService.search(myAgent, template, sc);
      								for (int i = 0; i < results.length; ++i) {
      									ACLMessage msg1 = new ACLMessage(ACLMessage.INFORM);
      									msg1.addReceiver(results[i].getName());
      									msg1.setContent("ALARM");
      									send(msg1);
      								}
      							}
  							    catch(Exception e) {}
  						    }
					  }
        }
        if(n_ignorados == mensajes_esperados && first_tick) {
          if(ultima_temperatura == -999999) {
            System.out.println("TERMOSTATO: Todas las temperaturas incorrectas. No tenemos suficiente informacion");
          }
          else {
            System.out.println("TERMOSTATO: Todas las temperaturas incorrectas. Ultima temperatura correcta: "+ultima_temperatura);
          }
        }
        else {
  				float media = suma/(mensajes_esperados-n_ignorados);
  				System.out.println("TERMOSTATO: La suma es: " + suma);
  				System.out.println("TERMOSTATO: La temperatura actual es: " + media);
  				temperaturas = new HashMap<AID,Float>();
          ultima_temperatura = media;
  				block();
        }
    		}
    	}
	}

	protected void setup() {
    	Object[] args = getArguments();
    	if(args.length != 2) {
    		System.out.println("Numero de argumentos incorrecto. Usage: 'A:p1.Termostato(a, b)'");
    		doDelete();
    	}
    	else {
    		a = Integer.parseInt((String)args[0]);
    		b = Integer.parseInt((String)args[1]);
			System.out.println("?");
			ParallelBehaviour pb = new ParallelBehaviour();
			pb.addSubBehaviour(new WaitAndTreatmentBehaviour(this));
			pb.addSubBehaviour(new SearchAndAskTermometrosBehaviour(this, 5000));
			this.addBehaviour(pb);
    	}
	}
}
