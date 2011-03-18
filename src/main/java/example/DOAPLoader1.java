/*****************************************************************************
 * File:    DOAPLoader1.java
 * Project: linux-mag-article
 * Created: 18 Mar 2011
 * By:      ian
 *
 * Copyright (c) 2011 Epimorphics Ltd. All rights reserved.
 *****************************************************************************/

// Package
///////////////

package example;


// Imports
///////////////

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

import javax.xml.parsers.*;

/**
 * <p>TODO class comment</p>
 *
 * @author Ian Dickinson, Epimorphics (mailto:ian@epimorphics.com)
 */
public class DOAPLoader1
{
    /***********************************/
    /* Constants                       */
    /***********************************/

    /** Where we load the DOAP inded file from */
    public static final String DOAP_SOURCE = "http://svn.apache.org/repos/asf/infrastructure/site-tools/trunk/projects/files.xml";

    /***********************************/
    /* Static variables                */
    /***********************************/

    @SuppressWarnings( value = "unused" )
    private static final Logger log = LoggerFactory.getLogger( DOAPLoader1.class );

    /***********************************/
    /* Instance variables              */
    /***********************************/

    /***********************************/
    /* Constructors                    */
    /***********************************/

    /***********************************/
    /* External signature methods      */
    /***********************************/

    public static void main( String[] args ) {
        new DOAPLoader1().run();
    }

    public void run() {
        List<String> doapFiles = loadDoapFileIndex();
        Model doaps = getDoapData( doapFiles );

    }

    /***********************************/
    /* Internal implementation methods */
    /***********************************/

    /**
     * Return a list of the DOAP files that we can load from Apache
     */
    private List<String> loadDoapFileIndex() {
        List<String> files = new ArrayList<String>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse( DOAP_SOURCE );

            NodeList nl = document.getElementsByTagName( "location" );
            for (int i = 0; i < nl.getLength(); i++) {
                files.add( nl.item( i ).getTextContent() );
            }
        }
        catch (ParserConfigurationException e) {
            log.error( e.getMessage(), e );
        }
        catch (SAXException e) {
            log.error( e.getMessage(), e );
        }
        catch (IOException e) {
            log.error( e.getMessage(), e );
        }

        return files;
    }

    /**
     * Return an RDF model containing all of the DOAP data read from the given
     * DOAP source files
     * @param files List of DOAP source files
     * @return A new RDF memory model containing
     */
    private Model getDoapData( List<String> files ) {
        Model doapData = getDOAPModel();

        for (String rdf: files) {
            try {
                FileManager.get().readModel( doapData, rdf );
            }
            catch (Exception e) {
                log.warn( "Failed to load file " + rdf + ", because: " + e.getMessage() );
            }
        }

        return doapData;
    }

    /**
     * Return a RDF model object for containing the DOAP data
     * @return A new, empty memory-model by default
     */
    protected Model getDOAPModel() {
        return ModelFactory.createDefaultModel();
    }

    /***********************************/
    /* Inner class definitions         */
    /***********************************/

}

