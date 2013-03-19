//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2011 by:
 - Department of Geography, University of Bonn -
 and
 - lat/lon GmbH -

 This library is free software; you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the Free
 Software Foundation; either version 2.1 of the License, or (at your option)
 any later version.
 This library is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 details.
 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation, Inc.,
 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Contact information:

 lat/lon GmbH
 Aennchenstr. 19, 53177 Bonn
 Germany
 http://lat-lon.de/

 Department of Geography, University of Bonn
 Prof. Dr. Klaus Greve
 Postfach 1147, 53001 Bonn
 Germany
 http://www.geographie.uni-bonn.de/deegree/

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/
package org.deegree.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Mojo to automatically setup deegree working sets and to move projects into working sets.
 * 
 * @goal setup-eclipse-working-sets
 * @aggregator
 * @requiresDirectInvocation
 * 
 * @author <a href="mailto:schmitz@occamlabs.de">Andreas Schmitz</a>
 * @author last edited by: $Author: stranger $
 * 
 * @version $Revision: $, $Date: $
 */
public class EclipseWorkingSetMojo extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    private int counter = 0;

    @Override
    public void execute()
                            throws MojoExecutionException, MojoFailureException {
        String eclipseWorkspace = System.getProperty( "eclipse.workspace" );
        String prefix = System.getProperty( "workingset-prefix" );
        if ( prefix == null ) {
            prefix = "d3-";
        }
        if ( prefix.isEmpty() ) {
            getLog().info( "Using no working set prefix." );
        } else {
            getLog().info( "Using working set prefix " + prefix );
        }
        if ( eclipseWorkspace == null ) {
            getLog().info( "You must specify the eclipse workspace using -Declipse.workspace=<workspacedir>." );
            return;
        }
        try {
            Map<String, String> moduleToWorkingSet = findModules();
            File workingsets = new File( eclipseWorkspace,
                                         ".metadata/.plugins/org.eclipse.ui.workbench/workingsets.xml" );
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse( workingsets );

            for ( Entry<String, String> e : moduleToWorkingSet.entrySet() ) {
                String mod = e.getKey();
                String ws = prefix + e.getValue();
                Element elem = getWorkingSetElement( doc, ws );
                Element item = doc.createElement( "item" );
                elem.appendChild( item );
                item.setAttribute( "elementID", "=" + mod );
                item.setAttribute( "factoryID", "org.eclipse.jdt.ui.PersistableJavaElementFactory" );
            }

            TransformerFactory fac = TransformerFactory.newInstance();
            Transformer trans = fac.newTransformer();
            trans.transform( new DOMSource( doc ), new StreamResult( workingsets ) );
        } catch ( Exception e ) {
            getLog().error( "Unable to read eclipse workingsets file: " + e.getLocalizedMessage(), e );
        }
    }

    private Element getWorkingSetElement( Document doc, String workingSet ) {
        NodeList nl = doc.getElementsByTagName( "workingSet" );
        for ( int i = 0; i < nl.getLength(); ++i ) {
            Element e = (Element) nl.item( i );
            if ( e.getAttribute( "name" ).equals( workingSet ) ) {
                return e;
            }
        }
        Element e = doc.getDocumentElement();
        Element ws = doc.createElement( "workingSet" );
        e.appendChild( ws );
        ws.setAttribute( "editPageId", "org.eclipse.jdt.ui.JavaWorkingSetPage" );
        ws.setAttribute( "factoryID", "org.eclipse.ui.internal.WorkingSetFactory" );
        ws.setAttribute( "id", System.currentTimeMillis() + "_" + ++counter );
        ws.setAttribute( "label", workingSet );
        ws.setAttribute( "name", workingSet );
        return ws;
    }

    private Map<String, String> findModules() {
        List<MavenProject> modules = project.getCollectedProjects();
        List<String> modList = new ArrayList<String>();
        for ( MavenProject p : modules ) {
            if ( p.getPackaging().equals( "pom" ) ) {
                continue;
            }
            modList.add( p.getArtifactId() );
        }
        Collections.sort( modList );
        List<String> workingsets = new ArrayList<String>();
        Map<String, String> moduleToWorkingSet = new HashMap<String, String>();
        for ( String mod : modList ) {
            int idx1 = mod.indexOf( "-" );
            if ( idx1 == -1 ) {
                continue;
            }
            int idx2 = mod.indexOf( "-", idx1 + 1 );
            if ( idx2 != -1 ) {
                String ws = mod.substring( idx1 + 1, idx2 );
                if ( !workingsets.contains( ws ) ) {
                    workingsets.add( ws );
                }
                moduleToWorkingSet.put( mod, ws );
            }
        }
        return moduleToWorkingSet;
    }

    public static class Workingset {

        /**
         * @parameter
         * @required
         */
        String name;

        /**
         * @parameter
         * @required
         */
        String modulePrefix;

        @Override
        public String toString() {
            return modulePrefix + "* to working set " + name;
        }

    }

}
