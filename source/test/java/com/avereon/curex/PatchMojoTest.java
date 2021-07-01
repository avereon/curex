package com.avereon.curex;

import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.FlatRepositoryLayout;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.junit.jupiter.api.Test;

import java.io.File;

public class PatchMojoTest {

	@Test
	void testPatch() throws Exception {
		DefaultMavenExecutionRequest request = new DefaultMavenExecutionRequest();
		MavenArtifactRepository repo = new MavenArtifactRepository();
		repo.setUrl( new File( "source/test/resources" ).toURI().toURL().toString() );
		repo.setLayout( new FlatRepositoryLayout() );

//		request.setLocalRepository( repo );
//
//		DefaultMaven maven = new DefaultMaven();
//
//		maven.execute( request );
	}

}
