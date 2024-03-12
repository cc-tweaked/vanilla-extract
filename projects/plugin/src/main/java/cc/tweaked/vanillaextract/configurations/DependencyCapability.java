package cc.tweaked.vanillaextract.configurations;

import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.capabilities.Capability;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link Capability} who derives its group and name from a dependency's.
 */
final class DependencyCapability implements Capability {
    private final ModuleDependency dependency;
    private final String name;

    /**
     * @param dependency The dependency which is exposing the capability.
     * @param name       The name of the capability.
     */
    DependencyCapability(ModuleDependency dependency, String name) {
        this.dependency = dependency;
        this.name = name;
    }

    @Override
    public String getGroup() {
        return dependency.getGroup();
    }

    @Override
    public String getName() {
        return dependency.getName() + "-" + name;
    }

    @Nullable
    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public String toString() {
        return "DependencyCapability[" + dependency + " with " + name + "]";
    }
}
