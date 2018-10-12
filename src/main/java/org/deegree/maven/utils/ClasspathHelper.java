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
package org.deegree.maven.utils;

import static java.lang.Thread.currentThread;
import static java.security.AccessController.doPrivileged;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

/**
 * 
 * @author <a href="mailto:schmitz@lat-lon.de">Andreas Schmitz</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class ClasspathHelper {

    /**
     * @return a list of all (possibly transitive) artifacts of the given type
     */
    public static Set<?> getDependencyArtifacts( MavenProject project, ArtifactResolver artifactResolver,
                                                 final RepositorySystem repositorySystem,
                                                 ArtifactRepository localRepository, final String type,
                                                 boolean transitively ) {

        List<Dependency> dependencies = project.getDependencies();
        Set<Artifact> artifacts = dependencies.stream()
                .filter((Dependency dep) -> dep.getType().equals(type))
                .map(
                    (Dependency dep) -> repositorySystem.createArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getScope(), dep.getType()))
                .collect(Collectors.toSet());

        ArtifactResolutionResult result;
        Artifact mainArtifact = project.getArtifact();
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(mainArtifact);
        request.setArtifactDependencies(artifacts);
        request.setLocalRepository(localRepository);
        request.setRemoteRepositories(project.getRemoteArtifactRepositories());

        result = artifactResolver.resolve(request);

        if ( transitively ) {
            return result.getArtifacts();
        }

        LinkedHashSet<Artifact> set = new LinkedHashSet<>();
        if ( mainArtifact.getType() != null && mainArtifact.getType().equals( type ) ) {
            set.add( mainArtifact );
        }
        set.addAll( artifacts );
        Set<Artifact> collected = set.stream()
                .filter(dep -> dep.getType().equals(type))
                .collect(Collectors.toSet());

        return collected;
    }

    private static Set<?> resolveDeps( MavenProject project, ArtifactResolver artifactResolver,
                                       final RepositorySystem repositorySystem,
                                       ArtifactRepository localRepository ) {

        List<Dependency> dependencies = project.getDependencies();

        Set<Artifact> artifacts = dependencies.stream()
                .map(
                        (Dependency dep) -> repositorySystem.createArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getScope(), dep.getType()))
                .collect(Collectors.toSet());

        artifacts.add( project.getArtifact() );

        Artifact mainArtifact = project.getArtifact();
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(mainArtifact);
        request.setArtifactDependencies(artifacts);
        request.setLocalRepository(localRepository);
        request.setRemoteRepositories(project.getRemoteArtifactRepositories());
        request.setResolveTransitively(true);

        ArtifactResolutionResult result = artifactResolver.resolve( request );

        return result.getArtifacts();
    }

    public static void addDependenciesToClasspath( MavenProject project, ArtifactResolver artifactResolver,
                                                   final RepositorySystem repositorySystem,
                                                   ArtifactRepository localRepository )
                            throws MojoExecutionException {
        try {
            Set<?> artifacts = resolveDeps( project, artifactResolver, repositorySystem, localRepository );
            final URL[] urls = new URL[artifacts.size()];
            Iterator<?> itor = artifacts.iterator();
            int i = 0;
            while ( itor.hasNext() ) {
                urls[i++] = ( (Artifact) itor.next() ).getFile().toURI().toURL();
            }

            doPrivileged( new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    URLClassLoader cl = new URLClassLoader( urls, currentThread().getContextClassLoader() );
                    currentThread().setContextClassLoader( cl );
                    return null;
                }
            } );

        } catch ( Throwable e ) {
            throw new MojoExecutionException( e.getLocalizedMessage(), e );
        }
    }

}
