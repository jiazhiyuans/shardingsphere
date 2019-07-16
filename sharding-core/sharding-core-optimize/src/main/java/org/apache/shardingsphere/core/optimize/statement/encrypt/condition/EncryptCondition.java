/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.core.optimize.statement.encrypt.condition;

import com.google.common.base.Preconditions;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.shardingsphere.core.constant.ShardingOperator;
import org.apache.shardingsphere.core.parse.sql.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.expr.simple.LiteralExpressionSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.expr.simple.ParameterMarkerExpressionSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.predicate.PredicateSegment;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Encrypt condition.
 *
 * @author zhangliang
 * @author maxiaoguang
 */
@Getter
@EqualsAndHashCode(exclude = {"predicateSegment"})
@ToString
public final class EncryptCondition {
    
    private final String columnName;
    
    private final String tableName;
    
    private final PredicateSegment predicateSegment;
    
    private final ShardingOperator operator;
    
    private final Map<Integer, Comparable<?>> positionValueMap = new LinkedHashMap<>();
    
    private final Map<Integer, Integer> positionIndexMap = new LinkedHashMap<>();
    
    public EncryptCondition(final String columnName, final String tableName, final PredicateSegment predicateSegment, final ExpressionSegment expressionSegment) {
        this.columnName = columnName;
        this.tableName = tableName;
        this.predicateSegment = predicateSegment;
        operator = ShardingOperator.EQUAL;
        putPositionMap(0, expressionSegment);
    }
    
    public EncryptCondition(final String columnName, final String tableName, final PredicateSegment predicateSegment, final List<ExpressionSegment> expressionSegments) {
        this.columnName = columnName;
        this.tableName = tableName;
        this.predicateSegment = predicateSegment;
        operator = ShardingOperator.IN;
        int count = 0;
        for (ExpressionSegment each : expressionSegments) {
            putPositionMap(count, each);
            count++;
        }
    }
    
    private void putPositionMap(final int position, final ExpressionSegment expressionSegment) {
        if (expressionSegment instanceof ParameterMarkerExpressionSegment) {
            positionIndexMap.put(position, ((ParameterMarkerExpressionSegment) expressionSegment).getParameterMarkerIndex());
        } else if (expressionSegment instanceof LiteralExpressionSegment) {
            positionValueMap.put(position, (Comparable<?>) ((LiteralExpressionSegment) expressionSegment).getLiterals());
        }
    }
    
    /**
     * Get condition values.
     *
     * @param parameters parameters
     * @return condition values
     */
    public List<Comparable<?>> getConditionValues(final List<Object> parameters) {
        List<Comparable<?>> result = new LinkedList<>(positionValueMap.values());
        for (Entry<Integer, Integer> entry : positionIndexMap.entrySet()) {
            Object parameter = parameters.get(entry.getValue());
            Preconditions.checkArgument(parameter instanceof Comparable<?>, "Parameter `%s` should extends Comparable for sharding value.", parameter);
            if (entry.getKey() < result.size()) {
                result.add(entry.getKey(), (Comparable<?>) parameter);
            } else {
                result.add((Comparable<?>) parameter);
            }
        }
        return result;
    }
}