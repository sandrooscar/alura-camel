package br.com.caelum.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.xstream.XStreamDataFormat;
import org.apache.camel.impl.DefaultCamelContext;

import com.thoughtworks.xstream.XStream;

/**
 * Desafio Parte 1: HTTP Polling
 * @author Sandro
 *
 */
public class TimerCamel {

	public static void main(String[] args) throws Exception {

		final XStream xstream = new XStream();
		//mapeamento nó negociacao para clalsse negociaca
		xstream.alias("negociacao", Negociacao.class);
		
		CamelContext context = new DefaultCamelContext();
		
		context.addRoutes(new RouteBuilder() {

			@Override
			public void configure() throws Exception { 
				//configuração do timer
				from("timer://negociacoes?fixedRate=true&delay=1s&period=360s").
				//endpoint que será acessado pelo timer
					to("http4://argentumws-spring.herokuapp.com/negociacoes").
					//necessário para não perder o corpo da mensagem
					convertBodyTo(String.class).
					unmarshal(new XStreamDataFormat(xstream)).
					split(body()). //cada negociação se torna uma mensagem
					log("${body}").
					end();
				//saida arquivo xml
//					setHeader(Exchange.FILE_NAME, constant("negociacoes.xml")).
//				to("file://negociacoes");	
			}
		});
		context.start();
		Thread.sleep(20000);
		context.stop();
	}

}
