package edu.unc.lib.boxc.fcrepo.utils;

import edu.unc.lib.boxc.fcrepo.exceptions.AuthorizationException;
import edu.unc.lib.boxc.fcrepo.exceptions.BadGatewayException;
import edu.unc.lib.boxc.fcrepo.exceptions.ConflictException;
import edu.unc.lib.boxc.fcrepo.exceptions.GoneException;
import edu.unc.lib.boxc.fcrepo.exceptions.RangeNotSatisfiableException;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import org.apache.http.HttpStatus;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author bbpennel
 */
public class ClientFaultResolverTest {

    @Test
    public void testResolveForbidden() {
        FcrepoOperationFailedException ex = mock(FcrepoOperationFailedException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.SC_FORBIDDEN);

        FedoraException result = ClientFaultResolver.resolve(ex);

        assertInstanceOf(AuthorizationException.class, result);
    }

    @Test
    public void testResolveNotFound() {
        FcrepoOperationFailedException ex = mock(FcrepoOperationFailedException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);

        FedoraException result = ClientFaultResolver.resolve(ex);

        assertInstanceOf(NotFoundException.class, result);
    }

    @Test
    public void testResolveGone() {
        FcrepoOperationFailedException ex = mock(FcrepoOperationFailedException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.SC_GONE);

        FedoraException result = ClientFaultResolver.resolve(ex);

        assertInstanceOf(GoneException.class, result);
    }

    @Test
    public void testResolveConflict() {
        FcrepoOperationFailedException ex = mock(FcrepoOperationFailedException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.SC_CONFLICT);

        FedoraException result = ClientFaultResolver.resolve(ex);

        assertInstanceOf(ConflictException.class, result);
    }

    @Test
    public void testResolveRangeNotSatisfiable() {
        FcrepoOperationFailedException ex = mock(FcrepoOperationFailedException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE);

        FedoraException result = ClientFaultResolver.resolve(ex);

        assertInstanceOf(RangeNotSatisfiableException.class, result);
    }

    @Test
    public void testResolveBadGateway() {
        FcrepoOperationFailedException ex = mock(FcrepoOperationFailedException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.SC_BAD_GATEWAY);

        FedoraException result = ClientFaultResolver.resolve(ex);

        assertInstanceOf(BadGatewayException.class, result);
    }

    @Test
    public void testResolveUnmappedStatusCode() {
        FcrepoOperationFailedException ex = mock(FcrepoOperationFailedException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);

        FedoraException result = ClientFaultResolver.resolve(ex);

        assertInstanceOf(FedoraException.class, result);
    }

    @Test
    public void testResolveNonFcrepoException() {
        Exception ex = new RuntimeException("Test exception");

        FedoraException result = ClientFaultResolver.resolve(ex);

        assertInstanceOf(FedoraException.class, result);
        assertSame(ex, result.getCause());
    }
}
