package com.bc.canal.client;

import java.net.InetSocketAddress;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.Message;
import com.bc.canal.client.cons.Constants;
import com.bc.canal.client.parser.DataParser;
import com.bc.canal.client.utils.ConfigUtils;

/**
 * 入口类
 *
 * @author zhou
 */
public class CanalClient {
    private static final Logger logger = Logger.getLogger(CanalClient.class);
    static String canalServerHost;
    static String canalServerPort;
    static String canalServerInstance;
    static String canalBatchSize;
    static String canalSleep;
    /**
     * 是否打印rowData
     * y:打印  n:不打印  默认打印
     */
    public static boolean canalPrint;
    public static String canalBinlogFilename;
    public static String canalBinlogDir;

    public static String canalMq;

    static int canalServerPortInt;
    static int canalBatchSizeInt;
    static int canalSleepInt;
    static String canalFilterRegex;


    /**
     * rabbitmq
     */
    public static String rabbitmqHost;
    public static String rabbitmqPort;
    public static String rabbitmqUser;
    public static String rabbitmqPass;
    public static String rabbitmqQueuename;
    public static String rabbitmqExchangeType;
    public static String rabbitmqExchangeName;
    public static String rabbitmqRoutingKey;

    public static int rabbitmqPortInt;

    /**
     * redis
     */
    public static String redisHost;
    public static String redisPort;
    public static String redisQueuename;
    public static String redisPassword;

    public static int redisPortInt;

    /**
     * kafka
     */
    public static String kafkaBootstrapServers;
    public static String kafkaTopic;


    public static void main(String[] args) {
        init();
        logger.info("[canal.server] host: " + canalServerHost +
                ", port: " + canalServerPort + ", instance: " + canalServerInstance);

        CanalConnector connector = CanalConnectors.newSingleConnector(new InetSocketAddress(canalServerHost, canalServerPortInt),
                canalServerInstance, "", "");
        try {
            connectAndGetMessage(connector);
        } catch (Exception e) {
            logger.error("connect error! msg: " + e.getMessage());
        } finally {
            connector.disconnect();
        }

    }

    private static void connectAndGetMessage(CanalConnector connector) throws Exception {
        connector.connect();

        /**
         * canal白名单配置
         * mysql 数据解析关注的表
         * 常见例子：
         * 1. 所有表：.* or .*\\..*
         * 2. canal schema下所有表： canal\\..*
         * 3. canal下的以canal打头的表：canal\\.canal.*
         * 4. canal schema下的一张表：canal.test1
         * 5. 多个规则组合使用：canal\\..*,mysql.test1,mysql.test2 (逗号分隔)
         */
        if (!StringUtils.isEmpty(canalFilterRegex)) {
            connector.subscribe(canalFilterRegex);
        }

        connector.rollback();

        logger.info("connect success! \r\n startup...");
        while (true) {
            Message message = connector.getWithoutAck(canalBatchSizeInt);
            long batchId = message.getId();
            int size = message.getEntries().size();
            if (-1 == batchId || 0 == size) {
                Thread.sleep(canalSleepInt);
            } else {
                DataParser.parse(message.getEntries());
            }
            connector.ack(batchId);
        }
    }

    /**
     * 初始化配置项
     */
    private static void init() {
        canalServerHost = ConfigUtils.getProperty(Constants.CANAL_SERVER_HOST_KEY,
                Constants.DEFAULT_CANAL_SERVER_HOST);

        canalServerPort = ConfigUtils.getProperty(Constants.CANAL_SERVER_PORT_KEY,
                Constants.DEFAULT_CANAL_SERVER_PORT);
        try {
            canalServerPortInt = Integer.valueOf(canalServerPort);
        } catch (Exception e) {
            logger.warn("Fail to load canal.server.port, it's not Integer, check canal.server.port value:" + canalServerPort);
            canalServerPortInt = Integer.valueOf(Constants.DEFAULT_CANAL_SERVER_PORT);
        }

        canalServerInstance = ConfigUtils.getProperty(Constants.CANAL_SERVER_INSTANCE_KEY,
                Constants.DEFAULT_CANAL_SERVER_INSTANCE);


        canalBatchSize = ConfigUtils.getProperty(Constants.CANAL_BATCHSIZE_KEY,
                Constants.DEFAULT_CANAL_BATCHSIZE);
        try {
            canalBatchSizeInt = Integer.valueOf(canalBatchSize);
        } catch (Exception e) {
            logger.warn("Fail to load canal.batchsize, it's not Integer, check canal.batchsize value:" + canalBatchSize);
            canalBatchSizeInt = Integer.valueOf(Constants.DEFAULT_CANAL_BATCHSIZE);
        }

        canalSleep = ConfigUtils.getProperty(Constants.CANAL_SLEEP_KEY,
                Constants.DEFAULT_CANAL_SLEEP);
        try {
            canalSleepInt = Integer.valueOf(canalSleep);
        } catch (Exception e) {
            logger.warn("Fail to load canal.sleep, it's not Integer, check canal.sleep value:" + canalSleep);
            canalSleepInt = Integer.valueOf(Constants.DEFAULT_CANAL_SLEEP);
        }

        canalPrint = ConfigUtils.getBooleanProperty(Constants.CANAL_PRINT_KEY,
                true);
        canalBinlogFilename = ConfigUtils.getProperty(Constants.CANAL_BINLOG_FILENAME_KEY,
                Constants.DEFAULT_CANAL_BINLOG_FILENAME);
        canalBinlogDir = ConfigUtils.getProperty(Constants.CANAL_BINLOG_DIR_KEY,
                Constants.DEFAULT_CANAL_BINLOG_DIR);

        canalMq = ConfigUtils.getProperty(Constants.CANAL_MQ_KEY,
                Constants.DEFAULT_CANAL_MQ);

        canalFilterRegex = ConfigUtils.getProperty(Constants.CANAL_FILTER_REGEX_KEY,
                Constants.DEFAULT_CANAL_FILTER_REGEX);

        //rabbitmq
        rabbitmqHost = ConfigUtils.getProperty(Constants.RABBITMQ_HOST_KEY,
                Constants.DEFAULT_RABBITMQ_HOST);
        rabbitmqPort = ConfigUtils.getProperty(Constants.RABBITMQ_PORT_KEY,
                Constants.DEFAULT_RABBITMQ_PORT);
        try {
            rabbitmqPortInt = Integer.valueOf(rabbitmqPort);
        } catch (Exception e) {
            logger.warn("Fail to load rabbitmq.port, it's not Integer, check rabbitmq.port value:" + rabbitmqPort);
            rabbitmqPortInt = Integer.valueOf(Constants.DEFAULT_RABBITMQ_PORT);
        }
        rabbitmqUser = ConfigUtils.getProperty(Constants.RABBITMQ_USER_KEY,
                Constants.DEFAULT_RABBITMQ_USER);
        rabbitmqPass = ConfigUtils.getProperty(Constants.RABBITMQ_PASS_KEY,
                Constants.DEFAULT_RABBITMQ_PASS);
        rabbitmqQueuename = ConfigUtils.getProperty(Constants.RABBITMQ_QUEUENAME_KEY,
                Constants.DEFAULT_RABBITMQ_QUEUENAME);
        // rabbitmq交换机类型(direct/topic/fanout)
        rabbitmqExchangeType = ConfigUtils.getProperty(Constants.RABBITMQ_EXCHANGE_TYPE_KEY,
                Constants.DEFAULT_RABBITMQ_EXCHANGE_TYPE);
        // 交换器名
        rabbitmqExchangeName = ConfigUtils.getProperty(Constants.RABBITMQ_EXCHANGE_NAME_KEY,
                Constants.DEFAULT_RABBITMQ_EXCHANGE_NAME);
        // routingKey
        rabbitmqRoutingKey = ConfigUtils.getProperty(Constants.RABBITMQ_ROUTINGKEY_KEY,
                Constants.DEFAULT_RABBITMQ_ROUTING_KEY);

        //redis
        redisHost = ConfigUtils.getProperty(Constants.REDIS_HOST_KEY,
                Constants.DEFAULT_REDIS_HOST);
        redisPort = ConfigUtils.getProperty(Constants.REDIS_PORT_KEY,
                Constants.DEFAULT_RABBITMQ_PORT);
        try {
            redisPortInt = Integer.valueOf(redisPort);
        } catch (Exception e) {
            logger.warn("Fail to load redis.port, it's not Integer, check redis.port value:" + redisPort);
            redisPortInt = Integer.valueOf(Constants.DEFAULT_REDIS_PORT);
        }
        redisQueuename = ConfigUtils.getProperty(Constants.REDIS_QUEUENAME_KEY,
                Constants.DEFAULT_REDIS_QUEUENAME);

        redisPassword = ConfigUtils.getProperty(Constants.REDIS_PASSWORD_KEY,
                Constants.DEFAULT_REDIS_PASSWORD);

        //kafka
        kafkaBootstrapServers = ConfigUtils.getProperty(Constants.KAFKA_BOOTSTRAP_SERVERS_KEYS,
                Constants.DEFAULT_KAFKA_BOOTSTRAP_SERVERS);
        kafkaTopic = ConfigUtils.getProperty(Constants.KAFKA_TOPIC_KEYS,
                Constants.DEFAULT_KAFKA_TOPIC);
    }
}
