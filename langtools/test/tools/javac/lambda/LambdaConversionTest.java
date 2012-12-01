/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @bug 8003280
 * @summary Add lambda tests
 *  perform several automated checks in lambda conversion, esp. around accessibility
 * @author  Maurizio Cimadamore
 * @run main LambdaConversionTest
 */

import com.sun.source.util.JavacTask;
import java.net.URI;
import java.util.Arrays;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class LambdaConversionTest {

    enum PackageKind {
        NO_PKG(""),
        PKG_A("a");

        String pkg;

        PackageKind(String pkg) {
            this.pkg = pkg;
        }

        String getPkgDecl() {
            return this == NO_PKG ?
                "" :
                "package " + pkg + ";";
        }

        String getImportStat() {
            return this == NO_PKG ?
                "" :
                "import " + pkg + ".*;";
        }
    }

    enum SamKind {
        CLASS("public class Sam {  }"),
        ABSTACT_CLASS("public abstract class Sam {  }"),
        ANNOTATION("public @interface Sam {  }"),
        ENUM("public enum Sam { }"),
        INTERFACE("public interface Sam { \n #METH; \n }");

        String sam_str;

        SamKind(String sam_str) {
            this.sam_str = sam_str;
        }

        String getSam(String methStr) {
            return sam_str.replaceAll("#METH", methStr);
        }
    }

    enum ModifierKind {
        PUBLIC("public"),
        PACKAGE("");

        String modifier_str;

        ModifierKind(String modifier_str) {
            this.modifier_str = modifier_str;
        }

        boolean stricterThan(ModifierKind that) {
            return this.ordinal() > that.ordinal();
        }
    }

    enum TypeKind {
        EXCEPTION("Exception"),
        PKG_CLASS("PackageClass");

        String typeStr;

        private TypeKind(String typeStr) {
            this.typeStr = typeStr;
        }
    }

    enum MethodKind {
        NONE(""),
        NON_GENERIC("public #R m(#ARG s) throws #T;"),
        GENERIC("public <X> #R m(#ARG s) throws #T;");

        String methodTemplate;

        private MethodKind(String methodTemplate) {
            this.methodTemplate = methodTemplate;
        }

        String getMethod(TypeKind retType, TypeKind argType, TypeKind thrownType) {
            return methodTemplate.replaceAll("#R", retType.typeStr).
                    replaceAll("#ARG", argType.typeStr).
                    replaceAll("#T", thrownType.typeStr);
        }
    }

    public static void main(String[] args) throws Exception {
        for (PackageKind samPkg : PackageKind.values()) {
            for (ModifierKind modKind : ModifierKind.values()) {
                for (SamKind samKind : SamKind.values()) {
                    for (MethodKind meth : MethodKind.values()) {
                        for (TypeKind retType : TypeKind.values()) {
                            for (TypeKind argType : TypeKind.values()) {
                                for (TypeKind thrownType : TypeKind.values()) {
                                    new LambdaConversionTest(samPkg, modKind, samKind,
                                            meth, retType, argType, thrownType).test();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    PackageKind samPkg;
    ModifierKind modKind;
    SamKind samKind;
    MethodKind meth;
    TypeKind retType;
    TypeKind argType;
    TypeKind thrownType;

    SourceFile samSourceFile = new SourceFile("Sam.java", "#P \n #C") {
        public String toString() {
            return template.replaceAll("#P", samPkg.getPkgDecl()).
                    replaceAll("#C", samKind.getSam(meth.getMethod(retType, argType, thrownType)));
        }
    };

    SourceFile pkgClassSourceFile = new SourceFile("PackageClass.java",
                                                   "#P\n #M class PackageClass extends Exception { }") {
        public String toString() {
            return template.replaceAll("#P", samPkg.getPkgDecl()).
                    replaceAll("#M", modKind.modifier_str);
        }
    };

    SourceFile clientSourceFile = new SourceFile("Client.java",
                                                 "#I\n class Client { Sam s = x -> null; }") {
        public String toString() {
            return template.replaceAll("#I", samPkg.getImportStat());
        }
    };

    LambdaConversionTest(PackageKind samPkg, ModifierKind modKind, SamKind samKind,
            MethodKind meth, TypeKind retType, TypeKind argType, TypeKind thrownType) {
        this.samPkg = samPkg;
        this.modKind = modKind;
        this.samKind = samKind;
        this.meth = meth;
        this.retType = retType;
        this.argType = argType;
        this.thrownType = thrownType;
    }

    void test() throws Exception {
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        DiagnosticChecker dc = new DiagnosticChecker();
        JavacTask ct = (JavacTask)tool.getTask(null, null, dc,
                null, null, Arrays.asList(samSourceFile, pkgClassSourceFile, clientSourceFile));
        ct.analyze();
        if (dc.errorFound == checkSamConversion()) {
            throw new AssertionError(samSourceFile + "\n\n" + pkgClassSourceFile + "\n\n" + clientSourceFile);
        }
    }

    boolean checkSamConversion() {
        if (samKind != SamKind.INTERFACE) {
            //sam type must be an interface
            return false;
        } else if (meth != MethodKind.NON_GENERIC) {
            //target method must be non-generic
            return false;
        } else if (samPkg != PackageKind.NO_PKG &&
                modKind != ModifierKind.PUBLIC &&
                (retType == TypeKind.PKG_CLASS ||
                argType == TypeKind.PKG_CLASS ||
                thrownType == TypeKind.PKG_CLASS)) {
            //target must not contain inaccessible types
            return false;
        } else {
            return true;
        }
    }

    abstract class SourceFile extends SimpleJavaFileObject {

        protected String template;

        public SourceFile(String filename, String template) {
            super(URI.create("myfo:/" + filename), JavaFileObject.Kind.SOURCE);
            this.template = template;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return toString();
        }

        public abstract String toString();
    }

    static class DiagnosticChecker implements javax.tools.DiagnosticListener<JavaFileObject> {

        boolean errorFound = false;

        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                errorFound = true;
            }
        }
    }
}
