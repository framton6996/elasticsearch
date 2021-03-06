/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.eql.expression.function.scalar.string;

import org.elasticsearch.xpack.ql.expression.Expression;
import org.elasticsearch.xpack.ql.expression.Expressions;
import org.elasticsearch.xpack.ql.expression.FieldAttribute;
import org.elasticsearch.xpack.ql.expression.function.scalar.ScalarFunction;
import org.elasticsearch.xpack.ql.expression.gen.pipeline.Pipe;
import org.elasticsearch.xpack.ql.expression.gen.script.ScriptTemplate;
import org.elasticsearch.xpack.ql.expression.gen.script.Scripts;
import org.elasticsearch.xpack.ql.tree.NodeInfo;
import org.elasticsearch.xpack.ql.tree.Source;
import org.elasticsearch.xpack.ql.type.DataType;
import org.elasticsearch.xpack.ql.type.DataTypes;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static java.lang.String.format;
import static org.elasticsearch.xpack.eql.expression.function.scalar.string.LengthFunctionProcessor.doProcess;
import static org.elasticsearch.xpack.ql.expression.TypeResolutions.ParamOrdinal.DEFAULT;
import static org.elasticsearch.xpack.ql.expression.TypeResolutions.isStringAndExact;
import static org.elasticsearch.xpack.ql.expression.gen.script.ParamsBuilder.paramsBuilder;

/**
 * EQL specific length function acting on every type of field, not only strings.
 * For strings it will return the length of that specific string, for any other type it will return 0.
 */
public class Length extends ScalarFunction {

    private final Expression input;

    public Length(Source source, Expression input) {
        super(source, Arrays.asList(input));
        this.input = input;
    }

    @Override
    protected TypeResolution resolveType() {
        if (childrenResolved() == false) {
            return new TypeResolution("Unresolved children");
        }

        return isStringAndExact(input, sourceText(), DEFAULT);
    }

    @Override
    protected Pipe makePipe() {
        return new LengthFunctionPipe(source(), this, Expressions.pipe(input));
    }

    @Override
    public boolean foldable() {
        return input.foldable();
    }

    @Override
    public Object fold() {
        return doProcess(input.fold());
    }

    @Override
    protected NodeInfo<? extends Expression> info() {
        return NodeInfo.create(this, Length::new, input);
    }

    @Override
    public ScriptTemplate asScript() {
        ScriptTemplate inputScript = asScript(input);

        return new ScriptTemplate(
            format(Locale.ROOT, formatTemplate("{eql}.%s(%s)"), "length", inputScript.template()),
            paramsBuilder().script(inputScript.params()).build(),
            dataType()
        );
    }

    @Override
    public ScriptTemplate scriptWithField(FieldAttribute field) {
        return new ScriptTemplate(
            processScript(Scripts.DOC_VALUE),
            paramsBuilder().variable(field.exactAttribute().name()).build(),
            dataType()
        );
    }

    @Override
    public DataType dataType() {
        return DataTypes.INTEGER;
    }

    @Override
    public Expression replaceChildren(List<Expression> newChildren) {
        return new Length(source(), newChildren.get(0));
    }

}
