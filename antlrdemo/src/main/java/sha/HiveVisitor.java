package sha;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HiveVisitor extends ExprBaseVisitor {
    private static final Logger log = LogManager.getLogger();
    public HiveVisitor() {
    }

    @Override
    public Object visitExpr(ExprParser.ExprContext ctx) {
        return super.visitExpr(ctx); // visit children
    }

    @Override
    public Object visitTerm(ExprParser.TermContext ctx) {
        String word = (ctx.WORD().getText());
        return super.visitTerm(ctx);
    }

    @Override
    public Object visitStruct(ExprParser.StructContext ctx) {
        return super.visitStruct(ctx);
    }


}
