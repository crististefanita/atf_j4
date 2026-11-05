package atf.tests.governance.source;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Syntax-only JDK parsing; no compilation, annotation processing or application-class loading. */
final class JavaTestDeclarations {
    record Type(String name, Tree.Kind kind, Set<Modifier> modifiers, List<String> annotations,
                List<List<String>> methods, List<Type> nested, String annotationSource) {
        boolean concrete() {
            return (kind == Tree.Kind.CLASS || kind == Tree.Kind.RECORD || kind == Tree.Kind.ENUM)
                    && !modifiers.contains(Modifier.ABSTRACT);
        }
    }

    record Unit(String packageName, Map<String, String> imports, List<String> wildcards,
                String header, List<Type> types) {
        String qualified(String name, Map<String, String> known) {
            if (name.contains(".")) return name;
            if (imports.containsKey(name)) return imports.get(name);
            String local = packageName.isEmpty() ? name : packageName + "." + name;
            if (known.containsKey(local)) return local;
            var matches = wildcards.stream().map(prefix -> prefix + "." + name).filter(known::containsKey).distinct().toList();
            if (matches.size() > 1) throw new IllegalArgumentException("GOV-ANNOTATION-AMBIGUOUS: " + name);
            return matches.isEmpty() ? local : matches.getFirst();
        }
    }

    private JavaTestDeclarations() { }

    static Unit parse(String source) {
        try (var parser = open()) { return parser.parse(source); }
    }

    static Parser open() { return new Parser(); }

    /** One inspection owns the file manager; each source still gets a fresh task and diagnostics. */
    static final class Parser implements AutoCloseable {
        private final JavaCompiler compiler;
        private final StandardJavaFileManager manager;
        private boolean closed;

        private Parser() {
            compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) throw new IllegalStateException("GOV-JDK: source governance requires a full JDK");
            manager = compiler.getStandardFileManager(null, Locale.ROOT, null);
        }

        Unit parse(String source) {
            if (closed) throw new IllegalStateException("GOV-JAVA-PARSE: parser is closed");
            var diagnostics = new DiagnosticCollector<JavaFileObject>();
            var input = new SimpleJavaFileObject(URI.create("string:///Governed.java"), JavaFileObject.Kind.SOURCE) {
                @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return source; }
            };
            try {
                var task = (JavacTask) compiler.getTask(null, manager, diagnostics, List.of("-proc:none"), null, List.of(input));
                CompilationUnitTree tree = task.parse().iterator().next();
                var errors = diagnostics.getDiagnostics().stream().filter(value -> value.getKind() == Diagnostic.Kind.ERROR).toList();
                if (!errors.isEmpty()) {
                    var first = errors.getFirst();
                    throw new IllegalArgumentException("GOV-JAVA-SYNTAX: line " + first.getLineNumber()
                            + ", column " + first.getColumnNumber() + ": " + first.getMessage(Locale.ROOT));
                }
                return describe(tree);
            } catch (IOException failure) {
                throw new IllegalStateException("GOV-JAVA-PARSE: cannot inspect source", failure);
            }
        }

        @Override public void close() {
            if (closed) return;
            closed = true;
            try { manager.close(); }
            catch (IOException failure) { throw new IllegalStateException("GOV-JAVA-PARSE: cannot close parser", failure); }
        }
    }

    private static Unit describe(CompilationUnitTree tree) {
        String owner = tree.getPackageName() == null ? "" : tree.getPackageName().toString();
        var imports = new LinkedHashMap<String, String>();
        var wildcards = new ArrayList<String>();
        StringBuilder header = new StringBuilder(owner.isEmpty() ? "" : "package " + owner + ";\n");
        for (var declaration : tree.getImports()) {
            String name = declaration.getQualifiedIdentifier().toString();
            header.append(declaration).append('\n');
            if (name.endsWith(".*")) wildcards.add(name.substring(0, name.length() - 2));
            else imports.put(name.substring(name.lastIndexOf('.') + 1), name);
        }
        var types = tree.getTypeDecls().stream().filter(ClassTree.class::isInstance)
                .map(ClassTree.class::cast).map(JavaTestDeclarations::describe).toList();
        return new Unit(owner, Map.copyOf(imports), List.copyOf(wildcards), header.toString(), types);
    }

    private static Type describe(ClassTree tree) {
        var annotations = tree.getModifiers().getAnnotations().stream().map(value -> value.getAnnotationType().toString()).toList();
        var methods = new ArrayList<List<String>>();
        var nested = new ArrayList<Type>();
        for (var member : tree.getMembers()) {
            if (member instanceof MethodTree method)
                methods.add(method.getModifiers().getAnnotations().stream().map(value -> value.getAnnotationType().toString()).toList());
            else if (member instanceof ClassTree type) nested.add(describe(type));
        }
        return new Type(tree.getSimpleName().toString(), tree.getKind(), Set.copyOf(tree.getModifiers().getFlags()),
                annotations, List.copyOf(methods), List.copyOf(nested), tree.getKind() == Tree.Kind.ANNOTATION_TYPE ? tree.toString() : "");
    }
}
