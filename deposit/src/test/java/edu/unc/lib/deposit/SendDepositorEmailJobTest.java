package edu.unc.lib.deposit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import net.greghaines.jesque.Job;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.deposit.work.SpringJobFactory;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class SendDepositorEmailJobTest {
	private static final Logger LOG = LoggerFactory.getLogger(SendDepositorEmailJobTest.class);
	
	// FIXME make sure the job can also run on failed deposits

	@Autowired
	File depositsDirectory;
	
	@Mock
	private DepositStatusFactory depositStatusFactory;
	
	@Mock
	private JavaMailSender mockSender;
	
	@Autowired
	private JavaMailSender xxmailSender;
	
	
	@Before
	public void setup() {
	    // Initialize mocks created above
	    MockitoAnnotations.initMocks(this);
	    
	    Mockito.doAnswer(new Answer<MimeMessage>() {
			@Override
			public MimeMessage answer(InvocationOnMock invocation)
					throws Throwable {
				return xxmailSender.createMimeMessage();
			}}).when(mockSender).createMimeMessage();
	    
	    Mockito.doAnswer(new Answer() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				MimeMessage msg = (MimeMessage)invocation.getArguments()[0];
				try(InputStream is = msg.getInputStream()) {
					IOUtils.copy(is, System.out);
				}
				return null;
			}}).when(mockSender).send(any(MimeMessage.class));
	}
	
	@Autowired
	SpringJobFactory springJobFactory = null;
	
	@Test
	public void testDepositSuccessful() throws ClassNotFoundException, MessagingException {
		String depositUUID = "bd5ff703-9c2e-466b-b4cc-15bbfd03c8ae";
		DepositTestUtils.makeTestDir(
				depositsDirectory,
				depositUUID, new File("src/test/resources/depositFileZipped.zip"));
		Job job = new Job("SendDepositorEmailJob", UUID.randomUUID().toString(), depositUUID);
		Object j = springJobFactory.materializeJob(job);
		SendDepositorEmailJob aj = (SendDepositorEmailJob)j;
		aj.setMailSender(mockSender);
		
		HashMap<String, String> status = new HashMap<String, String>();
		status.put(DepositField.depositMd5.name(), "c949138500f67e8617ac9968d2632d4e");
		status.put(DepositField.uuid.name(), depositUUID);
		status.put(DepositField.fileName.name(), "cdrMETS.zip");
		status.put(DepositField.depositorEmail.name(), "joe.depositor@example.com");
		status.put(DepositField.depositorName.name(), "Joe Depositor");
		status.put(DepositField.ingestedObjects.name(), "75");
		status.put(DepositField.excludeDepositRecord.name(), "true");
		Mockito.when(depositStatusFactory.get(anyString())).thenReturn(status);
		aj.setDepositStatusFactory(depositStatusFactory);
		
		Runnable r = (Runnable)j;
		r.run();
		
	}
	
	@Test
	public void testDepositFailure() throws ClassNotFoundException {
		String depositUUID = "bd5ff703-9c2e-466b-b4cc-15bbfd03c8ae";
		DepositTestUtils.makeTestDir(
				depositsDirectory,
				depositUUID, new File("src/test/resources/depositFileZipped.zip"));
		Job job = new Job("SendDepositorEmailJob", UUID.randomUUID().toString(), depositUUID);
		Object j = springJobFactory.materializeJob(job);
		SendDepositorEmailJob aj = (SendDepositorEmailJob)j;
		aj.setMailSender(mockSender);
		
		HashMap<String, String> status = new HashMap<String, String>();
		status.put(DepositField.depositMd5.name(), "c949138500f67e8617ac9968d2632d4e");
		status.put(DepositField.uuid.name(), depositUUID);
		status.put(DepositField.fileName.name(), "cdrMETS.zip");
		status.put(DepositField.depositorEmail.name(), "joe.depositor@example.com");
		status.put(DepositField.depositorName.name(), "Joe Depositor");
		status.put(DepositField.ingestedObjects.name(), "75");
		status.put(DepositField.errorMessage.name(), "MODS Validation failed");
		Mockito.when(depositStatusFactory.get(anyString())).thenReturn(status);
		aj.setDepositStatusFactory(depositStatusFactory);
		
		Runnable r = (Runnable)j;
		r.run();
	}
}
