package cc.tweaked.vanillaextract.core.util;

import cc.tweaked.vanillaextract.core.MavenArtifact;
import org.jetbrains.annotations.Nullable;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utilities for writing a {@code POM} file.
 *
 * @see <a href="https://maven.apache.org/pom.html">The POM documentation.</a>
 */
public final class PomWriter {
    private static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";
    private static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newFactory();

    private final String group;
    private final String artifact;
    private final String version;

    private @Nullable String name;
    private @Nullable String description;
    private @Nullable String url;
    private @Nullable String comment;

    private final List<Dependency> dependencies = new ArrayList<>();

    /**
     * Create a new POM writer for a Maven module.
     *
     * @param group    The module's group.
     * @param artifact The module's name.
     * @param version  The module version.
     */
    public PomWriter(String group, String artifact, String version) {
        this.group = group;
        this.artifact = artifact;
        this.version = version;
    }

    /**
     * Set the display name for this artifact.
     *
     * @param name The display name.
     * @return {@code this}, for chaining.
     */
    public PomWriter setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the description for this artifact.
     *
     * @param description The display name.
     * @return {@code this}, for chaining.
     */
    public PomWriter setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Set the URL for this artifact.
     *
     * @param url The URL.
     * @return {@code this}, for chaining.
     */
    public PomWriter setUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * Add a comment to the POM file.
     *
     * @param comment The comment.
     * @return {@code this}, for chaining.
     */
    public PomWriter setComment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * Add a dependency to this POM file.
     *
     * @param group    The group of this dependency.
     * @param artifact The module name of this dependency.
     * @param version  The version of this dependency.
     * @param scope    The scope, either {@code compile} or {@code runtime}.
     * @return {@code this}, for chaining.
     */
    public PomWriter addDependency(String group, String artifact, String version, String scope) {
        return addDependency(new MavenArtifact(group, artifact, version, null, null), scope, true);
    }

    /**
     * Add a dependency to this POM.
     *
     * @param coordinate The maven artifact of this dependency.
     * @param scope      The scope, either {@code compile} or {@code runtime}.
     * @param transitive Whether this dependency is transitive or not.
     * @return {@code this}, for chaining.
     */
    public PomWriter addDependency(MavenArtifact coordinate, String scope, boolean transitive) {
        dependencies.add(new Dependency(coordinate, scope, transitive));
        return this;
    }

    public void write(Writer out) throws XMLStreamException {
        var writer = XML_OUTPUT_FACTORY.createXMLStreamWriter(out);

        // <?xml version="1.0" encoding="UTF-8"?>
        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeCharacters("\n");


        // <project xmlns="http://maven.apache.org/POM/4.0.0" xsi:schemaLocation="..." xmlns:xsi="...">
        writer.writeStartElement("project");
        writer.writeAttribute("xmlns", "http://maven.apache.org/POM/4.0.0");
        writer.writeNamespace("xsi", XSI);
        writer.writeAttribute(XSI, "schemaLocation", "http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd");
        writer.writeCharacters("\n");

        var xml = new XmlWriter(writer);
        xml.withIndent(() -> {
            xml.writeElement("modelVersion", "4.0.0");
            xml.writeElement("groupId", group);
            xml.writeElement("artifactId", artifact);
            xml.writeElement("version", version);

            if (name != null) xml.writeElement("name", name);
            if (description != null) xml.writeElement("description", description);
            if (url != null) xml.writeElement("url", url);
            if (comment != null) xml.writeComment(comment);

            if (!dependencies.isEmpty()) {
                xml.withElement("dependencies", () -> {
                    for (var dependency : dependencies) {
                        xml.withElement("dependency", () -> dependency.write(xml));
                    }
                });
            }
        });

        writer.writeEndElement();
        writer.writeEndDocument();
        writer.writeCharacters("\n");
    }

    /**
     * A dependency in a POM file.
     *
     * @param artifact   The artifact to depend on.
     * @param scope      The scope of this dependency.
     * @param transitive Whether transitive dependencies should be included or not.
     * @see <a href="https://maven.apache.org/pom.html#Dependencies">The POM documentation.</a>
     */
    private record Dependency(
        MavenArtifact artifact, String scope, boolean transitive
    ) {
        void write(XmlWriter xml) throws XMLStreamException {
            xml.writeElement("groupId", artifact.group());
            xml.writeElement("artifactId", artifact.name());
            xml.writeElement("version", artifact.version());
            xml.writeElement("scope", scope);
            if (artifact.classifier() != null) xml.writeElement("classifier", artifact.classifier());
            if (artifact.extension() != null) xml.writeElement("type", artifact.extension());
            if (!transitive) {
                xml.writeIndent();
                xml.writer.writeStartElement("exclusions");
                xml.writer.writeStartElement("exclusion");

                xml.writer.writeStartElement("groupId");
                xml.writer.writeCharacters("*");
                xml.writer.writeEndElement();
                xml.writer.writeStartElement("artifactId");
                xml.writer.writeCharacters("*");
                xml.writer.writeEndElement();

                xml.writer.writeEndElement();
                xml.writer.writeEndElement();
                xml.writeNewline();
            }
        }
    }

    private static class XmlWriter {
        private final XMLStreamWriter writer;
        private int indent;

        private XmlWriter(XMLStreamWriter writer) {
            this.writer = writer;
        }

        private void writeIndent() throws XMLStreamException {
            for (int i = 0; i < indent; i++) writer.writeCharacters("    ");
        }

        private void writeNewline() throws XMLStreamException {
            writer.writeCharacters("\n");
        }

        public void writeComment(String comment) throws XMLStreamException {
            writeIndent();
            if (comment.indexOf('\n') < 0) {
                writer.writeComment(" " + comment + " ");
            } else {
                var indentString = "    ".repeat(this.indent);
                writer.writeComment("\n" + comment.lines().map(x -> indentString + x).collect(Collectors.joining("\n")) + "\n");
            }
            writeNewline();
        }

        public void writeElement(String element, String contents) throws XMLStreamException {
            writeIndent();
            writer.writeStartElement(element);
            writer.writeCharacters(contents);
            writer.writeEndElement();
            writeNewline();
        }

        public void withElement(String name, XmlTask task) throws XMLStreamException {
            writeIndent();
            writer.writeStartElement(name);
            writeNewline();

            indent++;
            task.run();
            indent--;

            writeIndent();
            writer.writeEndElement();
            writeNewline();
        }

        public void withIndent(XmlTask task) throws XMLStreamException {
            indent++;
            task.run();
            indent--;
        }
    }

    private interface XmlTask {
        void run() throws XMLStreamException;
    }
}
