package cc.tweaked.vanillaextract.configurations;

import org.gradle.api.Project;
import org.gradle.api.capabilities.Capability;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link Capability} who derives its group, name and version from a project's.
 */
final class ProjectCapability implements Capability {
    private final Project project;
    private final String name;

    /**
     * @param project The project which is exposing the capability.
     * @param name    The name of the capability.
     */
    public ProjectCapability(Project project, String name) {
        this.project = project;
        this.name = name;
    }

    @Override
    public String getGroup() {
        return project.getGroup().toString();
    }

    @Override
    public String getName() {
        return project.getName() + "-" + name;
    }

    @Nullable
    @Override
    public String getVersion() {
        return project.getVersion().toString();
    }

    @Override
    public String toString() {
        return "ProjectCapability[" + project + " with " + name + "]";
    }
}
