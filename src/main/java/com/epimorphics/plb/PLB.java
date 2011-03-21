/*****************************************************************************
 * File:    PLB.java
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

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Shared implementation code for PLB commands</p>
 *
 * @author Ian Dickinson, Epimorphics (mailto:ian@epimorphics.com)
 */
public abstract class PLB
    implements Runnable
{
    /***********************************/
    /* Constants                       */
    /***********************************/

    /** Default location for project log book */
    public static final String DEFAULT_LOGBOOK_LOCATION = ".plb/tdb";

    /** Default namespace for projects */
    public static final String DEFAULT_PROJECT_NAMESPACE = "http://www.epimorphics.com/tutorial/plb#";

    /***********************************/
    /* Static variables                */
    /***********************************/

    @SuppressWarnings( value = "unused" )
    private static final Logger log = LoggerFactory.getLogger( PLB.class );

    protected static Options common_options;
    static {
        common_options = new Options();
        common_options.addOption( "s", "short-description", true, "A short description of the project" );
        common_options.addOption( "d", "long-description", true, "A full description of the project" );
        common_options.addOption( "n", "--namespace", true, "The HTTP namespace for the project" );
    }

    /***********************************/
    /* Instance variables              */
    /***********************************/

    /** Parsed command line */
    private CommandLine commandLine;

    /** Location of the TDB data store */
    private String tdbLocation = DEFAULT_LOGBOOK_LOCATION;

    /***********************************/
    /* Constructors                    */
    /***********************************/

    /***********************************/
    /* External signature methods      */
    /***********************************/

    /** Extend the given options by adding the common options from this class */
    protected static void addCommonOptions( Options options ) {
        for (Iterator<?> i = common_options.getOptions().iterator(); i.hasNext(); ) {
            options.addOption( (Option) i.next() );
        }
    }

    public void setCommandLine( CommandLine commandLine ) {
        this.commandLine = commandLine;
    }

    public CommandLine getCommandLine() {
        return commandLine;
    }

    public void setTdbLocation( String tdbLocation ) {
        this.tdbLocation = tdbLocation;
    }

    public String getTdbLocation() {
        return tdbLocation;
    }

    public List<?> getArgList() {
        return commandLine.getArgList();
    }

    public boolean hasOption( String opt ) {
        return getCommandLine().hasOption( opt );
    }

    public File getTDBFile() {
        return new File( getTdbLocation() );
    }

    public String[] getArgs() {
        return getCommandLine().getArgs();
    }

    public String getOptionValue( String opt ) {
        return getCommandLine().getOptionValue( opt );
    }

    /***********************************/
    /* Internal implementation methods */
    /***********************************/

    /***********************************/
    /* Inner class definitions         */
    /***********************************/

}

