/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.fedora;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.InputStream;

import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * @author bbpennel
 * @date Mar 18, 2014
 */
public class JobForwardingJMSListenerTest {

    @Mock
    private TextMessage msg;
    @Mock
    private ListenerJob job1;
    @Mock
    private ListenerJob job2;

    private JobForwardingJMSListener listener;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        listener = new JobForwardingJMSListener();

        InputStream inStream = this.getClass().getResourceAsStream("/ingestMessage.xml");
        String messageBody = IOUtils.toString(inStream);

        when(msg.getText()).thenReturn(messageBody);

    }

    @Test
    public void testOnMessageNoListeners() throws Exception {

        listener.onMessage(msg);

        verify(msg, never()).getText();

    }

    @Test
    public void testOnMessage() throws Exception {

        listener.registerListener(job1);
        listener.registerListener(job2);

        listener.onMessage(msg);

        verify(msg).getText();

        verify(job1).onEvent(any(Document.class));
        verify(job2).onEvent(any(Document.class));

    }

    @Test
    public void testOnMessageUnregister() throws Exception {

        listener.registerListener(job1);
        listener.unregisterListener(job1);

        listener.onMessage(msg);

        verify(msg, never()).getText();

        verify(job1, never()).onEvent(any(Document.class));

    }

    @Test
    public void testOnMessageUnregisterMultipleMessages() throws Exception {

        listener.registerListener(job1);
        listener.registerListener(job2);

        listener.onMessage(msg);

        listener.unregisterListener(job1);

        listener.onMessage(msg);

        verify(msg, times(2)).getText();

        verify(job1).onEvent(any(Document.class));
        verify(job2, times(2)).onEvent(any(Document.class));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testOnMessageNotText() throws Exception {

        listener.registerListener(job1);

        Message badMessage = mock(Message.class);

        try {
            listener.onMessage(badMessage);
        } finally {
            verify(job1, never()).onEvent(any(Document.class));
        }

    }
}
