//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2010 by:
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.deegree.maven.ithelper.ServiceIntegrationTestHelper;

/**
 *
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
@Execute(goal = "test-services", phase = LifecyclePhase.INTEGRATION_TEST)
@Mojo(name = "test-services", defaultPhase = LifecyclePhase.INTEGRATION_TEST)
public class ServiceIntegrationTestMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "true")
    private boolean testCapabilities;

    @Parameter(defaultValue = "true")
    private boolean testLayers;

    @Parameter(defaultValue = "true")
    private boolean testRequests;

    @Parameter(defaultValue = "${project.basedir}/src/main/webapp/WEB-INF/workspace")
    private File workspace;

    @Override
    public void execute()
                            throws MojoExecutionException,
                            MojoFailureException {
        try {
            if ( !workspace.exists() ) {
                workspace = new File( project.getBasedir(), "src/main/webapp/WEB-INF/conf" );
                if ( !workspace.exists() ) {
                    getLog().error( "Could not find a workspace to operate on." );
                    throw new MojoFailureException( "Could not find a workspace to operate on." );
                }
                getLog().warn( "Default/configured workspace did not exist, using existing " + workspace
                               + " instead." );
            }

            ServiceIntegrationTestHelper helper = new ServiceIntegrationTestHelper( project, getLog() );

            File[] listed = new File( workspace, "services" ).listFiles();
            if ( listed != null ) {
                for ( File f : listed ) {
                    String nm = f.getName().toLowerCase();
                    if ( nm.length() != 7 ) {
                        continue;
                    }
                    String service = nm.substring( 0, 3 ).toUpperCase();
                    if ( testCapabilities ) {
                        helper.testCapabilities( service );
                    }
                    if ( testLayers ) {
                        helper.testLayers( service );
                        getLog().info( "All maps can be requested." );
                    }
                }
            }

            if ( testRequests ) {
                helper.testRequests();
            }
        } catch ( NoClassDefFoundError e ) {
            getLog().warn( "Class not found, not performing any tests." );
        }
    }

}
