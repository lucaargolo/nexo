package dev.lucaargolo.nexo.buildsrc

import org.objectweb.asm.*

class NullAwayScanner {

    static final String NULLABLE_DESC = 'Lorg/jetbrains/annotations/Nullable;'
    static final String NOTNULL_DESC  = 'Lorg/jetbrains/annotations/NotNull;'
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
        nd = a; nnd = b; p = ps; err = e
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
        if (tv(desc, sig)) return null
        return new FieldAnnotationVisitor(this, name)
    }

    @Override
    MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex) {
        if (name.contains('$') || name == '<clinit>') return null
        if (ann) return null
        if (en && isEnumSynthetic(name, desc)) return null
        if (rec && name == '<init>') return null
        if (rec && isRecordSynthetic(name, desc)) return null
        return new MethodAnnotationVisitor(this, name, desc, sig, name == '<init>', rec)
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

    boolean tv(String desc, String sig) {
        sig != null && sig.length() >= 3 && sig.charAt(0) == 'T' &&
            Character.isJavaIdentifierStart(sig.charAt(1) as char)
    }
}

class FieldAnnotationVisitor extends FieldVisitor {
    AnnotationCheckVisitor cv
    String n
    boolean ok

    FieldAnnotationVisitor(AnnotationCheckVisitor c, String n) {
        super(Opcodes.ASM9)
        cv = c; this.n = n
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
    String mn, md, sig
    boolean ctor, rec
    boolean retOk
    boolean[] pOk

    MethodAnnotationVisitor(AnnotationCheckVisitor c, String n, String d, String s,
                            boolean ct, boolean r) {
        super(Opcodes.ASM9)
        cv = c; mn = n; md = d; sig = s; ctor = ct; rec = r
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
        if (rec && !ctor && at.length == 0) return
        if (!ctor) {
            def rd = md.substring(md.indexOf(')') + 1)
            if (cv.ref(rd) && rd != 'V' && !retOk && !rtv())
                cv.err << "${cv.cn}: return type of '${mn}' missing @Nullable/@NotNull"
        }
        for (int i = 0; i < at.length; i++) {
            if (!cv.ref(at[i].descriptor)) continue
            if (ptv(i)) continue
            if (!pOk[i])
                cv.err << "${cv.cn}: parameter ${i} of '${mn}' missing @Nullable/@NotNull"
        }
    }

    boolean rtv() {
        if (sig == null) return false
        try {
            int p = sig.lastIndexOf(')')
            if (p < 0) return false
            def rs = sig.substring(p + 1)
            return rs.length() >= 3 && rs.charAt(0) == 'T' &&
                Character.isJavaIdentifierStart(rs.charAt(1) as char)
        } catch (Exception _) { return false }
    }

    boolean ptv(int idx) {
        if (sig == null) return false
        try {
            def ps = sig.substring(sig.indexOf('(') + 1, sig.lastIndexOf(')'))
            def parts = splitGenericParams(ps)
            if (idx < parts.size()) {
                def part = parts[idx]
                return part.length() >= 3 && part.charAt(0) == 'T' &&
                    Character.isJavaIdentifierStart(part.charAt(1) as char)
            }
        } catch (Exception _) { return false }
        return false
    }

    static List<String> splitGenericParams(String s) {
        def r = []
        int d = 0
        def b = new StringBuilder()
        for (char c : s.toCharArray()) {
            if (c == '<') d++
            else if (c == '>') d--
            if (c == ';' && d == 0) { b.append(c); r << b.toString(); b.setLength(0) }
            else b.append(c)
        }
        if (b.length() > 0) r << b.toString()
        return r
    }
}
