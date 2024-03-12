package cc.tweaked.test;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.lwjgl.opengl.GL45C;

class HelloClient {
    public static void useClientClasses() {
        System.out.println(DefaultVertexFormat.POSITION_TEX);
        System.out.println(GL45C.GL_QUADS);
    }

    public static void useMainClasses() {
        Hello.useBlock();
    }
}
