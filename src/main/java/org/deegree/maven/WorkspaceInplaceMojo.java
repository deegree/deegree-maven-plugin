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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

import static org.apache.commons.io.FileUtils.copyFileToDirectory;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.deegree.commons.utils.io.Zip.unzip;
import static org.deegree.maven.utils.ClasspathHelper.getDependencyArtifacts;

/**
 *
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
@Execute(goal = "workspace-inplace", phase = LifecyclePhase.GENERATE_RESOURCES)
@Mojo(name = "workspace-inplace", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class WorkspaceInplaceMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Component
    private ArtifactResolver artifactResolver;

    @Component
    private RepositorySystem repositorySystem;

    @Parameter(defaultValue = "${localRepository}")
    private ArtifactRepository localRepository;

    /**
     * If set to true, all existing files will be overwritten if contained in a dependency.
     */
    @Parameter(defaultValue = "false", required = true, readonly = true)
    private boolean overwrite;

    @Override
    public void execute()
                            throws MojoFailureException {
        Log log = getLog();

        File dir = determineWorkspaceDirectory();

        try {
            Set<?> workspaces = getDependencyArtifacts( project, artifactResolver, repositorySystem, localRepository,
                                                        "deegree-workspace", true );

            copyDependencies( log, dir );

            for ( Object o : workspaces ) {
                Artifact a = (Artifact) o;
                if ( a.getArtifactId().equals( project.getArtifactId() )
                     && a.getGroupId().equals( project.getArtifactId() ) ) {
                    continue;
                }
                log.info( "Unpacking workspace " + a.getArtifactId() );
                FileInputStream in = new FileInputStream( a.getFile() );
                try {
                    unzip( in, dir, overwrite );
                } finally {
                    closeQuietly( in );
                }
            }
        } catch ( IOException e ) {
            throw new MojoFailureException( "Could not extract workspace dependencies in place: "
                                            + e.getLocalizedMessage(), e );
        }

    }

    private void copyDependencies( Log log, File dir )
                            throws MojoFailureException,
                            IOException {
        Set<?> jarDeps = getDependencyArtifacts( project, artifactResolver, repositorySystem, localRepository, "jar",
                                                 false );

        File modules = new File( dir, "modules" );
        if ( !jarDeps.isEmpty() && !modules.isDirectory() && !modules.mkdirs() ) {
            throw new MojoFailureException( "Could not create modules directory in workspace." );
        }
        for ( Object o : jarDeps ) {
            Artifact a = (Artifact) o;
            if ( a.getScope() == null || a.getScope().equalsIgnoreCase( "runtime" )
                 || a.getScope().equalsIgnoreCase( "compile" ) ) {
                // ignore root artifact, they don't have a file yet in never maven versions
                if (a.getFile() == null) {
                    continue;
                }
                log.info( "Copying " + a + " to workspace modules directory." );
                copyFileToDirectory( a.getFile(), modules );
            }
        }
    }

    private File determineWorkspaceDirectory() {
        File dir = new File( project.getBasedir(), "src/main/webapp/WEB-INF/workspace" );
        if ( !dir.isDirectory() ) {
            dir = new File( project.getBasedir(), "src/main/webapp/WEB-INF/conf" );
        }
        if ( !dir.isDirectory() ) {
            dir = new File( project.getBasedir(), "src/main/workspace" );
        }
        return dir;
    }

}
