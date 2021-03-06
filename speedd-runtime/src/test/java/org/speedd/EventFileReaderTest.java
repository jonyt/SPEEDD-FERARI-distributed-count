package org.speedd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.producer.ProducerConfig;
import kafka.server.KafkaServer;
import kafka.utils.TestUtils;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.speedd.test.TestUtil;

import scala.actors.threadpool.AtomicInteger;

import com.netflix.curator.test.TestingServer;

public class EventFileReaderTest {
	private int brokerId = 0;
	private String topic = "test";
	Logger logger = LoggerFactory
			.getLogger(EventFileReaderTest.class.getName());

	private static class TestConsumer implements Runnable {
		private KafkaStream<byte[], byte[]> stream;
		private int threadNumber;
		private Logger logger = LoggerFactory.getLogger(getClass().getName());
		private AtomicInteger count = new AtomicInteger();

		public TestConsumer(KafkaStream<byte[], byte[]> aStream, int aThreadNumber) {
			threadNumber = aThreadNumber;
			stream = aStream;
		}

		public void run() {
			ConsumerIterator<byte[], byte[]> it = stream.iterator();
			
			while (it.hasNext()) {
				this.logger.info("Thread " + threadNumber + ": "
						+ new String(it.next().message()));
				this.logger.info("Count: " + count.incrementAndGet());
			}
		}

		public boolean isPassed() {
			return count.intValue() == 10;
		}
	}

	@Test
	public void producerTest() throws Exception {
		int zookeeperPort = TestUtils.choosePort();
		
		String zkConnect = "localhost:" + zookeeperPort; 
		
		TestingServer zkServer = TestUtil.startEmbeddedZkServer(zookeeperPort);
		Thread.sleep(5000);
		
		zkConnect = zkServer.getConnectString();
		
		// setup Broker
		int kafkaBrokerPort = TestUtils.choosePort();

		KafkaServer kafkaServer = TestUtil.setupKafkaServer(brokerId, kafkaBrokerPort, zkServer, new String[]{"test"});

		// setup producer
		Properties producerProperties = TestUtils.getProducerConfig(
				"localhost:" + kafkaBrokerPort, "kafka.producer.DefaultPartitioner");

		ProducerConfig pConfig = new ProducerConfig(producerProperties);

		EventFileReader eventFileReader = new EventFileReader(this.getClass().getClassLoader().getResource("test-events.csv").getPath(), topic, pConfig);

		eventFileReader.streamEvents(1000);

		Properties consumerProperties = TestUtils.createConsumerProperties(zkConnect, "group1", "consumer1", -1);
		ConsumerConfig consumerConfig = new ConsumerConfig(consumerProperties);

		ConsumerConnector consumer = Consumer
				.createJavaConsumerConnector(consumerConfig);
		
		Map<String, Integer> topicMap = new HashMap<String, Integer>();
		topicMap.put(topic, 1);
		Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer
				.createMessageStreams(topicMap);

		List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(topic);
		assertEquals("Supposed to be a single stream", 1, streams.size());

		KafkaStream<byte[], byte[]> stream = streams.get(0);

		ExecutorService executor = Executors.newSingleThreadExecutor();
		TestConsumer c = new TestConsumer(stream, 1);
		
		executor.submit(c);

		executor.awaitTermination(5, TimeUnit.SECONDS);

		executor.shutdown();

		assertTrue("Must pass", c.isPassed());

		consumer.shutdown();
		kafkaServer.shutdown();
		zkServer.stop();
		zkServer.close();
	}
}
