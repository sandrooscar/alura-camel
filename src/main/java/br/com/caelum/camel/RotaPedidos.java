package br.com.caelum.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

public class RotaPedidos {

	public static void main(String[] args) throws Exception {

		CamelContext context = new DefaultCamelContext();
		
		context.addRoutes(new RouteBuilder() {

			@Override
			public void configure() throws Exception {
				//monitora a cada 5 segundos, noop=true-> mantêm os arquivos na pasta original
				from("file:pedidos?delay=5s&noop=true").
				setProperty("pedidoId", xpath("/pedido/id/text()")).
				setProperty("clienteId", xpath("/pedido/pagamento/email-titular/text()")).
				
				split().
					xpath("/pedido/itens/item").
				filter().
					xpath("/item/formato[text()='EBOOK']").
					setProperty("ebookId", xpath("/item/livro/codigo/text()")).
					//imprime o id gerado pelo camel, body-> corpo do arquivo
					//log("${id} ${body}").
				marshal().
					xmljson().
				log("${body}").
					setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http4.HttpMethods.GET)).
					setHeader(Exchange.HTTP_QUERY, constant("clienteId=${property.clienteId}&pedidoId=${property.pedidoId}&ebookId=${property.ebookId}")).
				to("http4://localhost:8080/webservices/ebook/item");
			}
			
		});
		
		context.start();
		Thread.sleep(20000);
		context.stop();
	}	
}
