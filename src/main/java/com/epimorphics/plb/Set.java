/*****************************************************************************
 * File:    Set.java
 * Project: linux-mag-article
 * Created: 21 Mar 2011
 * By:      ian
 *
 * Copyright (c) 2011 Epimorphics Ltd. All rights reserved.
 *****************************************************************************/

// Package
///////////////

package com.epimorphics.plb;


// Imports
///////////////

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.plb.vocabs.DOAP;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * <p>Set elements of the project description</p>
 *
 * @author Ian Dickinson, Epimorphics (mailto:ian@epimorphics.com)
 */
public class Set
    extends PLB
{
    /***********************************/
    /* Constants                       */
    /***********************************/

    /***********************************/
    /* Static variables                */
    /***********************************/

    private static final Logger log = LoggerFactory.getLogger( Set.class );

    public static Options options;
    static {
        options = new Options();
        PLB.addCommonOptions( options );
        options.addOption( "r", "replace", false, "Replace any existing value for the given property" );
    }

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
        Set set = new Set();
        set.setCommandLine( options, args );
        try {
            set.run();
        }
        catch (RuntimeException e) {
            log.error( e.getMessage(), e );
            System.exit( 1 );
        }
    }

    /**
     * Perform the action of setting a value on the project, checking that arguments
     * are understood first
     */
    @Override
    public void run() {
        if (checkArguments()) {
            String propName = getArgs()[0];
            String propValue = getArgs()[1];

            Model model = getTDBModel();
            Resource project = getProjectResource();
            Property p = model.getProperty( DOAP.getURI() + propName );
            RDFNode obj = propValue.startsWith( "http:" ) ? model.getResource( propValue ) : model.createLiteral( propValue );

            checkKnownProperty( p, propName );
            removeOld( project, p );
            addNewValue( project, p, obj );
        }
    }

    /***********************************/
    /* Internal implementation methods */
    /***********************************/

    /** Return true if arguments are OK */
    private boolean checkArguments() {
        if (getArgs().length == 0) {
            usage();
        }
        else if (getArgs()[0].equals( "help" )) {
            listAvailableOptions();
            return false;
        }
        else if (getArgList().size() < 2) {
            usage();
        }
        return true;
    }

    private void usage() {
        new HelpFormatter().printHelp( "plb set [options] variable value", options );
        System.exit( 1 );
    }

    /** Return true if the given property is unknown, by which we mean is not defined
     * in the DOAP schema
     * @param p A property
     * @return True if the given property is not defined by DOAP
     */
    protected boolean unknownProperty( Property p ) {
        Model doapModel = DOAP.Project.getModel();
        Property dp = p.inModel( doapModel );
        return ! dp.hasProperty( RDF.type, RDF.Property );
    }

    /**
     * Print a list of all of the DOAP properties that can apply to doap:Projects, together with
     * their English labels
     */
    protected void listAvailableOptions() {
        System.out.println( "Available options\n-----------------" );

        Model doapModel = DOAP.Project.getModel();

        for (ResIterator i = doapModel.listSubjectsWithProperty( RDF.type, RDF.Property ); i.hasNext(); ) {
            Resource p = i.next();
            if (p.hasProperty( RDFS.domain, DOAP.Project )) {
                // we need to find the English label
                for (StmtIterator j = p.listProperties( RDFS.comment ); j.hasNext(); ) {
                    Literal label = j.next().getLiteral();
                    if (label.getLanguage().equals( "en" )) {
                        System.out.println( p.getLocalName() + "::  " + label.getLexicalForm() );
                    }
                }
            }
        }
    }

    /**
     * Check that the given property is known in the DOAP schema; throw an exception if not
     * @param p
     * @param propName
     */
    protected void checkKnownProperty( Property p, String propName ) {
        if (unknownProperty( p )) {
            System.err.println( String.format( "Sorry, property %s was not recognized.\nTry 'plb set help' to see available options", propName ) );
            throw new RuntimeException( "Invalid property " + propName );
        }
    }

    /**
     * Check if the new value replaces the old, and remove any old values if so
     * @param project The root resource for the DOAP project description
     * @param p
     */
    protected void removeOld( Resource project, Property p ) {
        if (hasOption( "r" )) {
            project.removeAll( p );
        }
    }

    /**
     * Add the new value for the given property
     * @param project
     * @param p
     * @param value
     */
    protected void addNewValue( Resource project, Property p, RDFNode value ) {
        project.addProperty( p, value );
    }

    /***********************************/
    /* Inner class definitions         */
    /***********************************/

}

