package cc.tweaked.vanillaextract;

import org.gradle.api.Action;
import org.gradle.api.DomainObjectCollection;
import org.gradle.api.Task;

public class GradleUtils {
    public static <U extends Task, T extends U> void lazyNamed(DomainObjectCollection<U> objects, String name, Class<T> klass, Action<? super T> configure) {
        objects.withType(klass).configureEach(c -> {
            if (c.getName().equals(name)) configure.execute(c);
        });
    }
}
