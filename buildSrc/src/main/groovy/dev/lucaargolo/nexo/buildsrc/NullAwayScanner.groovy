package dev.lucaargolo.nexo.buildsrc

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.TypePath
import org.objectweb.asm.TypeReference

class NullAwayScanner {

    static final String NULLABLE_DESC = 'Lorg/jetbrains/annotations/Nullable;'
    static final String NOTNULL_DESC = 'Lorg/jetbrains/annotations/NotNull;'
    static final Set<String> PRIMITIVES = ['Z','B','C','S','I','J','F','D','V'] as Set

    static List<String> scan(File dir) {
        def errors = []
        dir.eachFileRecurse { f ->
            if (!f.name.endsWith('.class')) return
            if (f.name.contains('$')) return
            try {
                new ClassReader(f.bytes).accept(
                    new AnnotationCheckVisitor(NULLABLE_DESC, NOTNULL_DESC, PRIMITIVES, errors), 0)
            } catch (Exception x) {
                errors << "${f.name}: ${x.message}"
            }
        }
        return errors
    }
}

class AnnotationCheckVisitor extends ClassVisitor {
    String nd, nnd, cn, cns
    Set<String> p
    List<String> err
    boolean en, rec, ann

    AnnotationCheckVisitor(String a, String b, Set<String> ps, List<String> e) {
        super(Opcodes.ASM9)
        nd = a
        nnd = b
        p = ps
        err = e
    }

    @Override
    void visit(int ver, int acc, String name, String sig, String sup, String[] ifs) {
        cn = name.replace('/', '.')
        cns = name
        en = (acc & Opcodes.ACC_ENUM) != 0
        ann = (acc & Opcodes.ACC_ANNOTATION) != 0
        rec = sup == 'java/lang/Record'
    }

    @Override
    FieldVisitor visitField(int acc, String name, String desc, String sig, Object value) {
        if (name.contains('$')) return null
        if (ann) return null
        if (en && (acc & Opcodes.ACC_ENUM) != 0) return null
        if (!ref(desc)) return null
        return new FieldAnnotationVisitor(this, name)
    }

    @Override
    MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex) {
        if (name.contains('$') || name == '<clinit>') return null
        if (ann) return null
        if (en && isEnumSynthetic(name, desc)) return null
        if (rec && isRecordSynthetic(name, desc)) return null
        return new MethodAnnotationVisitor(this, name, desc, name == '<init>')
    }

    boolean isEnumSynthetic(String n, String d) {
        (n == 'values' && d == "()[L${cns};") ||
        (n == 'valueOf' && d == "(Ljava/lang/String;)L${cns};") ||
        (n == '<init>' && d == '(Ljava/lang/String;I)V')
    }

    boolean isRecordSynthetic(String n, String d) {
        (n == 'equals' && d == '(Ljava/lang/Object;)Z') ||
        (n == 'hashCode' && d == '()I') ||
        (n == 'toString' && d == '()Ljava/lang/String;')
    }

    boolean ref(String d) {
        (d.startsWith('[') || d.startsWith('L')) && !(p.contains(d) || d.startsWith('T'))
    }

}

class FieldAnnotationVisitor extends FieldVisitor {
    AnnotationCheckVisitor cv
    String n
    boolean ok

    FieldAnnotationVisitor(AnnotationCheckVisitor c, String n) {
        super(Opcodes.ASM9)
        cv = c
        this.n = n
    }

    @Override
    AnnotationVisitor visitAnnotation(String d, boolean v) {
        if (d == cv.nd || d == cv.nnd) ok = true
        return null
    }

    @Override
    AnnotationVisitor visitTypeAnnotation(int tr, TypePath tp, String d, boolean v) {
        if (d == cv.nd || d == cv.nnd) ok = true
        return null
    }

    @Override
    void visitEnd() {
        if (!ok) cv.err << "${cv.cn}: field '${n}' missing @Nullable/@NotNull"
    }
}

class MethodAnnotationVisitor extends MethodVisitor {
    AnnotationCheckVisitor cv
    String mn, md
    boolean ctor
    boolean retOk
    boolean[] pOk

    MethodAnnotationVisitor(AnnotationCheckVisitor c, String n, String d,
                            boolean ct) {
        super(Opcodes.ASM9)
        cv = c
        mn = n
        md = d
        ctor = ct
        pOk = new boolean[Type.getArgumentTypes(d).length]
    }

    @Override
    AnnotationVisitor visitAnnotation(String d, boolean v) {
        if (d == cv.nd || d == cv.nnd) retOk = true
        return null
    }

    @Override
    AnnotationVisitor visitTypeAnnotation(int tr, TypePath tp, String d, boolean v) {
        if (d != cv.nd && d != cv.nnd) return null
        int sort = tr >>> 24
        if (sort == TypeReference.METHOD_RETURN) retOk = true
        else if (sort == TypeReference.METHOD_FORMAL_PARAMETER) {
            int i = (tr & 0x00FF0000) >> 16
            if (i < pOk.length) pOk[i] = true
        }
        return null
    }

    @Override
    AnnotationVisitor visitParameterAnnotation(int param, String d, boolean v) {
        if ((d == cv.nd || d == cv.nnd) && param < pOk.length) pOk[param] = true
        return null
    }

    @Override
    void visitEnd() {
        def at = Type.getArgumentTypes(md)
        if (!ctor) {
            def rd = md.substring(md.indexOf(')') + 1)
            if (cv.ref(rd) && rd != 'V' && !retOk)
                cv.err << "${cv.cn}: return type of '${mn}' missing @Nullable/@NotNull"
        }
        for (int i = 0; i < at.length; i++) {
            if (!cv.ref(at[i].descriptor)) continue
            if (!pOk[i])
                cv.err << "${cv.cn}: parameter ${i} of '${mn}' missing @Nullable/@NotNull"
        }
    }
}
