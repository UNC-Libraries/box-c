package edu.unc.lib.boxc.deposit.api.exceptions;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author Gregory Jansen
 * 
 */
public class METSParseException extends Exception implements ErrorHandler {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getMessage());
        sb.append("The METS did not meet all requirements.\n");
        for (SAXParseException e : this.fatalErrors) {
            sb.append(e.getMessage()).append("\n");
        }
        for (SAXParseException e : this.errors) {
            sb.append(e.getMessage()).append("\n");
        }
        for (SAXParseException e : this.warnings) {
            sb.append(e.getMessage()).append("\n");
        }
        return sb.toString();
    }

    private List<SAXParseException> warnings = new ArrayList<SAXParseException>();

    public List<SAXParseException> getWarnings() {
        return warnings;
    }

    public List<SAXParseException> getErrors() {
        return errors;
    }

    public List<SAXParseException> getFatalErrors() {
        return fatalErrors;
    }

    private List<SAXParseException> errors = new ArrayList<SAXParseException>();
    private List<SAXParseException> fatalErrors = new ArrayList<SAXParseException>();

    /**
     * @param msg
     */
    public METSParseException(String msg) {
        super(msg);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     */
    @Override
    public void error(SAXParseException exception) throws SAXException {
        this.errors.add(exception);
        throw exception;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        this.fatalErrors.add(exception);
        throw exception;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    @Override
    public void warning(SAXParseException exception) throws SAXException {
        this.warnings.add(exception);
        throw exception;
    }

}
