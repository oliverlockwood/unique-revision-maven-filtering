package fr.jcgay.maven.extension.revision;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.impl.MetadataGenerator;
import org.eclipse.aether.metadata.Metadata;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

class UniqueRevisionFiltering implements MetadataGenerator {

    private final ModelReader pomReader;
    private final ModelWriter pomWriter;
    private final Logger logger;

    UniqueRevisionFiltering(ModelReader pomReader, ModelWriter pomWriter, Logger logger) {
        this.pomReader = pomReader;
        this.pomWriter = pomWriter;
        this.logger = logger;
    }

    @Override
    public Collection<? extends Metadata> prepare(Collection<? extends Artifact> collection) {
        return Collections.emptyList();
    }

    @Override
    public Artifact transformArtifact(Artifact artifact) {
        if (!isPom(artifact)) {
            return artifact;
        }

        Model pom = readPom(artifact);
        if (pom == null) {
            return artifact;
        }

        boolean hasRevisionNumber = false;
        if (isRevision(pom.getVersion())) {
            hasRevisionNumber = true;
            pom.setVersion(artifact.getBaseVersion());
        }

        Parent parent = pom.getParent();
        if (parent != null && isRevision(parent.getVersion())) {
            hasRevisionNumber = true;
            parent.setVersion(artifact.getBaseVersion());
        }

        if (hasRevisionNumber) {
            File filteredPom = writePom(pom, artifact.getFile().getParent());
            return artifact.setFile(filteredPom);
        }

        return artifact;
    }

    @Override
    public Collection<? extends Metadata> finish(Collection<? extends Artifact> collection) {
        return Collections.emptyList();
    }

    private File writePom(Model pom, String path) {
        File target = new File(path + File.separator + "target" + File.separator + "pom.xml");
        try {
            pomWriter.write(target, Collections.<String, Object>emptyMap(), pom);
            return target;
        } catch (IOException e) {
            logger.warn("Cannot write filtered pom in " + target + ", original pom will be installed / deployed.", e);
            return null;
        }
    }

    private Model readPom(Artifact artifact) {
        try {
            return pomReader.read(artifact.getFile(), Collections.<String, Object>emptyMap());
        } catch (IOException e) {
            logger.warn("Error while reading pom from " + artifact.getFile() + ", original pom will be installed / deployed.", e);
            return null;
        }
    }

    private static boolean isRevision(String version) {
        return "${revision}".equals(version);
    }

    private static boolean isPom(Artifact artifact) {
        return "pom".equals(artifact.getExtension());
    }
}