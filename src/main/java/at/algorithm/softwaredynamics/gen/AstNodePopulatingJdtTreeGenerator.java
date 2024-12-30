package at.algorithm.softwaredynamics.gen;

import com.github.gumtreediff.gen.jdt.AbstractJdtTreeGenerator;
import com.github.gumtreediff.gen.jdt.AbstractJdtVisitor;
import org.eclipse.jdt.core.compiler.IScanner;

public class AstNodePopulatingJdtTreeGenerator extends AbstractJdtTreeGenerator {

//    @Override
//    protected AbstractJdtVisitor createVisitor() {
//        return new AstNodePopulatingJdtVisitor();
//    }

    @Override
    protected AbstractJdtVisitor createVisitor(IScanner scanner) {
        return new AstNodePopulatingJdtVisitor();
    }
}
