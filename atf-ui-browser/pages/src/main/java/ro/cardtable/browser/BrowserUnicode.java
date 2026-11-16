package ro.cardtable.browser;

import org.teavm.jso.JSBody;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.MethodReference;
import org.teavm.model.instructions.InvokeInstruction;

import java.text.Normalizer;

/** The browser provides NFC directly. The Java normalizer needs JDK-only ICU resources. */
public final class BrowserUnicode implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        if (!type.getName().equals("ro.cardtable.names.PlayerNames")) return;
        var jdk = new MethodReference(Normalizer.class, "normalize", CharSequence.class, Normalizer.Form.class, String.class);
        var browser = new MethodReference(BrowserUnicode.class, "normalize", CharSequence.class, Normalizer.Form.class, String.class);
        for (var method : type.getMethods()) {
            if (method.getProgram() == null) continue;
            for (var block : method.getProgram().getBasicBlocks()) {
                for (var instruction : block) {
                    if (instruction instanceof InvokeInstruction call && call.getMethod().equals(jdk)) call.setMethod(browser);
                }
            }
        }
    }

    public static String normalize(CharSequence value, Normalizer.Form form) {
        return normalize(value.toString(), form.name());
    }

    @JSBody(params = {"value", "form"}, script = "return value.normalize(form);")
    private static native String normalize(String value, String form);
}
