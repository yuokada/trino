/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestosql.sql.planner.iterative.rule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.prestosql.sql.planner.Symbol;
import io.prestosql.sql.planner.assertions.ExpressionMatcher;
import io.prestosql.sql.planner.assertions.RowNumberSymbolMatcher;
import io.prestosql.sql.planner.iterative.rule.test.BaseRuleTest;
import org.testng.annotations.Test;

import static io.prestosql.sql.planner.assertions.PlanMatchPattern.filter;
import static io.prestosql.sql.planner.assertions.PlanMatchPattern.rowNumber;
import static io.prestosql.sql.planner.assertions.PlanMatchPattern.sort;
import static io.prestosql.sql.planner.assertions.PlanMatchPattern.strictProject;
import static io.prestosql.sql.planner.assertions.PlanMatchPattern.values;
import static io.prestosql.sql.tree.SortItem.NullOrdering.FIRST;
import static io.prestosql.sql.tree.SortItem.Ordering.ASCENDING;

public class TestImplementOffsetOverSort
        extends BaseRuleTest
{
    @Test
    public void testReplaceOffsetOverSort()
    {
        tester().assertThat(new ImplementOffsetOverSort())
                .on(p -> {
                    Symbol a = p.symbol("a");
                    Symbol b = p.symbol("b");
                    return p.offset(
                            2,
                            p.sort(
                                    ImmutableList.of(a),
                                    p.values(a, b)));
                })
                .matches(
                        strictProject(
                                ImmutableMap.of("a", new ExpressionMatcher("a"), "b", new ExpressionMatcher("b")),
                                sort(
                                        ImmutableList.of(sort("row_num", ASCENDING, FIRST)),
                                        filter(
                                                "(row_num > BIGINT '2')",
                                                rowNumber(
                                                        pattern -> pattern
                                                                .partitionBy(ImmutableList.of()),
                                                        sort(
                                                                ImmutableList.of(sort("a", ASCENDING, FIRST)),
                                                                values("a", "b")))
                                                        .withAlias("row_num", new RowNumberSymbolMatcher())))));
    }
}
