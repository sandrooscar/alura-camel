package br.com.caelum.camel;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Locale;

import javax.sql.DataSource;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.xstream.XStreamDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;

import com.thoughtworks.xstream.XStream;

import oracle.jdbc.pool.OracleDataSource;

/**
 * Desafio Parte 1: HTTP Polling
 * @author Sandro
 * https://camel.apache.org/components/latest/timer-component.html
 *
 */
public class TimerCamel {

	public static void main(String[] args) throws Exception {
		Locale.setDefault(new Locale("pt", "pt_BR"));
		final XStream xstream = new XStream();
		//mapeamento n� negociacao para clalsse negociacao
		xstream.alias("negociacao", Negociacao.class);
		
		//configurando o bd oracle
		SimpleRegistry registro = new SimpleRegistry();
		registro.put("banco", oracleAqDataSource());
		CamelContext context = new DefaultCamelContext(registro);//construtor recebe registro
		
		context.addRoutes(new RouteBuilder() {

			@Override
			public void configure() throws Exception { 
				//configura��o do timer
				from("timer://negociacoes?fixedRate=true&delay=1s&period=360s").
				//endpoint que ser� acessado pelo timer
					to("http4://argentumws-spring.herokuapp.com/negociacoes").
					//necess�rio para n�o perder o corpo da mensagem
					convertBodyTo(String.class).
					unmarshal(new XStreamDataFormat(xstream)).  //O m�todo unmarshal usar� o XStream e todas as negocia��es ser�o adicionadas em uma java.util.List. Teremos como resultados um lista de negocia��es (List<Negociacao>).
					split(body()). //cada negocia��o se torna uma mensagem
					log("${body}").
					process(
							new Processor() {
						        @Override
						        public void process(Exchange exchange) throws Exception {
						            Negociacao negociacao = exchange.getIn().getBody(Negociacao.class);
						            Long id = System.currentTimeMillis();
						            exchange.setProperty("id", id);
						            exchange.setProperty("preco", negociacao.getPreco());
						            exchange.setProperty("quantidade", negociacao.getQuantidade());
//									n�o consegui inserir a data e hora no banco localhost do oracle XE							
//						            String data = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(negociacao.getData().getTime());
//						            exchange.setProperty("data", new Timestamp(id));
						        }								
							}
							).
				    setBody(simple("insert into negociacao(id, preco, quantidade, data) values (${property.id}, ${property.preco}, ${property.quantidade}, '${property.data}')")).
				    log("${body}"). //logando o comando esql
				    delay(1000). //esperando 1s para deixar a execu��o mais f�cil de entender
				    to("jdbc:banco"). //usando o componente jdbc que envia o SQL para mysql
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
	
    private static DataSource oracleAqDataSource() throws SQLException {
        OracleDataSource oracleDataSource = new OracleDataSource();
        oracleDataSource.setUser("camel");
        oracleDataSource.setPassword("camel");
//        oracleDataSource.setURL("jdbc:oracle:thin:@localhost:1521:camel");
        oracleDataSource.setURL("jdbc:oracle:thin:@localhost");
        oracleDataSource.setImplicitCachingEnabled(true);
        oracleDataSource.setFastConnectionFailoverEnabled(true);
        oracleDataSource.setPortNumber(1521);
        oracleDataSource.setServiceName("xe");
        return oracleDataSource;
    }	
}
