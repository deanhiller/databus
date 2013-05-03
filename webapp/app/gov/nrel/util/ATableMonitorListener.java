package gov.nrel.util;

import java.math.BigInteger;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.playorm.cron.api.CronListener;
import org.playorm.cron.api.PlayOrmCronJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;

import com.alvazan.orm.api.base.NoSqlEntityManager;
import com.alvazan.orm.api.base.NoSqlEntityManagerFactory;
import com.alvazan.orm.api.z3api.NoSqlTypedSession;
import com.alvazan.orm.api.z3api.QueryResult;
import com.alvazan.orm.api.z8spi.KeyValue;
import com.alvazan.orm.api.z8spi.iter.Cursor;
import com.alvazan.orm.api.z8spi.meta.TypedRow;

import controllers.TableMonitor;

public class ATableMonitorListener {

	private static final Logger log = LoggerFactory
			.getLogger(ATableMonitorListener.class);

	public void monitorFired(NoSqlEntityManager mgr, PlayOrmCronJob mon) {
		TableMonitor tableMon = TableMonitor.copy(mon);
		if (log.isInfoEnabled())
			log.info("monitor firing=" + tableMon.getId());

		String tableName = tableMon.getTableName();
		//NOTE: d.time>0 is here because PlayOrm does not yet have order by asc(time) which would choose the index
		//so instead PlayOrm randomly chooses an index
		String sql = "select d from " + tableName + " as d where d.time>0";
		NoSqlTypedSession s = mgr.getTypedSession();
		QueryResult result = s.createQueryCursor(sql, 1); // only need last
															// point so batch
															// size of 1

		Cursor<KeyValue<TypedRow>> cursor = result.getPrimaryViewCursor();
		TypedRow r = getLastVal(cursor);
		DateTime now = new DateTime();
		if (r == null) {
			fireEmailStreamIsDown(null, now, tableMon);
			return;
		}

		BigInteger timestamp = (BigInteger) r.getRowKey();

		int periodInMinutes = (int) tableMon.getUpdatePeriod();

		// find begin period and add 30 seconds as well in case last data point
		// was a little early.
		DateTime beginPeriod = now.minusMinutes(periodInMinutes).minusSeconds(30);

		DateTime lastDataPoint = new DateTime(timestamp.longValue());
		String msg = "lastpoint=("+timestamp.longValue()+")" + lastDataPoint + " beginPeriod=" + beginPeriod
				+ " now=" + now + " m=" + tableMon.getId();
		if (lastDataPoint.isAfter(beginPeriod)) {
			if (log.isInfoEnabled())
				log.info("Stream is good.  "+msg);
			return; // nothing to do
		}
		
		if (log.isInfoEnabled())
			log.info("Stream is down.  "+msg);
		fireEmailStreamIsDown(lastDataPoint, now, tableMon);
	}

	private void fireEmailStreamIsDown(DateTime lastDataPoint, DateTime now,
			TableMonitor tableMon) {
		if (log.isInfoEnabled())
			log.info("FIRE email since last datapoint is too far in the past. m="
				+ tableMon.getId());

		List<String> emails = tableMon.getEmailsAsList();
		// Get system properties
		Properties properties = System.getProperties();

		String host = Play.configuration.getProperty("email.host");
		String from = Play.configuration.getProperty("email.fromaddress");
		
		// Setup mail server
		if (StringUtils.isBlank(host)) {
			if (log.isWarnEnabled())
				log.warn("Error sending email!  Play configuration 'email.host' is blank, cannot send email.");
			return;
		}
		properties.setProperty("mail.smtp.host", host);

		// Get the default Session object.
		Session session = Session.getDefaultInstance(properties);

		try {
			// Create a default MimeMessage object.
			MimeMessage message = new MimeMessage(session);

			// Set From: header field of the header.
			message.setFrom(new InternetAddress(from));

			for(String email : emails) {
				// Set To: header field of the header.
				message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
			}

			// Set Subject: header field
			message.setSubject("Databus is not receiving data to table="+tableMon.getTableName()+" of schema="+tableMon.getSchema());

			// Now set the actual message
			message.setText("Your client has appeared to stop working.  The last time we received data was="
					+lastDataPoint+"(if null, we never received anything) and right now it is="+now+"  The table that we are monitoring" +
							" is table="+tableMon.getTableName()+" which exists in schema="+tableMon.getSchema());

			// Send message
			Transport.send(message);
			if (log.isInfoEnabled())
				log.info("send mail successfully for monitor="+tableMon.getId());
		} catch (MessagingException mex) {
			if (log.isWarnEnabled())
        		log.warn("Exception sending email", mex);
		}
	}

	private TypedRow getLastVal(Cursor<KeyValue<TypedRow>> cursor) {
		cursor.afterLast();
		if (!cursor.previous())
			return null;

		KeyValue<TypedRow> kv = cursor.getCurrent();
		TypedRow value = kv.getValue();
		if (value == null)
			return null;

		return value;
	}

}
