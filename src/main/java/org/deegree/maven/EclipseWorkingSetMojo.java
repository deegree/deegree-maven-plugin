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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Mojo to automatically setup deegree working sets and to move projects into working sets.
 * 
 * @author <a href="mailto:schmitz@occamlabs.de">Andreas Schmitz</a>
 * @author last edited by: $Author: stranger $
 * 
 * @version $Revision: $, $Date: $
 */
@Execute(goal = "setup-eclipse-working-sets")
@Mojo(name = "setup-eclipse-working-sets", aggregator = true, requiresDirectInvocation = true)
public class EclipseWorkingSetMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
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
            File workbench = new File( eclipseWorkspace, ".metadata/.plugins/org.eclipse.ui.workbench/workbench.xml" );
            File xmi = new File( eclipseWorkspace, ".metadata/.plugins/org.eclipse.e4.workbench/workbench.xmi" );
            File dialog = new File( eclipseWorkspace, ".metadata/.plugins/org.eclipse.jdt.ui/dialog_settings.xml" );

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document wsDoc = dBuilder.parse( workingsets );
            Document wbDoc = null;
            if ( workbench.exists() ) {
                wbDoc = dBuilder.parse( workbench );
            }
            Document xmiDoc = dBuilder.parse( xmi );
            Document dialogDoc = dBuilder.parse( dialog );

            for ( Entry<String, String> e : moduleToWorkingSet.entrySet() ) {
                String mod = e.getKey();
                String ws = prefix + e.getValue();
                Element elem = getWorkingSetElement( wsDoc, ws );
                Element item = wsDoc.createElement( "item" );
                elem.appendChild( item );
                item.setAttribute( "elementID", "=" + mod );
                item.setAttribute( "factoryID", "org.eclipse.jdt.ui.PersistableJavaElementFactory" );
                if ( workbench.exists() ) {
                    updateWorkbenchDocument( wbDoc, ws );
                }
                updateXmiDocument( xmiDoc, ws );
                updateDialogDocument( dialogDoc, ws );
            }

            TransformerFactory fac = TransformerFactory.newInstance();
            Transformer trans = fac.newTransformer();
            trans.transform( new DOMSource( wsDoc ), new StreamResult( workingsets ) );
            if ( workbench.exists() ) {
                trans.transform( new DOMSource( wbDoc ), new StreamResult( workbench ) );
            }
            trans.transform( new DOMSource( xmiDoc ), new StreamResult( xmi ) );
            trans.transform( new DOMSource( dialogDoc ), new StreamResult( dialog ) );
        } catch ( Exception e ) {
            getLog().error( "Unable to update eclipse configuration files: " + e.getLocalizedMessage(), e );
            throw new MojoFailureException( e.getLocalizedMessage(), e );
        }
    }

    private void updateDialogDocument( Document doc, String workingSet )
                            throws IOException, SAXException, TransformerException, ParserConfigurationException {
        NodeList nl = doc.getElementsByTagName( "section" );
        for ( int i = 0; i < nl.getLength(); ++i ) {
            Element e = (Element) nl.item( i );
            if ( e.getAttribute( "name" ).equals( "org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart" ) ) {
                nl = doc.getElementsByTagName( "item" );
                for ( int j = 0; j < nl.getLength(); ++j ) {
                    e = (Element) nl.item( j );
                    if ( e.getAttribute( "key" ).equals( "memento" ) ) {
                        byte[] bs = e.getAttribute( "value" ).getBytes();

                        ByteArrayInputStream in = new ByteArrayInputStream( bs );

                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                        Document wbDoc = dBuilder.parse( in );

                        updateWorkbenchDocument( wbDoc, workingSet );

                        TransformerFactory fac = TransformerFactory.newInstance();
                        Transformer trans = fac.newTransformer();
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        trans.transform( new DOMSource( wbDoc ), new StreamResult( bos ) );

                        bos.close();
                        bs = bos.toByteArray();
                        e.setAttribute( "value", new String( bs ) );
                    }
                }
            }
        }
    }

    private void updateXmiDocument( Document doc, String workingSet )
                            throws ParserConfigurationException, SAXException, IOException, TransformerException {
        // XML documents in an XML attribute, that's really great
        NodeList nl = doc.getElementsByTagName( "sharedElements" );
        for ( int i = 0; i < nl.getLength(); ++i ) {
            Element e = (Element) nl.item( i );
            if ( e.getAttribute( "elementId" ).equals( "org.eclipse.jdt.ui.PackageExplorer" ) ) {
                NodeList nl2 = e.getElementsByTagName( "persistedState" );
                Element val = (Element) nl2.item( 0 );
                byte[] bs = val.getAttribute( "value" ).getBytes();

                ByteArrayInputStream in = new ByteArrayInputStream( bs );

                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document wbDoc = dBuilder.parse( in );

                updateWorkbenchDocument( wbDoc, workingSet );

                TransformerFactory fac = TransformerFactory.newInstance();
                Transformer trans = fac.newTransformer();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                trans.transform( new DOMSource( wbDoc ), new StreamResult( bos ) );

                bos.close();
                bs = bos.toByteArray();
                val.setAttribute( "value", new String( bs ) );
            }
            if ( e.getAttribute( "elementID" ).equals( "org.eclipse.ui.navigator.PackageExplorer" ) ) {
                NodeList nl2 = e.getElementsByTagName( "persistedState" );
                Element val = (Element) nl2.item( 0 );
                byte[] bs = val.getAttribute( "value" ).getBytes();

                ByteArrayInputStream in = new ByteArrayInputStream( bs );

                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document wbDoc = dBuilder.parse( in );

                updateWorkingSetListDocument( wbDoc, workingSet );

                TransformerFactory fac = TransformerFactory.newInstance();
                Transformer trans = fac.newTransformer();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                trans.transform( new DOMSource( wbDoc ), new StreamResult( bos ) );

                bos.close();
                bs = bos.toByteArray();
                val.setAttribute( "value", new String( bs ) );
            }
        }
    }

    private void updateWorkingSetListDocument( Document doc, String workingSet ) {
        Element e = doc.getDocumentElement();
        String list = e.getAttribute( "currentWorkingSetName" );
        String[] wss = list.split( ":" );
        List<String> workingsets = new ArrayList<String>( Arrays.asList( wss ) );
        if ( !workingsets.contains( workingSet ) ) {
            e.setAttribute( "currentWorkingSetName", list + ":" + workingSet );
        }
    }

    private void updateWorkbenchDocument( Document doc, String workingSet ) {
        maybeAppend( doc, "activeWorkingSet", workingSet );
        maybeAppend( doc, "allWorkingSets", workingSet );
    }

    private void maybeAppend( Document doc, String elemName, String workingSet ) {
        NodeList nl = doc.getElementsByTagName( elemName );
        Element lastElem = null;
        for ( int i = 0; i < nl.getLength(); ++i ) {
            lastElem = (Element) nl.item( i );
            if ( lastElem.getAttribute( "workingSetName" ).equals( workingSet ) ) {
                return;
            }
        }
        Element e = doc.createElement( elemName );
        e.setAttribute( "workingSetName", workingSet );
        if ( lastElem == null ) {
            doc.getDocumentElement().appendChild( e );
        } else {
            Node n = lastElem.getNextSibling();
            lastElem.getParentNode().insertBefore( e, n );
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
