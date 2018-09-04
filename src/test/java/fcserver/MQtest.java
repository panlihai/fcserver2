package fcserver;

import cn.pcorp.model.DynaBean;
import cn.pcorp.mq.RabbitMqConsumer;

/**
 * 类描述
 * @author luyu
 * @date 2018年5月4日 下午3:37:10
 * @version 1.0
 */
public class MQtest {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		
		String pid = "SYSTEM";
		DynaBean mqBean = new DynaBean();
		mqBean.set("QUEUENAME", pid+"queue");
		mqBean.set("HOSTNAME", "localhost");
		mqBean.set("USERNAME", "admin");
		mqBean.set("PASSWORD", "admin");
		mqBean.set("PORT", 5672);
		mqBean.set("VIRTUALHOST", "/");
		
		mqBean.set("ROUTINGKEY", pid);
		mqBean.set("EXCHANGENAME", "exchange_"+pid+"_direct");
		mqBean.set("EXCHANGETYPE", "direct");// direct、fanout、topic
		
		// 发布消息
//		RabbitMqProducer producer = new RabbitMqProducer(mqBean);
//		int i = 0;
//		while (i < 10) {
//			HashMap<String, Object> hm = new HashMap<>();
//			hm.put("tagId", i);
//			producer.sendMSGByExchange(hm);
//			System.out.println("发送第" + i + "消息");
//			i++;
//		}
//		producer.close();
				
		// 消费消息
		RabbitMqConsumer consumer = new RabbitMqConsumer(mqBean);
        Thread cuThread = new Thread(consumer);
        cuThread.start();
		
		
	}

}