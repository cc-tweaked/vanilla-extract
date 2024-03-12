package cc.tweaked.vanillaextract.decompile;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.Arrays;

/**
 * Applies line number remappings from Vineflower to an input class.
 */
class LineNumberMapper extends ClassVisitor {
    private final int[] oldLineNumbers, newLineNumbers;
    private final int oldMaxLine, newMaxLine;

    public LineNumberMapper(@Nullable ClassVisitor classVisitor, int[] lineMapping) {
        super(Opcodes.ASM9, classVisitor);

        var oldLineNumbers = this.oldLineNumbers = new int[lineMapping.length / 2];
        var newLineNumbers = this.newLineNumbers = new int[lineMapping.length / 2];
        for (int i = 0; i < lineMapping.length / 2; i++) {
            oldLineNumbers[i] = lineMapping[i * 2];
            newLineNumbers[i] = lineMapping[i * 2 + 1];
        }

        oldMaxLine = lineMapping.length == 0 ? 1 : oldLineNumbers[oldLineNumbers.length - 1];
        newMaxLine = lineMapping.length == 0 ? 1 : newLineNumbers[newLineNumbers.length - 1];
    }

    public static byte[] remapClass(byte[] classContents, int[] lineMapping) {
        var cv = new ClassReader(classContents);
        var cw = new ClassWriter(cv, 0);
        cv.accept(new LineNumberMapper(cw, lineMapping), 0);
        return cw.toByteArray();
    }

    @Override
    public @Nullable MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        var mw = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (mw == null) return null;
        return new MethodVisitor(Opcodes.ASM9, mw) {
            @Override
            public void visitLineNumber(int line, Label start) {
                super.visitLineNumber(remapLine(line), start);
            }
        };
    }

    int remapLine(int oldLine) {
        // Handle the edge cases to do a bounds check.
        if (oldLine <= 0) return oldLine;
        if (oldLine >= oldMaxLine) return newMaxLine;

        // Otherwise binary search to find the exact line number (index >= 0) or the next line number if not found.
        int index = Arrays.binarySearch(oldLineNumbers, oldLine);
        return newLineNumbers[index >= 0 ? index : -index - 1];
    }
}
