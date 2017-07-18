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

import static edu.unc.lib.dl.fedora.AuthorizationException.AuthorizationErrorType.INDETERMINATE;
import static edu.unc.lib.dl.fedora.AuthorizationException.AuthorizationErrorType.NOT_APPLICABLE;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.WebServiceFaultException;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import edu.unc.lib.dl.fedora.types.ArrayOfString;
import edu.unc.lib.dl.fedora.types.DescribeRepository;
import edu.unc.lib.dl.fedora.types.DescribeRepositoryResponse;
import edu.unc.lib.dl.fedora.types.FieldSearchQuery;
import edu.unc.lib.dl.fedora.types.FindObjects;
import edu.unc.lib.dl.fedora.types.FindObjectsResponse;
import edu.unc.lib.dl.fedora.types.GetDatastreamDissemination;
import edu.unc.lib.dl.fedora.types.GetDatastreamDisseminationResponse;
import edu.unc.lib.dl.fedora.types.GetObjectProfile;
import edu.unc.lib.dl.fedora.types.GetObjectProfileResponse;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.fedora.types.ObjectFields;
import edu.unc.lib.dl.fedora.types.ObjectProfile;

/**
 * The AccessClient is a Fedora 3.0 API-A SOAP client bean. After the fedoraUrl,
 * username and password properties are set, call the init() method before using
 * it.
 * <p>
 * For Fedora 3.0 API documentation, please consult the
 * <a href="https://fedora-commons.org/confluence/display/FCR30/API-A">wiki</a>.
 * </p>
 *
 * @author count0
 *
 */
public class AccessClient extends WebServiceTemplate {
    /**
     * These are the actions support by the Fedora API-A service.
     *
     * @author count0
     *
     */
    private enum Action {
        describeRepository("describeRepository"), findObjects("findObjects"), getDatastreamDissemination(
                "getDatastreamDissemination"), getObjectProfile("getObjectProfile");
        String uri = null;

        Action(String action) {
            uri = "http://www.fedora.info/definitions/1/0/api/#" + action;
        }

        WebServiceMessageCallback callback() {
            return new WebServiceMessageCallback() {
                @Override
                public void doWithMessage(WebServiceMessage message) {
                    ((SoapMessage) message).setSoapAction(uri);
                }
            };
        }
    }

    private static final Log log = LogFactory.getLog(AccessClient.class);
    private String fedoraContextUrl;
    private String password;
    private String username;

    /**
     * Encapsulates all marshalled web service calls.
     *
     * @param request
     *            a marshalled request object
     * @param action
     *            the Fedora API-A action
     * @return an unmarshalled response object
     */
    private Object callService(Object request, Action action) throws FedoraException {
        return callService(request, action, true);
    }

    private Object callService(Object request, Action action, boolean retry) throws FedoraException {
        Object response = null;
        try {
            response = this.marshalSendAndReceive(request, action.callback());
        } catch (WebServiceIOException e) {
            if (e.getMessage().contains("503")) {
                throw new FedoraTimeoutException(e);
            } else if (java.net.SocketTimeoutException.class.isInstance(e.getCause())) {
                throw new FedoraTimeoutException(e);
            } else {
                throw new ServiceException(e);
            }
        } catch (SoapFaultClientException e) {
            try {
                FedoraFaultMessageResolver.resolveFault(e);
            } catch (AuthorizationException ae) {
                if (retry && (NOT_APPLICABLE.equals(ae.getType()) || INDETERMINATE.equals(ae.getType()))) {
                    log.warn("Authorization failed, attempting to reestablish connection to Fedora.");
                    try {
                        this.init();
                    } catch (Exception e1) {
                        log.error("Failed to reestablish connection to Fedora", e);
                        throw ae;
                    }
                    return callService(request, action, false);
                }

                throw ae;
            }
        } catch (WebServiceFaultException e) {
            throw new ServiceException(e);
        }
        return response;
    }

    public DescribeRepositoryResponse describeRepository(DescribeRepository o)
            throws ServiceException, FedoraException {
        DescribeRepositoryResponse response = (DescribeRepositoryResponse) this.callService(o,
                Action.describeRepository);
        return response;
    }

    public List<PID> findAllObjectPIDs() throws FedoraException, ServiceException {
        List<PID> result = new ArrayList<PID>();
        FindObjects fo = new FindObjects();
        fo.setMaxResults(BigInteger.valueOf(100000));
        FieldSearchQuery query = new FieldSearchQuery();
        fo.setQuery(query);
        ArrayOfString fields = new ArrayOfString();
        fields.getItem().add("pid");
        fo.setResultFields(fields);
        FindObjectsResponse response = (FindObjectsResponse) this.callService(fo, Action.findObjects);
        for (ObjectFields o : response.getResult().getResultList().getObjectFields()) {
            String s = o.getPid().getValue();
            result.add(new PID(s));
        }
        return result;
    }

    public MIMETypedStream getDatastreamDissemination(PID pid, String dsid, String timestamp)
            throws FedoraException, ServiceException {
        GetDatastreamDissemination req = new GetDatastreamDissemination();
        req.setPid(pid.getPid());
        // TODO: test that NotFoundException is throw for non-existant
        // datastreams
        req.setDsID(dsid);
        if (timestamp != null) {
            req.setAsOfDateTime(timestamp);
        }
        GetDatastreamDisseminationResponse response = (GetDatastreamDisseminationResponse) this.callService(req,
                Action.getDatastreamDissemination);
        return response.getDissemination();
    }

    public ObjectProfile getObjectProfile(PID pid, String timestamp) throws FedoraException, ServiceException {
        GetObjectProfile req = new GetObjectProfile();
        req.setPid(pid.getPid());
        if (timestamp != null) {
            req.setAsOfDateTime(timestamp);
        }
        GetObjectProfileResponse response = (GetObjectProfileResponse) this.callService(req, Action.getObjectProfile);
        return response.getObjectProfile();
    }

    /**
     * Get the Fedora base URL.
     *
     * @return Fedora base URL
     */
    public String getFedoraContextUrl() {
        return fedoraContextUrl;
    }

    /**
     * Get the Fedora password.
     *
     * @return
     */
    public String getPassword() {
        return password;
    }

    /**
     * Get the Fedora username
     *
     * @return
     */
    public String getUsername() {
        return username;
    }

    /**
     * Initializes this client bean, calling the initializers of dependencies.
     *
     * @throws Exception
     *             when initialization fails
     */
    public void init() throws Exception {
        SaajSoapMessageFactory msgFactory = new SaajSoapMessageFactory();
        msgFactory.afterPropertiesSet();
        this.setMessageFactory(msgFactory);

        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("edu.unc.lib.dl.fedora.types");
        marshaller.afterPropertiesSet();
        this.setMarshaller(marshaller);
        this.setUnmarshaller(marshaller);

        HttpComponentsMessageSender sender = new HttpComponentsMessageSender();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(this.username, this.password);
        sender.setCredentials(credentials);
        sender.afterPropertiesSet();
        this.setMessageSender(sender);

        // this.setFaultMessageResolver(new FedoraFaultMessageResolver());
        this.setDefaultUri(this.getFedoraContextUrl() + "/services/access");
        this.afterPropertiesSet();
    }

    /**
     * Set the Fedora base URL.
     *
     * @param fedoraUrl
     */
    public void setFedoraContextUrl(String fedoraContextUrl) {
        this.fedoraContextUrl = fedoraContextUrl;
    }

    /**
     * Set the Fedora password.
     *
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Set the Fedora username
     *
     * @param username
     */
    public void setUsername(String username) {
        this.username = username;
    }
}
