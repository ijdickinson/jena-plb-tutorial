/*****************************************************************************
 * File:    Augment.java
 * Project: linux-mag-article
 * Created: 22 Mar 2011
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
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.plb.vocabs.DOAP;
import com.epimorphics.plb.vocabs.Sindice;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.sparql.function.library.sha1sum;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.util.ResourceUtils;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * <p>PLB action to augment the description automatically</p>
 * <pre>
 *  plb augment [-c git]
 * </pre>
 *
 * @author Ian Dickinson, Epimorphics (mailto:ian@epimorphics.com)
 */
public class Augment
    extends PLB
{
    /***********************************/
    /* Constants                       */
    /***********************************/

    /** Enumeration of the source-control systems the code knows how to work with */
    enum SourceControl  {git}

    /***********************************/
    /* Static variables                */
    /***********************************/

    private static final Logger log = LoggerFactory.getLogger( Augment.class );

    public static Options options;
    static {
        options = new Options();
        PLB.addCommonOptions( options );
        options.addOption( "c", "--source-control", true, "Denotes which source-code control system will be used (e.g. git)" );
    }

    /***********************************/
    /* Instance variables              */
    /***********************************/

    /** Selected SC system, default is git */
    private SourceControl sourceControl = SourceControl.git;

    /** We start by assuming the project directory is "." */
    private String projectDirectory = "./.git";

    /***********************************/
    /* Constructors                    */
    /***********************************/

    /***********************************/
    /* External signature methods      */
    /***********************************/

    public static void main( String[] args ) {
        Augment aug = new Augment();
        aug.setCommandLine( options, args );
        try {
            aug.run();
        }
        catch (RuntimeException e) {
            log.error( e.getMessage(), e );
            System.exit( 1 );
        }
    }

    @Override
    public void run() {
        if (checkArguments()) {
            Model devs = collectDevelopers();
            augmentProject( getProjectResource(), DOAP.developer, devs );
            getTDBModel().add( devs );
        }
    }

    /***********************************/
    /* Internal implementation methods */
    /***********************************/

    /** Return true if arguments are OK */
    private boolean checkArguments() {
        if (getArgs().length > 0 && getArgs()[0].equals( "help" )) {
            usage();
        }

        if (hasOption( "c" )) {
            boolean found = false;

            String sc = getOptionValue( "c" );
            for (SourceControl c: SourceControl.values()) {
                if (c.toString().equals( sc )) {
                    sourceControl = c;
                    found = true;
                }
            }

            if (!found) {
                System.out.println( "Unrecognised source control system: " + sc );
                usage();
            }
        }

        return true;
    }

    private void usage() {
        new HelpFormatter().printHelp( "plb augment [options]\n Known source-control systems: git", options );
        System.exit( 1 );
    }

    /** List developers of the current project, using the selected source control system */
    protected Model collectDevelopers() {
        Model m = ModelFactory.createDefaultModel();

        switch (sourceControl) {
            case git: return collectGitDevelopers( m );
        }

        return m;
    }

    /**
     * Collect all of the developers who have committed to this git repository, returning them
     * as a model of foaf:Person resources
     *
     * @param m
     * @return
     */
    protected Model collectGitDevelopers( Model m ) {
        try {
            Git git = new Git( getGitRepository() );

            for (RevCommit rc: git.log().call()) {
                String authorName = rc.getAuthorIdent().getName();
                String authorEmail = rc.getAuthorIdent().getEmailAddress();

                if (!m.contains( null, FOAF.name, authorName )) {
                    Resource author = m.createResource( FOAF.Person );
                    if (authorName != null) {author.addProperty( FOAF.name, authorName );}
                    if (authorEmail != null) {
                        String email = "mailto:" + authorEmail;
                        author.addProperty( FOAF.mbox, email )
                              .addProperty( FOAF.mbox_sha1sum, getMboxSha1Sum( email ));

                    }
                    System.out.println( String.format( "Collecting dev name = <%s>, email = <%s>", authorName, authorEmail ) );
                }
            }

            return m;
        }
        catch (NoHeadException e) {
            log.error( e.getMessage(), e );
        }
        catch (JGitInternalException e) {
            log.error( e.getMessage(), e );
        }

        throw new RuntimeException( "Failed to read Git developer information" );
    }


    /**
     * Initialise the git repository object
     * @return
     */
    protected Repository getGitRepository() {
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder.setGitDir( new File( projectDirectory ) )
                                           .readEnvironment()
                                           .findGitDir()
                                           .build();
            return repository;
        }
        catch (IOException e) {
            log.error( e.getMessage(), e );
            throw new RuntimeException( "Failed to intialize Git repository" );
        }
    }


    /**
     * Augment the project description resource by attaching the given developers using
     * the given relation, and then querying the web to discover more information about
     * the developers to add to the model.
     *
     * @param project Resource denoting the project
     * @param rel Property relating the project to the developers
     * @param devs Model containing foaf:Person resources denoting the discovered developers
     */
    protected void augmentProject( Resource project, Property rel, Model devs ) {
        for (ResIterator i = devs.listSubjectsWithProperty( RDF.type, FOAF.Person ); i.hasNext(); ) {
            Resource dev = i.next();

            // attach the person to the project
            devs.add( project, rel, dev );

            // see if we can find any foaf information about this person
            if (dev.hasProperty( FOAF.mbox )) {
                Model augmented = ModelFactory.createDefaultModel();
                String mboxSha1 = getMboxSha1Sum( dev.getProperty( FOAF.mbox ).getString() );
                collectFoafData( augmented, dev, mboxSha1 );

                enrichFoafDescription( dev, augmented, mboxSha1 );
            }
        }
    }

    /**
     * Collect FOAF data on the given developer by passing the SHA1 sum of their mbox
     * to Sindice
     *
     * @param augmented The cumulative model of augmented data we are collecting
     * @param dev The developer whose FOAF data we wish to collect
     */
    protected void collectFoafData( Model augmented, Resource dev, String mboxSha1 ) {
        augmented.add( sindiceQuery( mboxSha1 ) );
    }

    /**
     * Return the SHA1 sum of a given string
     * @param str The string to be hashed
     * @return The SHA1 sum as a hex encoded string
     */
    protected String getMboxSha1Sum( String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.update( str.getBytes() );
            return new BigInteger( 1,md.digest() ).toString(16);
        }
        catch (NoSuchAlgorithmException e) {
            log.error( e.getMessage(), e );
            throw new RuntimeException( "Failed to collect additional data" );
        }
    }

    /**
     * Return a model of combined data about the given search term, obtained by first querying
     * Sindice and then merging the matching documents into one model
     *
     * @param searchTerm A URL-encoded string to pass to the Sindice search API
     * @return A model of contents of the documents matching searchTerm
     */
    protected Model sindiceQuery( String searchTerm ) {
        Model sIndex = ModelFactory.createDefaultModel();

        // query phase: get a list of matching documents from Sindice
        try {
            sIndex.read( "http://api.sindice.com/v2/search?q=" + searchTerm );
        }
        catch (Exception e) {
            log.error( e.getMessage(), e );
            throw new RuntimeException( "Failed to perform Sindice query" );
        }

        // read phase: add those documents into a combined model
        Model m = ModelFactory.createDefaultModel();
        for (ResIterator i = sIndex.listSubjectsWithProperty( RDF.type, Sindice.Result ); i.hasNext(); ) {
            String docURL = i.next().getPropertyResourceValue( Sindice.link ).getURI();

            // we could be more sophisticated here about provenance tracking
            try {
                m.read( docURL );
            }
            catch (RuntimeException e) {
                // warn about the read failure, but carry on and read the other docs
                log.warn( String.format( "Failed to retrieve from %s because: %s", docURL, e.getMessage() ) );
            }
        }

        return m;
    }

    /**
     * Enrich the project description by adding
     *
     * @param dev The developer whose profile we're enriching
     * @param collected The model containing collected public info via sindice.com
     */
    protected void enrichFoafDescription( Resource dev, Model collected, String mboxSha1 ) {
        // copy the close-bounded description from augmented into the main model
        String queryString = String.format( "describe ?s where {?s <%s> \"%s\"}", FOAF.mbox_sha1sum.getURI(), mboxSha1 );
        Query query = QueryFactory.create( queryString ) ;
        QueryExecution qexec = QueryExecutionFactory.create( query, collected );
        dev.getModel().add( qexec.execDescribe() );

        // merge the new person resources
        Resource merged = dev;
        for (ResIterator i = dev.getModel().listResourcesWithProperty( FOAF.mbox_sha1sum, mboxSha1 ); i.hasNext(); ) {
            Resource newDev = i.next();
            merged = mergeResources( merged, newDev );
        }
    }

    /**
     * Merge resources r0 and r1. If either r0 or r1 is an anonymous resource, this is
     * the <em>smushing</em> operation. Otherwise, we add an <code>owl:sameAs</code>
     * relation to denote the relationship.
     * @param r0
     * @param r1
     */
    protected Resource mergeResources( Resource r0, Resource r1 ) {
        if (!r0.equals( r1 )) {
            // not already merged
            if (r0.isAnon()) {
                mergeInto( r0, r1 );
                return r1;
            }
            else if (r1.isAnon()) {
                mergeInto( r1, r0 );
                return r0;
            }
            else {
                mergeInto( r0, r1 );
                r0.addProperty( OWL.sameAs, r1 );
                return r1;
            }
        }
        return r0;
    }

    /**
     * Merge the source resource into the target resource by replacing every
     * triple <code>?source ?p ?o</code> with <code>?target ?p ?o</code>. This
     * has the effect of removing the source resource from the model
     *
     * @param source
     * @param target
     */
    protected void mergeInto( Resource source, Resource target ) {
        ResourceUtils.renameResource( source, target.getURI() );
    }


    /***********************************/
    /* Inner class definitions         */
    /***********************************/

}

