package cc.tweaked.test;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.lwjgl.opengl.GL45C;

class HelloTest {
    public void useBlock() {
        System.out.println(DefaultVertexFormat.POSITION_TEX);
        System.out.println(GL45C.GL_QUADS);

        HelloClient.useClientClasses();
        Hello.useBlock();
    }
}
